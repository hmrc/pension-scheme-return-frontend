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

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.otherassetsdisposal._
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import cats.implicits._
import config.Constants.{maxDisposalPerOtherAsset, maxOtherAssetsTransactions}
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.otherassetsdisposal.ReportedOtherAssetsDisposalListController._
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import models.HowDisposed._
import com.google.inject.Inject
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
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

import java.time.LocalDateTime
import javax.inject.Named

class ReportedOtherAssetsDisposalListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = ReportedOtherAssetsDisposalListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, page, mode)(implicitly)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
    onPageLoadCommon(srn, page, mode)(implicitly)
  }

  def onPageLoadCommon(srn: Srn, page: Int, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    getCompletedDisposals(srn).map { completedDisposals =>
      if (completedDisposals.values.exists(_.nonEmpty)) {
        Ok(
          view(
            form,
            viewModel(
              srn,
              mode,
              page,
              completedDisposals,
              request.userAnswers,
              viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getCompletedOrUpdatedTaskListStatus(
                  request.userAnswers,
                  request.previousUserAnswers.get,
                  pages.nonsipp.otherassetsdisposal.Paths.assetsDisposed
                ) == Updated
              } else {
                false
              },
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
            )
          )
        )
      } else {
        Redirect(routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode))
      }
    }.merge

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      getCompletedDisposals(srn)
        .map { disposals =>
          val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
          val numberOfOtherAssetsItems = request.userAnswers.map(OtherAssetsCompleted.all(srn)).size
          val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems

          if (numberOfDisposals == maxPossibleNumberOfDisposals) {
            Redirect(
              navigator
                .nextPage(ReportedOtherAssetsDisposalListPage(srn, addDisposal = false), mode, request.userAnswers)
            ).pure[Future]
          } else {
            form
              .bindFromRequest()
              .fold(
                errors =>
                  BadRequest(
                    view(errors, viewModel(srn, mode, page, disposals, request.userAnswers, false, None, None, None))
                  ).pure[Future],
                reportAnotherDisposal =>
                  for {
                    updatedUserAnswers <- request.userAnswers
                      .setWhen(!reportAnotherDisposal)(OtherAssetsDisposalCompleted(srn), SectionCompleted)
                      .mapK[Future]
                    _ <- saveService.save(updatedUserAnswers)
                    submissionResult <- if (!reportAnotherDisposal) {
                      psrSubmissionService.submitPsrDetailsWithUA(
                        srn,
                        updatedUserAnswers,
                        fallbackCall =
                          controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                            .onPageLoad(srn, page)
                      )
                    } else {
                      Future.successful(Some(()))
                    }
                  } yield submissionResult.getOrRecoverJourney(
                    _ =>
                      Redirect(
                        navigator.nextPage(
                          ReportedOtherAssetsDisposalListPage(srn, reportAnotherDisposal),
                          mode,
                          request.userAnswers
                        )
                      )
                  )
              )
          }
        }
        .leftMap(_.pure[Future])
        .merge
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
          controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
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
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[ListRow] =
    disposals
      .flatMap {
        case (otherAssetsIndex, disposalIndexes) =>
          disposalIndexes.map { disposalIndex =>
            val otherAssetsDisposalData = OtherAssetsDisposalData(
              otherAssetsIndex,
              disposalIndex,
              userAnswers.get(WhatIsOtherAssetPage(srn, otherAssetsIndex)).get,
              userAnswers.get(HowWasAssetDisposedOfPage(srn, otherAssetsIndex, disposalIndex)).get
            )

            if (mode.isViewOnlyMode) {
              (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                case (ViewOnlyMode, Some(year), Some(current), Some(previous)) =>
                  ListRow.view(
                    buildMessage("assetDisposal.reportedOtherAssetsDisposalList.row", otherAssetsDisposalData),
                    routes.AssetDisposalCYAController
                      .onPageLoadViewOnly(srn, otherAssetsIndex, disposalIndex, year, current, previous)
                      .url,
                    buildMessage(
                      "assetDisposal.reportedOtherAssetsDisposalList.row.view.hidden",
                      otherAssetsDisposalData
                    )
                  )
              }
            } else {
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
      }
      .toList
      .sortBy(_.change.fold("")(_.url))

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
    disposals: Map[Max5000, List[Max50]],
    userAnswers: UserAnswers,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ListViewModel] = {

    val numberOfDisposals = disposals.map { case (_, disposalIndexes) => disposalIndexes.size }.sum
    val numberOfOtherAssetsItems = userAnswers.map(OtherAssetsCompleted.all(srn)).size
    val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems

    val (title, heading) = ((mode, numberOfDisposals) match {

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
      numberOfDisposals,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          routes.ReportedOtherAssetsDisposalListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, _)
      }
    )

    val conditionalInsetText: DisplayMessage = {
      if (numberOfDisposals >= maxOtherAssetsTransactions) {
        Message("assetDisposal.reportedOtherAssetsDisposalList.inset.maximumReached")
      } else if (numberOfDisposals >= maxPossibleNumberOfDisposals) {
        ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.inset.allOtherAssetsDisposed.paragraph1") ++
          ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.inset.allOtherAssetsDisposed.paragraph2")
      } else {
        Message("")
      }
    }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = Option.when(
        !((numberOfDisposals >= maxOtherAssetsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals))
      )(
        ParagraphMessage("assetDisposal.reportedOtherAssetsDisposalList.description")
      ),
      page = ListViewModel(
        inset = conditionalInsetText,
        rows(srn, mode, disposals, userAnswers, optYear, optCurrentVersion, optPreviousVersion),
        Message("assetDisposal.reportedOtherAssetsDisposalList.radios"),
        showRadios =
          !((numberOfDisposals >= maxOtherAssetsTransactions) | (numberOfDisposals >= maxPossibleNumberOfDisposals)),
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
      optViewOnlyDetails = Option.when(mode.isViewOnlyMode) {
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if (optYear.nonEmpty && currentVersion > 1 && previousVersion > 0) =>
              Some(
                LinkMessage(
                  "assetDisposal.reportedOtherAssetsDisposalList.view.link",
                  controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                    .onPreviousViewOnly(
                      srn,
                      page,
                      year,
                      currentVersion,
                      previousVersion
                    )
                    .url
                )
              )
            case _ => None
          },
          submittedText =
            compilationOrSubmissionDate.fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
          title = title,
          heading = heading,
          buttonText = "site.return.to.tasklist",
          onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion)) =>
              controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                .onSubmit(srn, page, mode)
          }
        )
      }
    )
  }

  case class OtherAssetsDisposalData(
    otherAssetsIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    disposalMethod: HowDisposed
  )
}
