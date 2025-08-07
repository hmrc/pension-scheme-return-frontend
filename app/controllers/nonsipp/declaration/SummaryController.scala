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

import services.*
import viewmodels.implicits.*
import play.api.mvc.*
import utils.nonsipp.summary.*
import controllers.PSRController
import cats.implicits.{toShow, *}
import controllers.actions.IdentifyAndRequireData
import play.api.i18n.*
import models.requests.DataRequest
import cats.data.EitherT
import views.html.SummaryView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import utils.nonsipp.TaskListUtils
import models.backend.responses.ReportStatus.SubmittedAndSuccessfullyProcessed
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeUtils.localDateShow
import models.{DateRange, NormalMode}
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message
import viewmodels.models.{SummaryPageEntry, *}

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

  def onPageLoadPostSubmissionFromSession(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>

      val lastSubmittedDateTime = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)).map { dateTime =>
        Message(
          "nonsipp.summary.message.submittedOn",
          dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
          dateTime.format(DateTimeFormatter.ofPattern("h:mma"))
        )
      }

      entries(srn)
        .map(entries => Ok(view(postSubmissionViewModel(entries), schemeDate(srn), false, lastSubmittedDateTime)))
        .value
        .map(_.merge)
  }

  def onPageLoadPostSubmission(srn: Srn, startDate: String): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      (for {
        versions <- EitherT.right[Result](
          psrVersionsService
            .getVersions(request.schemeDetails.pstr, startDate, srn)
        )
        submittedVersions = versions
          .filter(_.reportStatus == SubmittedAndSuccessfullyProcessed)
        lastSubmitted = submittedVersions
          .maxByOption(_.compilationOrSubmissionDate)
          .getOrElse(versions.maxBy(_.compilationOrSubmissionDate))
        lastSubmittedDateTime = lastSubmitted.compilationOrSubmissionDate
        schemeDate = schemeDateService
          .taxYearOrAccountingPeriods(srn)
          .map(_.map(x => DateRange(x.head._1.from, x.reverse.head._1.to)).merge)
          .map(x => Message("nonsipp.summary.caption", x.from.show, x.to.show))
        submittedDate = lastSubmittedDateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        submittedTime = lastSubmittedDateTime.format(DateTimeFormatter.ofPattern("h:mma"))

        submittedAnswers <- EitherT.right[Result](
          psrRetrievalService.getAndTransformStandardPsrDetails(
            None,
            Some(startDate),
            Some("%03d".format(lastSubmitted.reportVersion)),
            controllers.routes.OverviewController.onPageLoad(request.srn)
          )
        )

        entries <- entries(srn)(using
          request.copy(
            userAnswers = submittedAnswers
          )
        )
      } yield Ok(
        view(
          postSubmissionViewModel(entries),
          schemeDate,
          false,
          Some(Message("nonsipp.summary.message.submittedOn", submittedDate, submittedTime))
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

      val declarationLink = if (request.pensionSchemeId.isPSP) {
        routes.PspDeclarationController.onPageLoad(srn)
      } else {
        routes.PsaDeclarationController.onPageLoad(srn)
      }

      entries(srn)
        .map(entries => Ok(view(preSubmissionViewModel(entries, declarationLink), schemeDate(srn), true, None)))
        .value
        .map(_.merge)
    }
  }

  private def preSubmissionViewModel(
    entries: List[SummaryPageEntry],
    declarationLink: Call
  ): FormPageViewModel[List[SummaryPageEntry]] = FormPageViewModel[List[SummaryPageEntry]](
    Message("nonsipp.summary.title"),
    Message("nonsipp.summary.heading"),
    entries,
    declarationLink
  )

  private def postSubmissionViewModel(
    entries: List[SummaryPageEntry]
  ): FormPageViewModel[List[SummaryPageEntry]] = FormPageViewModel[List[SummaryPageEntry]](
    Message("nonsipp.summary.title"),
    Message("nonsipp.summary.heading"),
    entries,
    Call("GET", "#")
  )
}
