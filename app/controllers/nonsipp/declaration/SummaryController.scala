/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.nonsipp.declaration

import services._
import viewmodels.implicits._
import play.api.mvc._
import utils.nonsipp.summary._
import controllers.PSRController
import cats.implicits.{toShow, _}
import controllers.actions.IdentifyAndRequireData
import models.requests.DataRequest
import cats.data.EitherT
import views.html.SummaryView
import models.SchemeId.Srn
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbStatus}
import play.api.Logger
import utils.nonsipp.TaskListUtils
import models.backend.responses.ReportStatus
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeUtils.localDateShow
import models.{DateRange, NormalMode}
import play.api.i18n._
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message
import viewmodels.models.{SummaryPageEntry, _}

import scala.concurrent.{ExecutionContext, Future}

import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SummaryController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: SummaryView,
  val schemeDateService: SchemeDateService,
  val saveService: SaveService,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  val logger = Logger(getClass)
  def declarationLink(srn: Srn)(using request: DataRequest[AnyContent]) = if (request.pensionSchemeId.isPSP) {
    routes.PspDeclarationController.onPageLoad(srn)
  } else {
    routes.PsaDeclarationController.onPageLoad(srn)
  }

  def entries(srn: Srn)(using
    request: DataRequest[AnyContent],
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, List[SummaryPageEntry]] = {
    val landOrPropertyDisposalCheckAnswersUtils = LandOrPropertyDisposalCheckAnswersUtils(saveService)
    val loansCheckAnswersUtils = LoansCheckAnswersUtils(schemeDateService)

    given SchemeDateService = schemeDateService

    List(
      // scheme details
      BasicDetailsCheckAnswersUtils.sectionEntries(srn, NormalMode),
      FinancialDetailsCheckAnswersUtils.sectionEntries(srn, NormalMode),

      // members
      MemberDetailsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),

      // member payments
      EmployerContributionsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      UnallocatedContributionsCheckAnswersUtils.sectionEntries(srn, NormalMode),
      MemberContributionsCheckAnswersUtils.sectionEntries(srn, NormalMode),
      TransfersInCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      TransfersOutCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      PclsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      MemberPaymentsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      MemberSurrenderedBenefitsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),

      // loans
      loansCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      MoneyBorrowedCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),

      // shares
      SharesCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      SharesDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),

      // land or property
      LandOrPropertyCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      landOrPropertyDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),

      // bonds
      BondsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      BondsDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),

      // other assets
      TotalValueQuotedSharesCheckAnswersUtils.sectionEntries(srn, NormalMode),
      OtherAssetsCheckAnswersUtils.allSectionEntriesT(srn, NormalMode),
      OtherAssetsDisposalCheckAnswersUtils.allSectionEntriesT(srn, NormalMode)
    ).sequence
      .map(_.flatten)
  }

  def schemeDate(srn: Srn)(using DataRequest[AnyContent]): Option[DisplayMessage] = schemeDateService
    .taxYearOrAccountingPeriods(srn)
    .map(_.map(x => DateRange(x.head._1.from, x.reverse.head._1.to)).merge)
    .map(x => Message("nonsipp.summary.caption", x.from.show, x.to.show))

  def onPageLoadPostSubmission(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    if (!request.userAnswers.get(FbStatus(srn)).exists(_.isSubmitted)) {
      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    } else {
      val lastSubmittedDateTime = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)).map { dateTime =>
        Message(
          "nonsipp.summary.message.submittedOn",
          dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
          dateTime.format(DateTimeFormatter.ofPattern("h:mma"))
        )
      }

      entries(srn)
        .map(entries =>
          Ok(view(viewModel(entries, declarationLink(srn)), schemeDate(srn), false, lastSubmittedDateTime))
        )
        .value
        .map(_.merge)
    }
  }

  def onPageLoad(srn: Srn, startDate: String): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      (for {
        versions <- EitherT.right[Result](
          psrVersionsService
            .getVersions(request.schemeDetails.pstr, startDate, srn)
        )
        lastVersion = versions.maxBy(_.reportVersion)
        lastVersionDateTime = lastVersion.compilationOrSubmissionDate
        schemeDate = schemeDateService
          .taxYearOrAccountingPeriods(srn)
          .map(_.map(x => DateRange(x.head._1.from, x.reverse.head._1.to)).merge)
          .map(x => Message("nonsipp.summary.caption", x.from.show, x.to.show))
        date = lastVersionDateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        time = lastVersionDateTime.format(DateTimeFormatter.ofPattern("h:mma"))

        submittedAnswers <- EitherT.right[Result](
          psrRetrievalService.getAndTransformStandardPsrDetails(
            None,
            Some(startDate),
            Some("%03d".format(lastVersion.reportVersion)),
            controllers.routes.OverviewController.onPageLoad(request.srn)
          )
        )
        isSubmitted = lastVersion.reportStatus == ReportStatus.SubmittedAndSuccessfullyProcessed

        entries <- entries(srn)(using
          request.copy(
            userAnswers = submittedAnswers
          )
        )
      } yield Ok(
        view(
          viewModel(entries, declarationLink(srn)),
          schemeDate,
          !isSubmitted,
          Option.when(isSubmitted)(Message("nonsipp.summary.message.submittedOn", date, time))
        )
      )).value
        .map(_.merge)
  }

  def onPageLoadPreSubmission(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    if (
      !TaskListUtils.isDeclarationReady(
        srn,
        request.schemeDetails.schemeName,
        request.userAnswers,
        request.pensionSchemeId,
        isPrePopulation
      )
    ) {
      logger.warn("Cannot render summary page for return, data is incomplete")
      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    } else {

      entries(srn)
        .map(entries => Ok(view(viewModel(entries, declarationLink(srn)), schemeDate(srn), true, None)))
        .value
        .map(_.merge)
    }
  }

  private def viewModel(
    entries: List[SummaryPageEntry],
    declarationLink: Call
  ): FormPageViewModel[List[SummaryPageEntry]] = FormPageViewModel[List[SummaryPageEntry]](
    Message("nonsipp.summary.title"),
    Message("nonsipp.summary.heading"),
    entries,
    declarationLink
  )
}
