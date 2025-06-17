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

package controllers.nonsipp

import services.PsrVersionsService
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.userAnswersUnchangedAllSections
import controllers.actions._
import pages.nonsipp.accountingperiod.AccountingPeriods
import _root_.config.Constants.defaultFbVersion
import models.backend.responses.{PsrVersionsResponse, ReportStatus}
import viewmodels.models.TaskListStatus._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import views.html.TaskListView
import models.SchemeId.Srn
import cats.implicits.toShow
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import pages.nonsipp._
import play.api.Logging
import utils.nonsipp.TaskListUtils._
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

class TaskListController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  psrVersionsService: PsrVersionsService
)(implicit ec: ExecutionContext)
    extends PSRController
    with Logging {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    def isSubmitted(psrVersionsResponse: PsrVersionsResponse): Boolean = (
      psrVersionsResponse.reportStatus == ReportStatus.SubmittedAndInProgress
        || psrVersionsResponse.reportStatus == ReportStatus.SubmittedAndSuccessfullyProcessed
    )

    withCompletedBasicDetails(srn) { dates =>
      for {
        response <- psrVersionsService.getVersions(request.schemeDetails.pstr, formatDateForApi(dates.from), srn)
        lastVersion = if (response.nonEmpty) response.maxBy(_.reportVersion).reportVersion else defaultFbVersion.toInt
        fbVersion = request.userAnswers.get(FbVersionPage(srn)).getOrElse(defaultFbVersion).toInt
        hasHistory = response.exists(isSubmitted)
        noChangesSincePreviousVersion =
          if (!hasHistory || request.previousUserAnswers.isEmpty) {
            true
          } else {
            userAnswersUnchangedAllSections(
              request.userAnswers,
              if (isSubmitted(response.maxBy(_.reportVersion))) {
                request.pureUserAnswers.get
              } else {
                request.previousUserAnswers.get
              }
            )
          }
        viewModel = TaskListController.viewModel(
          srn,
          request.schemeDetails.schemeName,
          dates.from,
          dates.to,
          request.userAnswers,
          request.pensionSchemeId,
          hasHistory,
          noChangesSincePreviousVersion,
          request.previousUserAnswers,
          request.pureUserAnswers,
          isPrePopulation
        )
      } yield
        if (fbVersion < lastVersion && hasHistory && noChangesSincePreviousVersion) {
          logger.warn(
            s"[TaskListController] fbVersion ($fbVersion) < lastVersion($lastVersion) and return hasn't just changed, redirecting to overview page"
          )
          Redirect(controllers.routes.OverviewController.onPageLoad(srn).url)
        } else {
          Ok(view(viewModel, request.schemeDetails.schemeName))
        }
    }
  }

  private def withCompletedBasicDetails(
    srn: Srn
  )(f: DateRange => Future[Result])(implicit request: DataRequest[_]): Future[Result] = {
    val basicDetails: Option[DateRange] = for {
      _ <- request.userAnswers.get(CheckReturnDatesPage(srn)).flatMap {
        case false => request.userAnswers.get(AccountingPeriods(srn))
        case _ => Some(())
      }
      _ <- request.userAnswers.get(ActiveBankAccountPage(srn)).flatMap {
        case false => request.userAnswers.get(WhyNoBankAccountPage(srn))
        case _ => Some(())
      }
      _ <- request.userAnswers.get(HowManyMembersPage.bySrn(srn))
      dates <- request.userAnswers.get(WhichTaxYearPage(srn))
    } yield dates

    basicDetails.fold {
      logger.info("[TaskListController] Unable to retrieve basic details, redirecting to overview page")
      Future.successful(
        Redirect(
          controllers.routes.JourneyRecoveryController.onPageLoad(
            Some(RedirectUrl(controllers.routes.OverviewController.onPageLoad(srn).url))
          )
        )
      )
    }(f)
  }
}

object TaskListController {

  def messageKey(prefix: String, suffix: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted => s"$prefix.add.$suffix"
      case _ => s"$prefix.change.$suffix"
    }

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId,
    hasHistory: Boolean,
    noChangesSincePreviousVersion: Boolean,
    previousUserAnswers: Option[UserAnswers],
    pureUserAnswers: Option[UserAnswers],
    isPrePop: Boolean
  ): PageViewModel[TaskListViewModel] = {

    val sectionList =
      getSectionList(
        srn,
        schemeName,
        userAnswers,
        pensionSchemeId,
        previousUserAnswers,
        pureUserAnswers,
        startDate,
        isPrePop
      )

    val (numSectionsReadyForSubmission, numSectionsTotal) = evaluateReadyForSubmissionTotalTuple(sectionList)

    val allSectionsReadyForSubmission = numSectionsReadyForSubmission == numSectionsTotal

    val displayNotSubmittedMessage = (hasHistory, noChangesSincePreviousVersion) match {
      // In Compile mode, if all sections ready for submission, display message
      case (false, _) => allSectionsReadyForSubmission
      // In View & Change mode, if userAnswers unchanged, don't display message
      case (true, true) => false
      // In View & Change mode, if userAnswers changed & all sections ready for submission, display message
      case (true, false) => allSectionsReadyForSubmission
    }

    val historyLink = if (hasHistory) {
      Some(
        LinkMessage(
          Message("nonsipp.tasklist.history"),
          controllers.routes.ReturnsSubmittedController.onPageLoad(srn, 1).url
        )
      )
    } else {
      None
    }

    val submissionDateMessage = userAnswers
      .get(CompilationOrSubmissionDatePage(srn))
      .fold(Message(""))(date => Message("site.submittedOn", date.show))

    val viewModel = TaskListViewModel(
      displayNotSubmittedMessage,
      hasHistory,
      historyLink,
      submissionDateMessage,
      sectionList.head,
      sectionList.tail: _*
    )

    PageViewModel(
      Message("nonsipp.tasklist.title", startDate.show, endDate.show),
      Message("nonsipp.tasklist.heading", startDate.show, endDate.show),
      viewModel
    )
  }
}
