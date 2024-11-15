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
import utils.ListUtils.ListOps
import cats.implicits._
import controllers.actions._
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import models.HowDisposed.HowDisposed
import com.google.inject.Inject
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.{
  getCompletedOrUpdatedTaskListStatus,
  getLandOrPropertyDisposalsTaskListStatusWithLink
}
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalListController._
import pages.nonsipp.landorproperty.{
  LandOrPropertyAddressLookupPages,
  LandOrPropertyChosenAddressPage,
  LandOrPropertyCompleted
}
import config.Constants.{maxLandOrProperties, maxLandOrPropertyDisposals}
import pages.nonsipp.landorpropertydisposal._
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logging
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models.{ViewOnlyViewModel, _}
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
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

    getDisposals(srn).map { completedDisposals =>
      if (viewOnlyViewModel.nonEmpty || completedDisposals.values.exists(_.nonEmpty)) {
        val numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
        val numberOfAddresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn)).size
        val maxPossibleNumberOfDisposals = maxLandOrPropertyDisposals * numberOfAddresses

        getAddressesWithIndexes(srn, completedDisposals).map { addressesWithIndexes =>
          val allPropertiesFullyDisposed: Boolean = addressesWithIndexes.forall {
            case ((addressIndex, disposalIndexes), _) =>
              disposalIndexes.exists { disposalIndex =>
                request.userAnswers.get(LandOrPropertyStillHeldPage(srn, addressIndex, disposalIndex)).contains(false)
              }
          }

          val maximumDisposalsReachedUpdated = numberOfDisposals >= maxLandOrProperties * maxLandOrPropertyDisposals ||
            numberOfDisposals >= maxPossibleNumberOfDisposals ||
            allPropertiesFullyDisposed

          Ok(
            view(
              form,
              viewModel(
                srn,
                mode,
                page,
                addressesWithIndexes,
                numberOfDisposals,
                maxPossibleNumberOfDisposals,
                request.userAnswers,
                request.schemeDetails.schemeName,
                viewOnlyViewModel,
                showBackLink = showBackLink,
                maximumDisposalsReached = maximumDisposalsReachedUpdated,
                allPropertiesFullyDisposed = allPropertiesFullyDisposed
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
    (for {
      completedDisposals <- getDisposals(srn)
      numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      numberOfAddresses = request.userAnswers.map(LandOrPropertyAddressLookupPages(srn)).size
      maxPossibleNumberOfDisposals = maxLandOrPropertyDisposals * numberOfAddresses

      addressesWithIndexes <- getAddressesWithIndexes(srn, completedDisposals)

      allPropertiesFullyDisposed = addressesWithIndexes.forall {
        case ((addressIndex, disposalIndexes), _) =>
          disposalIndexes.exists { disposalIndex =>
            request.userAnswers.get(LandOrPropertyStillHeldPage(srn, addressIndex, disposalIndex)).contains(false)
          }
      }

      maximumDisposalsReachedUpdated = numberOfDisposals >= maxLandOrProperties * maxLandOrPropertyDisposals ||
        numberOfDisposals >= maxPossibleNumberOfDisposals ||
        allPropertiesFullyDisposed

      result <- if (maximumDisposalsReachedUpdated) {
        Right(Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))
      } else {
        Right(
          form
            .bindFromRequest()
            .fold(
              errors =>
                BadRequest(
                  view(
                    errors,
                    viewModel(
                      srn,
                      mode,
                      page,
                      addressesWithIndexes,
                      numberOfDisposals,
                      maxPossibleNumberOfDisposals,
                      request.userAnswers,
                      request.schemeDetails.schemeName,
                      showBackLink = true,
                      maximumDisposalsReached = maximumDisposalsReachedUpdated,
                      allPropertiesFullyDisposed = allPropertiesFullyDisposed
                    )
                  )
                ),
              answer =>
                Redirect(navigator.nextPage(LandOrPropertyDisposalListPage(srn, answer), mode, request.userAnswers))
            )
        )
      }
    } yield result).merge
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }

  private def getDisposals(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    Right(
      request.userAnswers
        .map(LandOrPropertyCompleted.all(srn))
        .keys
        .toList
        .refine[Max5000.Refined]
        .map { index =>
          index -> request.userAnswers
            .map(LandPropertyDisposalCompleted.all(srn, index))
            .keys
            .toList
            .refine[Max50.Refined]
        }
        .toMap
    )

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
    userAnswers: UserAnswers,
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    if (addressesWithIndexes.isEmpty) {
      List(
        ListRow.viewNoLink(
          Message("landOrPropertyDisposalList.view.none", schemeName),
          "landOrPropertyDisposalList.view.none.value"
        )
      )
    } else {
      addressesWithIndexes.flatMap {
        case ((addressIndex, disposalIndexes), address) =>
          disposalIndexes.map { disposalIndex =>
            val landOrPropertyDisposalData = LandOrPropertyDisposalData(
              addressIndex,
              disposalIndex,
              address.addressLine1,
              userAnswers.get(HowWasPropertyDisposedOfPage(srn, addressIndex, disposalIndex)).get
            )

            (mode, viewOnlyViewModel) match {
              case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _))) =>
                ListRow.view(
                  buildMessage("landOrPropertyDisposalList.row", landOrPropertyDisposalData),
                  routes.LandPropertyDisposalCYAController
                    .onPageLoadViewOnly(srn, addressIndex, disposalIndex, year, currentVersion, previousVersion)
                    .url,
                  buildMessage("landOrPropertyDisposalList.row.view.hidden", landOrPropertyDisposalData)
                )
              case (_, _) =>
                ListRow(
                  buildMessage("landOrPropertyDisposalList.row", landOrPropertyDisposalData),
                  changeUrl = routes.LandPropertyDisposalCYAController
                    .onPageLoad(srn, addressIndex, disposalIndex, CheckMode)
                    .url,
                  changeHiddenText = buildMessage(
                    "landOrPropertyDisposalList.row.change.hidden",
                    landOrPropertyDisposalData
                  ),
                  removeUrl = routes.RemoveLandPropertyDisposalController
                    .onPageLoad(srn, addressIndex, disposalIndex, NormalMode)
                    .url,
                  removeHiddenText = buildMessage(
                    "landOrPropertyDisposalList.row.remove.hidden",
                    landOrPropertyDisposalData
                  )
                )
            }
          }
      }
    }

  private def buildMessage(messageString: String, landOrPropertyDisposalData: LandOrPropertyDisposalData): Message =
    landOrPropertyDisposalData match {
      case LandOrPropertyDisposalData(_, _, addressLine1, typeOfDisposal) =>
        val disposalType = typeOfDisposal match {
          case HowDisposed.Sold => "landOrPropertyDisposalList.methodOfDisposal.sold"
          case HowDisposed.Transferred => "landOrPropertyDisposalList.methodOfDisposal.transferred"
          case HowDisposed.Other(_) => "landOrPropertyDisposalList.methodOfDisposal.other"
        }
        Message(messageString, addressLine1, disposalType)
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    addressesWithIndexes: List[((Max5000, List[Max50]), Address)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int,
    userAnswers: UserAnswers,
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    maximumDisposalsReached: Boolean,
    allPropertiesFullyDisposed: Boolean
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

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxLandOrProperties * maxLandOrPropertyDisposals) {
        Message("landOrPropertyDisposalList.inset")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals || allPropertiesFullyDisposed) {
        ParagraphMessage("landOrPropertyDisposal.landOrPropertyDisposalList.inset.allLandOrPropertyDisposed.paragraph1") ++
          ParagraphMessage(
            "landOrPropertyDisposal.landOrPropertyDisposalList.inset.allLandOrPropertyDisposed.paragraph2"
          )
      } else {
        Message("")
      }
    }

    val showRadios = !maximumDisposalsReached && !mode.isViewOnlyMode && numberOfDisposals < maxPossibleNumberOfDisposals && !allPropertiesFullyDisposed

    val description = Option.when(
      numberOfDisposals < maxPossibleNumberOfDisposals && !maximumDisposalsReached && !allPropertiesFullyDisposed
    )(
      ParagraphMessage("landOrPropertyDisposalList.description")
    )

    FormPageViewModel(
      title = title,
      heading = heading,
      description = description,
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, mode, addressesWithIndexes, userAnswers, viewOnlyViewModel, schemeName),
        Message("landOrPropertyDisposalList.radios"),
        showRadios = showRadios,
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

  case class LandOrPropertyDisposalData(
    addressIndex: Max5000,
    disposalIndex: Max50,
    addressLine1: String,
    disposalMethod: HowDisposed
  )
}
