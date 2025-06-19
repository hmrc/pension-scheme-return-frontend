/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.nonsipp.landorproperty

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import controllers.nonsipp.landorproperty.LandOrPropertyListController._
import cats.implicits.toShow
import config.Constants.maxLandOrProperties
import controllers.actions._
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import config.RefinedTypes.Max5000
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.{getCompletedOrUpdatedTaskListStatus, getLandOrPropertyTaskListStatusAndLink}
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import utils.IntUtils.toInt
import pages.nonsipp.landorproperty._
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import utils.nonsipp.check.LandOrPropertyCheckStatusUtils
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import utils.MapUtils.UserAnswersMapOps
import play.api.data.Form

import scala.concurrent.Future

import javax.inject.Named

class LandOrPropertyListController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  given logger: Logger = Logger(getClass)

  val form: Form[Boolean] = LandOrPropertyListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, page, mode, showBackLink = true)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
    val showBackLink = true
    val viewOnlyViewModel = ViewOnlyViewModel(
      viewOnlyUpdated = request.previousUserAnswers match {
        case Some(previousUserAnswers) =>
          getCompletedOrUpdatedTaskListStatus(
            request.userAnswers,
            previousUserAnswers,
            pages.nonsipp.landorproperty.Paths.landOrProperty,
            Some("disposedPropertyTransaction")
          ) == Updated
        case _ => false
      },
      year = year,
      currentVersion = current,
      previousVersion = previous,
      compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
    )
    onPageLoadCommon(srn, page, mode, Some(viewOnlyViewModel), showBackLink)
  }

  def onPageLoadCommon(
    srn: Srn,
    page: Int,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  )(implicit
    request: DataRequest[AnyContent]
  ): Result = {
    val (status, incompleteLandOrPropertyUrl) =
      getLandOrPropertyTaskListStatusAndLink(request.userAnswers, srn, isPrePopulation)

    if (status == TaskListStatus.NotStarted) {
      Redirect(routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode))
    } else if (status == TaskListStatus.InProgress) {
      Redirect(incompleteLandOrPropertyUrl)
    } else {
      addresses(srn).getOrRecoverJourney.map { case (addressesToCheck, addresses) =>
        Ok(
          view(
            form,
            viewModel(
              srn,
              page,
              mode,
              addresses = addresses,
              addressesToCheck = addressesToCheck,
              request.schemeDetails.schemeName,
              viewOnlyViewModel,
              showBackLink = showBackLink,
              isPrePopulation
            )
          )
        )
      }.merge
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>

    val inProgressAnswers = request.userAnswers.map(LandOrPropertyProgress.all())
    val inProgressUrl = inProgressAnswers.collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
    addresses(srn).getOrRecoverJourney.map { case (addressesToCheck, addresses) =>
      if (addressesToCheck.size + addresses.size == maxLandOrProperties) {
        Redirect(
          navigator.nextPage(LandOrPropertyListPage(srn, addLandOrProperty = false), mode, request.userAnswers)
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              BadRequest(
                view(
                  errors,
                  viewModel(
                    srn,
                    page,
                    mode,
                    addresses = addresses,
                    addressesToCheck = addressesToCheck,
                    request.schemeDetails.schemeName,
                    None,
                    showBackLink = true,
                    isPrePopulation
                  )
                )
              ),
            answer =>
              if (answer) {
                inProgressUrl match {
                  case Some(url) => Redirect(url)
                  case _ =>
                    Redirect(
                      navigator.nextPage(
                        LandOrPropertyListPage(srn, addLandOrProperty = answer),
                        mode,
                        request.userAnswers
                      )
                    )
                }
              } else {
                Redirect(
                  navigator
                    .nextPage(LandOrPropertyListPage(srn, addLandOrProperty = answer), mode, request.userAnswers)
                )
              }
          )
      }
    }.merge
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }

  def onPreviousViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)).async {
      implicit request =>
        Future.successful {
          val showBackLink = false
          val viewOnlyViewModel = ViewOnlyViewModel(
            viewOnlyUpdated = request.previousUserAnswers match {
              case Some(previousUserAnswers) =>
                getCompletedOrUpdatedTaskListStatus(
                  request.userAnswers,
                  previousUserAnswers,
                  pages.nonsipp.landorproperty.Paths.landOrProperty,
                  Some("disposedPropertyTransaction")
                ) == Updated
              case _ => false
            },
            year = year,
            currentVersion = (current - 1).max(0),
            previousVersion = (previous - 1).max(0),
            compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
          )
          onPageLoadCommon(srn, page, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
        }
    }

  private def addresses(
    srn: Srn
  )(implicit request: DataRequest[?]): Either[String, (Map[Max5000, Address], Map[Max5000, Address])] = {
    val completedIndexes = request.userAnswers.map(LandOrPropertyProgress.all()).filter(_._2.completed).keys.toList
    // if return has been pre-populated, partition addresses by those that need to be checked
    if (isPrePopulation) {
      request.userAnswers
        .map(LandOrPropertyAddressLookupPages(srn))
        .collect { case (index, address) if completedIndexes.contains(index) => (index, address) }
        .refine[Max5000.Refined]
        .map(_.map { case (index, address) =>
          (index, address.copy(canRemove = request.userAnswers.get(LandOrPropertyPrePopulated(srn, index)).isEmpty))
        })
        .map(_.partition { case (index, _) =>
          LandOrPropertyCheckStatusUtils.checkLandOrPropertyRecord(request.userAnswers, srn, index)
        })
    } else {
      val noAddressesToCheck = Map.empty[Max5000, Address]
      request.userAnswers
        .map(LandOrPropertyAddressLookupPages(srn))
        .collect { case (index, address) if completedIndexes.contains(index) => (index, address) }
        .refine[Max5000.Refined]
        .map((noAddressesToCheck, _))
    }
  }
}

object LandOrPropertyListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "landOrPropertyList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    addresses: Map[Max5000, Address],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String,
    check: Boolean = false
  ): List[ListRow] =
    if (addresses.isEmpty && mode.isViewOnlyMode) {
      List(
        ListRow.viewNoLink(
          Message("landOrPropertyList.viewOnly.none", schemeName),
          "landOrPropertyList.viewOnly.none.value"
        )
      )
    } else if (addresses.isEmpty && !mode.isViewOnlyMode) {
      List()
    } else {
      addresses
        .flatMap { case (index, address) =>
          (mode, viewOnlyViewModel) match {
            case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
              List(
                index -> ListRow.view(
                  address.addressLine1,
                  routes.LandOrPropertyCYAController
                    .onPageLoadViewOnly(srn, index, year, current, previous)
                    .url,
                  Message("landOrPropertyList.row.view.hiddenText", address.addressLine1)
                )
              )
            case _ if check =>
              List(
                index -> ListRow.check(
                  address.addressLine1,
                  routes.LandOrPropertyCheckAndUpdateController.onPageLoad(srn, index).url,
                  Message("landOrPropertyList.row.check.hiddenText", address.addressLine1)
                )
              )
            case _ if address.canRemove =>
              List(
                index -> ListRow(
                  address.addressLine1,
                  changeUrl = routes.LandOrPropertyCYAController.onPageLoad(srn, index, CheckMode).url,
                  changeHiddenText = Message("landOrPropertyList.row.change.hiddenText", address.addressLine1),
                  removeUrl = routes.RemovePropertyController.onPageLoad(srn, index, mode).url,
                  removeHiddenText = Message("landOrPropertyList.row.remove.hiddenText", address.addressLine1)
                )
              )
            case _ =>
              List(
                index -> ListRow(
                  address.addressLine1,
                  changeUrl = routes.LandOrPropertyCYAController.onPageLoad(srn, index, CheckMode).url,
                  changeHiddenText = Message("landOrPropertyList.row.change.hiddenText", address.addressLine1)
                )
              )
          }
        }
        .toList
        .sortBy { case (index, _) => index.value }
        .map { case (_, listRow) => listRow }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    addresses: Map[Max5000, Address],
    addressesToCheck: Map[Max5000, Address],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    isPrePop: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val addressesSize = if (isPrePop) addresses.size + addressesToCheck.size else addresses.size

    val (title, heading) = ((mode, addressesSize, isPrePop) match {
      // View only
      case (ViewOnlyMode, addressesSize, _) if addressesSize == 0 =>
        ("landOrPropertyList.viewOnly.title.none", "landOrPropertyList.viewOnly.heading.none")
      case (ViewOnlyMode, addressesSize, _) if addressesSize > 1 =>
        ("landOrPropertyList.viewOnly.title.plural", "landOrPropertyList.viewOnly.heading.plural")
      case (ViewOnlyMode, _, _) =>
        ("landOrPropertyList.viewOnly.title", "landOrPropertyList.viewOnly.heading")
      // Pre-pop
      case (_, _, isPrePop @ true) if addresses.nonEmpty =>
        ("landOrPropertyList.title.prepop.check", "landOrPropertyList.heading.prepop.check")
      case (_, addressesSize, isPrePop @ true) if addressesSize > 1 =>
        ("landOrPropertyList.title.prepop.plural", "landOrPropertyList.heading.prepop.plural")
      case (_, _, isPrePop @ true) =>
        ("landOrPropertyList.title.prepop", "landOrPropertyList.heading.prepop")
      // Normal
      case (_, addressesSize, _) if addressesSize > 1 =>
        ("landOrPropertyList.title.plural", "landOrPropertyList.heading.plural")
      case _ =>
        ("landOrPropertyList.title", "landOrPropertyList.heading")
    }) match {
      case (title, heading) =>
        (Message(title, addressesSize), Message(heading, addressesSize))
    }

    val paragraph = Option.when(addressesSize < maxLandOrProperties) {
      if (addressesToCheck.nonEmpty) {
        ParagraphMessage("landOrPropertyList.paragraph.prepop") ++
          ParagraphMessage("landOrPropertyList.paragraph.disposal")
      } else {
        ParagraphMessage("landOrPropertyList.paragraph") ++
          ParagraphMessage("landOrPropertyList.paragraph.disposal")
      }
    }

    val currentPage = if ((page - 1) * Constants.landOrPropertiesSize >= addressesSize) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.landOrPropertiesSize,
      addressesSize,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.LandOrPropertyListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.LandOrPropertyListController.onPageLoad(srn, _, NormalMode)
      }
    )

    val sections =
      if (isPrePop) {
        Option
          .when(addressesToCheck.nonEmpty)(
            ListSection(
              heading = Some("landOrPropertyList.section.addresses.check"),
              rows(srn, mode, addressesToCheck, viewOnlyViewModel, schemeName, check = true)
            )
          )
          .toList ++
          Option
            .when(addresses.nonEmpty)(
              ListSection(
                heading = Some("landOrPropertyList.section.addresses"),
                rows(srn, mode, addresses, viewOnlyViewModel, schemeName)
              )
            )
            .toList
      } else {
        List(
          ListSection(rows(srn, mode, addresses, viewOnlyViewModel, schemeName))
        )
      }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = paragraph,
      page = ListViewModel(
        inset = "landOrPropertyList.inset",
        sections,
        Message("landOrPropertyList.radios"),
        showRadios = addresses.size < Constants.maxLandOrProperties,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "landOrPropertyList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      onSubmit = routes.LandOrPropertyListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "landOrPropertyList.viewOnly.link",
                routes.LandOrPropertyListController
                  .onPreviousViewOnly(
                    srn,
                    page,
                    viewOnly.year,
                    viewOnly.currentVersion,
                    viewOnly.previousVersion
                  )
                  .url
              )
            )
          } else {
            None
          },
          submittedText = viewOnly.compilationOrSubmissionDate
            .fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
          title = title,
          heading = heading,
          buttonText = "site.return.to.tasklist",
          onSubmit = routes.LandOrPropertyListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }
}
