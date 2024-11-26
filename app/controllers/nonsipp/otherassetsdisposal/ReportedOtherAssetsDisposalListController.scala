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

package controllers.nonsipp.otherassetsdisposal

import services.SaveService
import pages.nonsipp.otherassetsdisposal._
import viewmodels.implicits._
import play.api.mvc._
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import cats.implicits._
import _root_.config.Constants
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.otherassetsdisposal.ReportedOtherAssetsDisposalListController._
import _root_.config.Constants.{maxDisposalPerOtherAsset, maxOtherAssetsTransactions}
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import play.api.mvc.Results.Redirect
import _root_.config.RefinedTypes.{Max50, Max5000}
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import models.HowDisposed._
import com.google.inject.Inject
import views.html.ListView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class ReportedOtherAssetsDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = ReportedOtherAssetsDisposalListController.form(formProvider)

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
      viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
        getCompletedOrUpdatedTaskListStatus(
          request.userAnswers,
          request.previousUserAnswers.get,
          pages.nonsipp.otherassetsdisposal.Paths.assetsDisposed
        ) == Updated
      } else {
        false
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
  ): Result =
    getCompletedDisposals(srn).map { completedDisposals =>
      val numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
      val numberOfOtherAssetsItems = request.userAnswers.map(OtherAssetsCompleted.all(srn)).size
      val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems

      getOtherAssetsDisposalsWithIndexes(srn, completedDisposals).map { assetsDisposalsWithIndexes =>
        val allAssetsFullyDisposed: Boolean = assetsDisposalsWithIndexes.forall {
          case ((assetIndex, disposalIndexes), _) =>
            disposalIndexes.exists { disposalIndex =>
              request.userAnswers.get(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex)).contains(false)
            }
        }

        val maximumDisposalsReached = numberOfDisposals >= maxOtherAssetsTransactions * maxDisposalPerOtherAsset ||
          numberOfDisposals >= maxPossibleNumberOfDisposals ||
          allAssetsFullyDisposed

        if (viewOnlyViewModel.nonEmpty || completedDisposals.values.exists(_.nonEmpty)) {
          Ok(
            view(
              form,
              viewModel(
                srn,
                mode,
                page,
                assetsDisposalsWithIndexes,
                numberOfDisposals,
                maxPossibleNumberOfDisposals,
                request.userAnswers,
                request.schemeDetails.schemeName,
                viewOnlyViewModel,
                showBackLink = showBackLink,
                maximumDisposalsReached = maximumDisposalsReached,
                allAssetsFullyDisposed = allAssetsFullyDisposed
              )
            )
          )
        } else {
          Redirect(routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode))
        }
      }.merge
    }.merge

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getCompletedDisposals(srn)
        .traverse { completedDisposals =>
          val numberOfDisposals = completedDisposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfOtherAssetsItems = request.userAnswers.map(OtherAssetsCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems

          val allAssetsFullyDisposed: Boolean = completedDisposals.forall {
            case (assetIndex, disposalIndexes) =>
              disposalIndexes.exists { disposalIndex =>
                request.userAnswers.get(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex)).contains(false)
              }
          }

          val maximumDisposalsReached = numberOfDisposals >= maxOtherAssetsTransactions * maxDisposalPerOtherAsset ||
            numberOfDisposals >= maxPossibleNumberOfDisposals ||
            allAssetsFullyDisposed

          if (maximumDisposalsReached) {
            Redirect(
              navigator
                .nextPage(ReportedOtherAssetsDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors =>
                  getOtherAssetsDisposalsWithIndexes(srn, completedDisposals)
                    .map { assetsDisposalsWithIndexes =>
                      BadRequest(
                        view(
                          errors,
                          viewModel(
                            srn,
                            mode,
                            page,
                            assetsDisposalsWithIndexes,
                            numberOfDisposals,
                            maxPossibleNumberOfDisposals,
                            request.userAnswers,
                            request.schemeDetails.schemeName,
                            viewOnlyViewModel = None,
                            showBackLink = true,
                            maximumDisposalsReached = maximumDisposalsReached,
                            allAssetsFullyDisposed = allAssetsFullyDisposed
                          )
                        )
                      )
                    }
                    .merge
                    .pure[Future],
                reportAnotherDisposal =>
                  if (reportAnotherDisposal) {
                    Redirect(
                      navigator.nextPage(
                        ReportedOtherAssetsDisposalListPage(srn, reportAnotherDisposal),
                        mode,
                        request.userAnswers
                      )
                    ).pure[Future]
                  } else {
                    for {
                      updatedUserAnswers <- request.userAnswers
                        .set(OtherAssetsDisposalCompleted(srn), SectionCompleted)
                        .mapK[Future]
                      _ <- saveService.save(updatedUserAnswers)
                    } yield Redirect(
                      navigator.nextPage(
                        ReportedOtherAssetsDisposalListPage(srn, reportAnotherDisposal),
                        mode,
                        updatedUserAnswers
                      )
                    )
                  }
              )
          }
        }
        .map(_.merge)
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
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

  private def getCompletedDisposals(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, Map[Max5000, List[Max50]]] =
    request.userAnswers
      .map(OtherAssetsDisposalProgress.all(srn))
      .map {
        case (key, secondaryMap) =>
          key -> secondaryMap.filter { case (_, status) => status.completed }
      }
      .toList
      .sortBy(_._1)
      .traverse {
        case (key, sectionCompleted) =>
          for {
            otherAssetsIndex <- refineStringIndex[Max5000.Refined](key).getOrRecoverJourney
            disposalIndexes <- sectionCompleted.keys.toList
              .map(refineStringIndex[Max50.Refined])
              .traverse(_.getOrRecoverJourney)
          } yield (otherAssetsIndex, disposalIndexes)
      }
      .map(_.toMap)
}

object ReportedOtherAssetsDisposalListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "assetDisposal.reportedOtherAssetsDisposalList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers,
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    if (disposals.isEmpty) {
      List(
        ListRow.viewNoLink(
          Message("assetDisposal.reportedOtherAssetsDisposalList.view.none", schemeName),
          "assetDisposal.reportedOtherAssetsDisposalList.view.none.value"
        )
      )
    } else {
      disposals.flatMap {
        case (otherAssetsIndex, disposalIndexes) =>
          disposalIndexes.sortBy(_.value).map { disposalIndex =>
            val otherAssetsDisposalData = OtherAssetsDisposalData(
              otherAssetsIndex,
              disposalIndex,
              userAnswers.get(WhatIsOtherAssetPage(srn, otherAssetsIndex)).get,
              userAnswers.get(HowWasAssetDisposedOfPage(srn, otherAssetsIndex, disposalIndex)).get
            )

            (mode, viewOnlyViewModel) match {
              case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _))) =>
                ListRow.view(
                  buildMessage("assetDisposal.reportedOtherAssetsDisposalList.row", otherAssetsDisposalData),
                  routes.AssetDisposalCYAController
                    .onPageLoadViewOnly(srn, otherAssetsIndex, disposalIndex, year, currentVersion, previousVersion)
                    .url,
                  buildMessage(
                    "assetDisposal.reportedOtherAssetsDisposalList.row.view.hidden",
                    otherAssetsDisposalData
                  )
                )
              case (_, _) =>
                ListRow(
                  buildMessage("assetDisposal.reportedOtherAssetsDisposalList.row", otherAssetsDisposalData),
                  changeUrl = routes.AssetDisposalCYAController
                    .onPageLoad(srn, otherAssetsIndex, disposalIndex, CheckMode)
                    .url,
                  changeHiddenText = buildMessage(
                    "assetDisposal.reportedOtherAssetsDisposalList.row.change.hidden",
                    otherAssetsDisposalData
                  ),
                  removeUrl = routes.RemoveAssetDisposalController
                    .onPageLoad(srn, otherAssetsIndex, disposalIndex)
                    .url,
                  removeHiddenText = buildMessage(
                    "assetDisposal.reportedOtherAssetsDisposalList.row.remove.hidden",
                    otherAssetsDisposalData
                  )
                )
            }
          }
      }.toList
    }

  private def getOtherAssetsDisposalsWithIndexes(srn: Srn, disposals: Map[Max5000, List[Max50]])(
    implicit request: DataRequest[_]
  ): Either[Result, List[((Max5000, List[Max50]), SectionCompleted)]] =
    disposals
      .map {
        case indexes @ (index, _) =>
          index -> request.userAnswers
            .get(OtherAssetsCompleted(srn, index))
            .toRight(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            .map(otherAssetCompletionStatus => (indexes, otherAssetCompletionStatus))
      }
      .toList
      .sortBy { case (index, _) => index.value }
      .map { case (_, result) => result }
      .sequence

  private def buildMessage(messageString: String, otherAssetsDisposalData: OtherAssetsDisposalData): Message =
    otherAssetsDisposalData match {
      case OtherAssetsDisposalData(_, _, companyName, typeOfDisposal) =>
        val disposalType = typeOfDisposal match {
          case Sold => "assetDisposal.reportedOtherAssetsDisposalList.methodOfDisposal.sold"
          case Transferred => "assetDisposal.reportedOtherAssetsDisposalList.methodOfDisposal.transferred"
          case Other(_) => "assetDisposal.reportedOtherAssetsDisposalList.methodOfDisposal.other"
        }
        Message(messageString, companyName, disposalType)
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    assetsDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)],
    numberOfDisposals: Int,
    maxPossibleNumberOfDisposals: Int,
    userAnswers: UserAnswers,
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    maximumDisposalsReached: Boolean,
    allAssetsFullyDisposed: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val disposals: Map[Max5000, List[Max50]] =
      assetsDisposalsWithIndexes.map { case ((assetIndex, disposalIndexes), _) => (assetIndex, disposalIndexes) }.toMap

    val (title, heading) = ((mode, numberOfDisposals) match {

      case (ViewOnlyMode, numberOfDisposals) if numberOfDisposals == 0 =>
        (
          "assetDisposal.reportedOtherAssetsDisposalList.view.title",
          "assetDisposal.reportedOtherAssetsDisposalList.view.heading.none"
        )

      case (ViewOnlyMode, 1) =>
        (
          "assetDisposal.reportedOtherAssetsDisposalList.view.title",
          "assetDisposal.reportedOtherAssetsDisposalList.view.heading"
        )
      case (ViewOnlyMode, _) =>
        (
          "assetDisposal.reportedOtherAssetsDisposalList.view.title.plural",
          "assetDisposal.reportedOtherAssetsDisposalList.view.heading.plural"
        )
      case (_, 1) =>
        (
          "assetDisposal.reportedOtherAssetsDisposalList.title",
          "assetDisposal.reportedOtherAssetsDisposalList.heading"
        )
      case (_, _) =>
        (
          "assetDisposal.reportedOtherAssetsDisposalList.title.plural",
          "assetDisposal.reportedOtherAssetsDisposalList.heading.plural"
        )
    }) match {
      case (title, heading) =>
        (Message(title, numberOfDisposals), Message(heading, numberOfDisposals))
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.reportedOtherAssetsDisposalListSize,
      totalSize = numberOfDisposals,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.ReportedOtherAssetsDisposalListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case None =>
          routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, _)
      }
    )

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxOtherAssetsTransactions * maxDisposalPerOtherAsset) {
        Message("assetDisposal.reportedOtherAssetsDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals || allAssetsFullyDisposed) {
        ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.inset.allOtherAssetsDisposed.paragraph1") ++
          ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.inset.allOtherAssetsDisposed.paragraph2")
      } else {
        Message("")
      }
    }

    val showRadios = !maximumDisposalsReached && !mode.isViewOnlyMode &&
      numberOfDisposals < maxPossibleNumberOfDisposals && !allAssetsFullyDisposed

    val description = Option.when(
      !maximumDisposalsReached && !allAssetsFullyDisposed
    )(
      ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.description")
    )

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = description,
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, mode, disposals, userAnswers, viewOnlyViewModel, schemeName),
        Message("assetDisposal.reportedOtherAssetsDisposalList.radios"),
        showRadios = showRadios,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "assetDisposal.reportedOtherAssetsDisposalList.pagination.label",
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
      onSubmit = routes.ReportedOtherAssetsDisposalListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "assetDisposal.reportedOtherAssetsDisposalList.view.link",
                controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
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
          onSubmit = viewOnlyViewModel match {
            case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
              controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                .onSubmit(srn, page, mode)
          }
        )
      },
      showBackLink = showBackLink
    )
  }

  case class OtherAssetsDisposalData(
    otherAssetsIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    disposalMethod: HowDisposed
  )
}
