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
import config.Refined.Max5000
import controllers.PSRController
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPages, LandOrPropertyListPage}
import cats.implicits.toShow
import config.Constants.maxLandOrProperties
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import utils.nonsipp.TaskListStatusUtils.{getCompletedOrUpdatedTaskListStatus, getLandOrPropertyTaskListStatusAndLink}
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.Future

import javax.inject.Named

class LandOrPropertyListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

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
    previous: Int,
    showBackLink: Boolean
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
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
  )(
    implicit request: DataRequest[AnyContent]
  ): Result = {
    val userAnswers = request.userAnswers
    val (status, incompleteLandOrPropertyUrl) = getLandOrPropertyTaskListStatusAndLink(userAnswers, srn)

    if (status == TaskListStatus.NotStarted) {
      Redirect(routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode))
    } else if (status == TaskListStatus.InProgress) {
      Redirect(incompleteLandOrPropertyUrl)
    } else {
      val addresses = userAnswers.map(LandOrPropertyAddressLookupPages(srn))
      val viewModel =
        LandOrPropertyListController.viewModel(
          srn,
          page,
          mode,
          addresses,
          request.schemeDetails.schemeName,
          viewOnlyViewModel,
          showBackLink = showBackLink
        )
      Ok(view(form, viewModel))
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val addresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn))

    if (addresses.size == maxLandOrProperties) {
      Redirect(navigator.nextPage(LandOrPropertyListPage(srn, addLandOrProperty = false), mode, request.userAnswers))
    } else {
      val viewModel =
        LandOrPropertyListController.viewModel(srn, page, mode, addresses, "", showBackLink = true)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer =>
            Redirect(
              navigator.nextPage(LandOrPropertyListPage(srn, addLandOrProperty = answer), mode, request.userAnswers)
            )
        )
    }
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }

  def onPreviousViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          routes.LandOrPropertyListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0), showBackLink = false)
        )
      )
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
    addresses: Map[String, Address],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
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
        .flatMap {
          case (index, address) =>
            refineV[Max5000.Refined](index.toInt + 1).fold(
              _ => Nil,
              index =>
                (mode, viewOnlyViewModel) match {
                  case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
                    List(
                      index -> ListRow.view(
                        address.addressLine1,
                        routes.LandOrPropertyCYAController
                          .onPageLoadViewOnly(srn, index, year, current, previous)
                          .url,
                        Message("landOrPropertyList.row.change.hiddenText", address.addressLine1)
                      )
                    )
                  case _ =>
                    List(
                      index -> ListRow(
                        address.addressLine1,
                        changeUrl = routes.LandOrPropertyCYAController.onPageLoad(srn, index, CheckMode).url,
                        changeHiddenText = Message("landOrPropertyList.row.change.hiddenText", address.addressLine1),
                        removeUrl = routes.RemovePropertyController.onPageLoad(srn, index, mode).url,
                        removeHiddenText = Message("landOrPropertyList.row.remove.hiddenText", address.addressLine1)
                      )
                    )
                }
            )
        }
        .toList
        .sortBy { case (index, _) => index.value }
        .map { case (_, listRow) => listRow }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    addresses: Map[String, Address],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = ((mode, addresses.size) match {
      case (ViewOnlyMode, addressesSize) if addressesSize == 0 =>
        ("landOrPropertyList.viewOnly.title.none", "landOrPropertyList.viewOnly.heading.none")
      case (ViewOnlyMode, addressesSize) if addressesSize > 1 =>
        ("landOrPropertyList.viewOnly.title.plural", "landOrPropertyList.viewOnly.heading.plural")
      case (ViewOnlyMode, _) =>
        ("landOrPropertyList.viewOnly.title", "landOrPropertyList.viewOnly.heading")
      case (_, addressesSize) if addressesSize > 1 =>
        ("landOrPropertyList.title.plural", "landOrPropertyList.heading.plural")
      case _ =>
        ("landOrPropertyList.title", "landOrPropertyList.heading")
    }) match {
      case (title, heading) =>
        (Message(title, addresses.size), Message(heading, addresses.size))
    }

    val paragraph =
      if (addresses.size < maxLandOrProperties) Some(ParagraphMessage("landOrPropertyList.paragraph")) else None

    val currentPage = if ((page - 1) * Constants.landOrPropertiesSize >= addresses.size) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.landOrPropertiesSize,
      addresses.size,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.LandOrPropertyListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion, showBackLink = true)
        case _ =>
          routes.LandOrPropertyListController.onPageLoad(srn, _, NormalMode)
      }
    )

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = paragraph,
      page = ListViewModel(
        inset = "landOrPropertyList.inset",
        rows(srn, mode, addresses, viewOnlyViewModel, schemeName),
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
