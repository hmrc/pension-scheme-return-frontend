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

package controllers.nonsipp.landorpropertydisposal

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import cats.implicits._
import controllers.actions._
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import utils.nonsipp.TaskListStatusUtils.{
  getCompletedOrUpdatedTaskListStatus,
  getLandOrPropertyDisposalsTaskListStatusWithLink
}
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalListController._
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPages, LandOrPropertyChosenAddressPage}
import config.Constants.maxLandOrPropertyDisposals
import pages.nonsipp.landorpropertydisposal._
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logging
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models.{ViewOnlyViewModel, _}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.Future

import javax.inject.Named

class LandOrPropertyDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController
    with Logging {

  val form: Form[Boolean] = LandOrPropertyDisposalListController.form(formProvider)

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous) { implicit request =>
      val showBackLink = true
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            val updated = getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              pages.nonsipp.landorpropertydisposal.Paths.disposalPropertyTransaction
            ) == Updated
            updated
          case None =>
            false
          case _ => false
        },
        year = year,
        currentVersion = current,
        previousVersion = previous,
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )
      onPageLoadCommon(srn, page, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
    }

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, page, mode, showBackLink = true)
    }

  def onPreviousViewOnly(
    srn: Srn,
    page: Int,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
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
                  pages.nonsipp.otherassetsdisposal.Paths.assetsDisposed
                ) == Updated
              case None =>
                false
            },
            year = year,
            currentVersion = (current - 1).max(0),
            previousVersion = (previous - 1).max(0),
            compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
          )
          onPageLoadCommon(srn, page, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
        }
    }

  private def onPageLoadCommon(
    srn: Srn,
    page: Int,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  )(
    implicit request: DataRequest[_]
  ): Result = {
    val (status, _) = getLandOrPropertyDisposalsTaskListStatusWithLink(request.userAnswers, srn)
    logger.info(s"Land or property disposal status is $status")

    getCompletedDisposals(srn).map { completedDisposals =>
      if (viewOnlyViewModel.nonEmpty || completedDisposals.values.exists(_.nonEmpty)) {
        val numberOfDisposal = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
        val numberOfAddresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn)).size
        val maxPossibleNumberOfDisposals = maxLandOrPropertyDisposals * numberOfAddresses
        getAddressesWithIndexes(srn, completedDisposals).map { indexes =>
          Ok(
            view(
              form,
              viewModel(
                srn,
                mode,
                page,
                indexes,
                numberOfDisposal,
                maxPossibleNumberOfDisposals,
                request.schemeDetails.schemeName,
                viewOnlyViewModel,
                showBackLink = showBackLink
              )
            )
          )
        }.merge
      } else {
        Redirect(routes.LandOrPropertyDisposalController.onPageLoad(srn, NormalMode))
      }
    }.merge
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    getCompletedDisposals(srn).map { completedDisposals =>
      val numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfAddresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn)).size
      val maxPossibleNumberOfDisposals = maxLandOrPropertyDisposals * numberOfAddresses
      if (numberOfDisposals == maxPossibleNumberOfDisposals) {
        Redirect(
          navigator.nextPage(LandOrPropertyDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors =>
              getAddressesWithIndexes(srn, completedDisposals)
                .map(
                  indexes =>
                    BadRequest(
                      view(
                        errors,
                        viewModel(
                          srn,
                          mode,
                          page,
                          indexes,
                          numberOfDisposals,
                          maxPossibleNumberOfDisposals,
                          request.schemeDetails.schemeName,
                          showBackLink = true
                        )
                      )
                    )
                )
                .merge,
            answer =>
              Redirect(navigator.nextPage(LandOrPropertyDisposalListPage(srn, answer), mode, request.userAnswers))
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

  private def getCompletedDisposals(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(LandPropertyDisposalCompletedPages(srn))
      .filter(_._2.nonEmpty)
      .map {
        case (key, sectionCompleted) =>
          val maybeLandOrPropertyIndex: Either[Result, Max5000] =
            refineStringIndex[Max5000.Refined](key).getOrRecoverJourney

          val maybeDisposalIndexes: Either[Result, List[Max50]] =
            sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)

          for {
            landOrPropertyIndex <- maybeLandOrPropertyIndex
            disposalIndexes <- maybeDisposalIndexes
          } yield (landOrPropertyIndex, disposalIndexes)
      }
      .toList
      .sequence
      .map(_.toMap)

  private def getAddressesWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(
    implicit request: DataRequest[_]
  ): Either[Result, List[((Max5000, List[Max50]), Address)]] =
    disposals
      .map {
        case indexes @ (landOrPropertyIndex, _) =>
          landOrPropertyIndex -> request.userAnswers
            .get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex))
            .getOrRecoverJourney
            .map(address => (indexes, address))
      }
      .toList
      .sortBy { case (index, _) => index.value }
      .map { case (_, listRow) => listRow }
      .sequence

}

object LandOrPropertyDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "landOrPropertyDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    addressesWithIndexes: List[((Max5000, List[Max50]), Address)],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    addressesWithIndexes match {
      case Nil =>
        List(
          ListRow.viewNoLink(
            Message("landOrPropertyDisposalList.view.none", schemeName),
            "landOrPropertyDisposalList.view.none.value"
          )
        )
      case list =>
        list.flatMap { a =>
          (a, viewOnlyViewModel) match {
            case (((index, disposalIndexes), address), Some(viewOnly)) =>
              List(
                ListRow.view(
                  Message(
                    if (disposalIndexes.size > 1) "landOrPropertyDisposalList.view.row.plural"
                    else "landOrPropertyDisposalList.view.row",
                    disposalIndexes.size,
                    address.addressLine1
                  ),
                  routes.LandPropertyDisposalCYAController
                    .onPageLoadViewOnly(
                      srn,
                      index,
                      disposalIndexes.head,
                      viewOnly.year,
                      viewOnly.currentVersion,
                      viewOnly.previousVersion
                    )
                    .url,
                  Message("landOrPropertyDisposalList.row.view.hidden", address.addressLine1)
                )
              )
            case (((index, disposalIndexes), address), None) =>
              disposalIndexes.map { x =>
                ListRow(
                  Message("landOrPropertyDisposalList.row", address.addressLine1),
                  changeUrl = routes.LandPropertyDisposalCYAController
                    .onPageLoad(srn, index, x, mode)
                    .url,
                  changeHiddenText = Message("landOrPropertyDisposalList.row.change.hidden", address.addressLine1),
                  removeUrl = routes.RemoveLandPropertyDisposalController.onPageLoad(srn, index, x, NormalMode).url,
                  removeHiddenText = Message("landOrPropertyDisposalList.row.remove.hidden", address.addressLine1)
                )
              }
          }
        }
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    addressesWithIndexes: List[((Max5000, List[Max50]), Address)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int,
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = ((mode, numberOfDisposals) match {
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals == 0 =>
        ("landOrPropertyDisposalList.view.title", "landOrPropertyDisposalList.view.heading.none")
      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals > 1 =>
        ("landOrPropertyDisposalList.view.title", "landOrPropertyDisposalList.view.heading.plural")
      case (ViewOnlyMode, _) =>
        ("landOrPropertyDisposalList.view.title", "landOrPropertyDisposalList.view.heading")
      case (_, numberOfDisposals) if numberOfDisposals > 1 =>
        ("landOrPropertyDisposalList.title.plural", "landOrPropertyDisposalList.heading.plural")
      case _ =>
        ("landOrPropertyDisposalList.title", "landOrPropertyDisposalList.heading")
    }) match {
      case (title, heading) => (Message(title, numberOfDisposals), Message(heading, numberOfDisposals))
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.landOrPropertyDisposalsSize,
      totalSize = numberOfDisposals,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.LandOrPropertyDisposalListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          routes.LandOrPropertyDisposalListController.onPageLoad(srn, _)
      }
    )

    FormPageViewModel(
      title = title,
      heading = heading,
      description = Option.when(numberOfDisposals < maxPossibleNumberOfDisposals)(
        ParagraphMessage("landOrPropertyDisposalList.description")
      ),
      page = ListViewModel(
        inset = "landOrPropertyDisposalList.inset",
        rows(srn, mode, addressesWithIndexes, viewOnlyViewModel, schemeName),
        Message("landOrPropertyDisposalList.radios"),
        showRadios = !mode.isViewOnlyMode && numberOfDisposals < maxPossibleNumberOfDisposals,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "landOrPropertyDisposalList.pagination.label",
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
      onSubmit = routes.LandOrPropertyDisposalListController.onSubmit(srn, page),
      mode = mode,
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "landOrPropertyDisposalList.view.link",
                routes.LandOrPropertyDisposalListController
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
          onSubmit = controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }
}
