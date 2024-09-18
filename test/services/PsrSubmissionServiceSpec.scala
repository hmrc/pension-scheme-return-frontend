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

package services

import play.api.mvc.AnyContentAsEmpty
import connectors.PSRConnector
import controllers.TestValues
import cats.data.NonEmptyList
import transformations._
import utils.UserAnswersUtils.UserAnswersOps
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import models.{DateRange, NormalMode}
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.any
import services.PsrSubmissionServiceSpec._
import play.api.test.FakeRequest
import models.audit.{PSRCompileAuditEvent, PSRSubmissionAuditEvent}
import pages.nonsipp.schemedesignatory.{FeesCommissionsWagesSalariesPage, HowMuchCashPage, ValueOfAssetsPage}
import utils.BaseSpec
import play.api.test.Helpers.stubMessagesApi
import org.mockito.Mockito._
import models.requests.psr._
import config.Constants.{PSP, UNCHANGED_SESSION_PREFIX}
import pages.nonsipp.CheckReturnDatesPage
import play.api.libs.json.{JsArray, JsValue}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import java.time.LocalDate

class PsrSubmissionServiceSpec extends BaseSpec with TestValues {

  val schemeDateRange: DateRange = dateRangeGen.sample.value

  override def beforeEach(): Unit = {
    reset(mockConnector)
    reset(mockMinimalRequiredSubmissionTransformer)
    reset(mockLoansTransformer)
    reset(mockAssetsTransformer)
    reset(mockMemberPaymentsTransformerTransformer)
    reset(mockSharesTransformer)
    reset(mockDeclarationTransformer)
    reset(mockSchemeDateService)
    reset(mockAuditService)
    reset(mockSessionRepository)
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDateRange))
    when(mockAuditService.sendEvent(any)(any(), any())).thenReturn(Future.successful(AuditResult.Success))
  }

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockConnector = mock[PSRConnector]
  private val mockSchemeDateService = mock[SchemeDateService]
  private val mockAuditService = mock[AuditService]
  private val mockMinimalRequiredSubmissionTransformer = mock[MinimalRequiredSubmissionTransformer]
  private val mockLoansTransformer = mock[LoansTransformer]
  private val mockAssetsTransformer = mock[AssetsTransformer]
  private val mockMemberPaymentsTransformerTransformer = mock[MemberPaymentsTransformer]
  private val mockSharesTransformer = mock[SharesTransformer]
  private val mockDeclarationTransformer = mock[DeclarationTransformer]
  private val mockSessionRepository = mock[SessionRepository]

  private val service =
    new PsrSubmissionService(
      mockConnector,
      mockSchemeDateService,
      mockAuditService,
      stubMessagesApi(),
      mockMinimalRequiredSubmissionTransformer,
      mockLoansTransformer,
      mockMemberPaymentsTransformerTransformer,
      mockSharesTransformer,
      mockAssetsTransformer,
      mockDeclarationTransformer,
      mockSessionRepository
    )

  "PSRSubmissionService" - {

    "shouldn't submit PsrDetails request when initial UA not exist yet" in {
      when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
        .thenReturn(Future.successful(None))
      whenReady(service.submitPsrDetails(srn, fallbackCall = fallbackCall)) { result: Option[Unit] =>
        verify(mockMinimalRequiredSubmissionTransformer, never).transformToEtmp(any(), any(), any())(any())
        verify(mockLoansTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockAssetsTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockMemberPaymentsTransformerTransformer, never).transformToEtmp(any(), any(), any(), any())
        verify(mockDeclarationTransformer, never).transformToEtmp(any())
        verify(mockConnector, never).submitPsrDetails(any(), any(), any(), any())(any(), any())
        verify(mockAuditService, never).sendExtendedEvent(any())(any(), any())
        verify(mockSessionRepository, times(1)).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)
        result mustBe None
      }
    }

    "shouldn't submit PsrDetails request when initial UA and current UA are same and isSubmitted false" in {
      when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
        .thenReturn(Future.successful(Some(defaultUserAnswers)))
      whenReady(service.submitPsrDetails(srn, fallbackCall = fallbackCall)) { result: Option[Unit] =>
        verify(mockMinimalRequiredSubmissionTransformer, never).transformToEtmp(any(), any(), any())(any())
        verify(mockLoansTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockAssetsTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockMemberPaymentsTransformerTransformer, never).transformToEtmp(any(), any(), any(), any())
        verify(mockDeclarationTransformer, never).transformToEtmp(any())
        verify(mockConnector, never).submitPsrDetails(any(), any(), any(), any())(any(), any())
        verify(mockAuditService, never).sendExtendedEvent(any())(any(), any())
        verify(mockSessionRepository, times(1)).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)
        result mustBe Some(())
      }
    }

    "shouldn't submit PsrDetails request when userAnswer is empty (initial and current UAs are different)" in {
      when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
        .thenReturn(Future.successful(Some(emptyUserAnswers)))
      when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any(), any(), any())(any()))
        .thenReturn(Some(minimalRequiredSubmission))
      whenReady(service.submitPsrDetails(srn, fallbackCall = fallbackCall)) { result: Option[Unit] =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any(), any(), any())(any())
        verify(mockLoansTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockAssetsTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockMemberPaymentsTransformerTransformer, never).transformToEtmp(any(), any(), any(), any())
        verify(mockDeclarationTransformer, never).transformToEtmp(any())
        verify(mockConnector, never).submitPsrDetails(any(), any(), any(), any())(any(), any())
        verify(mockAuditService, never).sendExtendedEvent(any())(any(), any())
        verify(mockSessionRepository, times(1)).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)
        result mustBe None
      }
    }

    List(true, false).foreach(
      checkReturnDatesAnswer =>
        s"should submitPsrDetails request when only minimalRequiredSubmission object is exist and checkReturnDates is $checkReturnDatesAnswer" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), checkReturnDatesAnswer)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any(), any(), any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockLoansTransformer.transformToEtmp(any(), any())(any())).thenReturn(None)
          when(mockMemberPaymentsTransformerTransformer.transformToEtmp(any(), any(), any(), any())).thenReturn(None)
          when(mockAssetsTransformer.transformToEtmp(any(), any())(any())).thenReturn(None)
          when(mockSharesTransformer.transformToEtmp(any(), any())(any())).thenReturn(None)
          when(mockConnector.submitPsrDetails(any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(Right(())))
          when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
            .thenReturn(Future.successful(Some(emptyUserAnswers)))

          whenReady(
            service.submitPsrDetails(srn, fallbackCall = fallbackCall)(implicitly, implicitly, request)
          ) {
            result: Option[Unit] =>
              verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any(), any(), any())(any())
              verify(mockLoansTransformer, times(1)).transformToEtmp(any(), any())(any())
              verify(mockAssetsTransformer, times(1)).transformToEtmp(any(), any())(any())
              verify(mockSharesTransformer, times(1)).transformToEtmp(any(), any())(any())
              verify(mockDeclarationTransformer, never).transformToEtmp(any())
              verify(mockConnector, times(1))
                .submitPsrDetails(psrSubmissionCaptor.capture(), any(), any(), any())(any(), any())
              verify(mockAuditService, times(1)).sendExtendedEvent(psrCompileAuditEventCaptor.capture())(any(), any())
              verify(mockSessionRepository, times(1)).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)

              psrSubmissionCaptor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
              psrSubmissionCaptor.getValue.checkReturnDates mustBe checkReturnDatesAnswer
              psrSubmissionCaptor.getValue.loans mustBe None
              psrSubmissionCaptor.getValue.assets mustBe None
              psrSubmissionCaptor.getValue.shares mustBe None
              psrSubmissionCaptor.getValue.psrDeclaration mustBe None
              psrCompileAuditEventCaptor.getValue.taskList must not be JsArray(Array.empty[JsValue])
              result mustBe Some(())
          }
        }
    )

    s"submitPsrDetails request successfully when optional fields are not None" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
      val request = DataRequest(allowedAccessRequest, userAnswers)

      when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any(), any(), any())(any()))
        .thenReturn(Some(minimalRequiredSubmission))
      when(mockLoansTransformer.transformToEtmp(any(), any())(any())).thenReturn(optLoans)
      when(mockMemberPaymentsTransformerTransformer.transformToEtmp(any(), any(), any(), any()))
        .thenReturn(optMemberPayments)
      when(mockAssetsTransformer.transformToEtmp(any(), any())(any())).thenReturn(optAssets)
      when(mockSharesTransformer.transformToEtmp(any(), any())(any())).thenReturn(optShares)
      when(mockConnector.submitPsrDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(())))
      when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
        .thenReturn(Future.successful(Some(emptyUserAnswers)))

      whenReady(
        service.submitPsrDetails(srn, fallbackCall = fallbackCall)(implicitly, implicitly, request)
      ) { result: Option[Unit] =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1))
          .transformToEtmp(any(), any(), ArgumentMatchers.eq(false))(any())
        verify(mockLoansTransformer, times(1)).transformToEtmp(any(), any())(any())
        verify(mockMemberPaymentsTransformerTransformer, times(1)).transformToEtmp(any(), any(), any(), any())
        verify(mockAssetsTransformer, times(1)).transformToEtmp(any(), any())(any())
        verify(mockSharesTransformer, times(1)).transformToEtmp(any(), any())(any())
        verify(mockDeclarationTransformer, never).transformToEtmp(any())
        verify(mockConnector, times(1)).submitPsrDetails(psrSubmissionCaptor.capture(), any(), any(), any())(
          any(),
          any()
        )
        verify(mockAuditService, times(1)).sendExtendedEvent(psrCompileAuditEventCaptor.capture())(any(), any())
        verify(mockSessionRepository, times(1)).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)

        psrSubmissionCaptor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
        psrSubmissionCaptor.getValue.checkReturnDates mustBe false
        psrSubmissionCaptor.getValue.loans mustBe optLoans
        psrSubmissionCaptor.getValue.assets mustBe optAssets
        psrSubmissionCaptor.getValue.shares mustBe optShares
        psrSubmissionCaptor.getValue.psrDeclaration mustBe None
        psrCompileAuditEventCaptor.getValue.taskList must not be JsArray(Array.empty[JsValue])
        result mustBe Some(())
      }
    }

    s"submitPsrDetails request successfully with PsrDeclaration when isSubmitted is true and (initial UA current UA) are same" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
      val request = DataRequest(allowedAccessRequest, userAnswers)

      when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any(), any(), any())(any()))
        .thenReturn(Some(minimalRequiredSubmission))
      when(mockLoansTransformer.transformToEtmp(any(), any())(any())).thenReturn(optLoans)
      when(mockMemberPaymentsTransformerTransformer.transformToEtmp(any(), any(), any(), any()))
        .thenReturn(optMemberPayments)
      when(mockAssetsTransformer.transformToEtmp(any(), any())(any())).thenReturn(optAssets)
      when(mockSharesTransformer.transformToEtmp(any(), any())(any())).thenReturn(optShares)
      when(mockConnector.submitPsrDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(())))
      when(mockDeclarationTransformer.transformToEtmp(any())).thenReturn(declaration)
      when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
        .thenReturn(Future.successful(Some(emptyUserAnswers)))

      whenReady(service.submitPsrDetails(srn = srn, isSubmitted = true, fallbackCall)(implicitly, implicitly, request)) {
        result: Option[Unit] =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1))
            .transformToEtmp(any(), any(), ArgumentMatchers.eq(true))(any())
          verify(mockLoansTransformer, times(1)).transformToEtmp(any(), any())(any())
          verify(mockMemberPaymentsTransformerTransformer, times(1)).transformToEtmp(any(), any(), any(), any())
          verify(mockAssetsTransformer, times(1)).transformToEtmp(any(), any())(any())
          verify(mockSharesTransformer, times(1)).transformToEtmp(any(), any())(any())
          verify(mockDeclarationTransformer, times(1)).transformToEtmp(any())
          verify(mockConnector, times(1)).submitPsrDetails(psrSubmissionCaptor.capture(), any(), any(), any())(
            any(),
            any()
          )
          verify(mockAuditService, times(1)).sendExtendedEvent(psrSubmissionAuditEventCaptor.capture())(any(), any())
          verify(mockSessionRepository, times(1)).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)

          psrSubmissionCaptor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
          psrSubmissionCaptor.getValue.checkReturnDates mustBe false
          psrSubmissionCaptor.getValue.loans mustBe optLoans
          psrSubmissionCaptor.getValue.assets mustBe optAssets
          psrSubmissionCaptor.getValue.shares mustBe optShares
          psrSubmissionCaptor.getValue.psrDeclaration mustBe Some(declaration)
          psrSubmissionAuditEventCaptor.getValue.psrSubmission mustBe psrSubmissionCaptor.getValue
          result mustBe Some(())
      }
    }

    "should submit PsrDetails request when initial UA and current UA are same but isSubmitted true" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
      val request = DataRequest(allowedAccessRequest, userAnswers)

      when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any(), any(), any())(any()))
        .thenReturn(Some(minimalRequiredSubmission))
      when(mockLoansTransformer.transformToEtmp(any(), any())(any())).thenReturn(optLoans)
      when(mockMemberPaymentsTransformerTransformer.transformToEtmp(any(), any(), any(), any()))
        .thenReturn(optMemberPayments)
      when(mockAssetsTransformer.transformToEtmp(any(), any())(any())).thenReturn(optAssets)
      when(mockSharesTransformer.transformToEtmp(any(), any())(any())).thenReturn(optShares)
      when(mockConnector.submitPsrDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(())))
      when(mockDeclarationTransformer.transformToEtmp(any())).thenReturn(declaration)
      when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
        .thenReturn(Future.successful(Some(userAnswers)))

      whenReady(service.submitPsrDetails(srn = srn, isSubmitted = true, fallbackCall)(implicitly, implicitly, request)) {
        result: Option[Unit] =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1))
            .transformToEtmp(any(), any(), ArgumentMatchers.eq(true))(any())
          verify(mockLoansTransformer, times(1)).transformToEtmp(any(), any())(any())
          verify(mockMemberPaymentsTransformerTransformer, times(1)).transformToEtmp(any(), any(), any(), any())
          verify(mockAssetsTransformer, times(1)).transformToEtmp(any(), any())(any())
          verify(mockSharesTransformer, times(1)).transformToEtmp(any(), any())(any())
          verify(mockDeclarationTransformer, times(1)).transformToEtmp(any())
          verify(mockConnector, times(1)).submitPsrDetails(psrSubmissionCaptor.capture(), any(), any(), any())(
            any(),
            any()
          )
          verify(mockAuditService, times(1)).sendExtendedEvent(psrSubmissionAuditEventCaptor.capture())(any(), any())
          verify(mockSessionRepository, times(1)).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)

          psrSubmissionCaptor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
          psrSubmissionCaptor.getValue.checkReturnDates mustBe false
          psrSubmissionCaptor.getValue.loans mustBe optLoans
          psrSubmissionCaptor.getValue.assets mustBe optAssets
          psrSubmissionCaptor.getValue.shares mustBe optShares
          psrSubmissionCaptor.getValue.psrDeclaration mustBe Some(declaration)
          psrSubmissionAuditEventCaptor.getValue.psrSubmission mustBe psrSubmissionCaptor.getValue
          result mustBe Some(())
      }
    }

    "should submit PsrDetails bypassed" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(HowMuchCashPage(srn, NormalMode), moneyInPeriod)
        .unsafeSet(ValueOfAssetsPage(srn, NormalMode), moneyInPeriod)
        .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), money)
      val request = DataRequest(allowedAccessRequest, userAnswers)

      when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any(), any(), any())(any()))
        .thenReturn(
          Some(
            MinimalRequiredSubmission(
              minimalRequiredSubmission.reportDetails,
              minimalRequiredSubmission.accountingPeriodDetails,
              minimalRequiredSubmission.schemeDesignatory.copy(totalPayments = Some(money.value))
            )
          )
        )
      when(mockConnector.submitPsrDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(())))
      when(mockDeclarationTransformer.transformToEtmp(any())).thenReturn(declaration)
      when(mockSessionRepository.get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id))
        .thenReturn(Future.successful(Some(userAnswers)))

      whenReady(service.submitPsrDetailsBypassed(srn = srn, fallbackCall)(implicitly, implicitly, request)) {
        result: Option[Unit] =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1))
            .transformToEtmp(any(), any(), ArgumentMatchers.eq(true))(any())
          verify(mockLoansTransformer, never).transformToEtmp(any(), any())(any())
          verify(mockMemberPaymentsTransformerTransformer, never).transformToEtmp(any(), any(), any(), any())
          verify(mockAssetsTransformer, never).transformToEtmp(any(), any())(any())
          verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
          verify(mockDeclarationTransformer, times(1)).transformToEtmp(any())
          verify(mockConnector, times(1)).submitPsrDetails(psrSubmissionCaptor.capture(), any(), any(), any())(
            any(),
            any()
          )
          verify(mockAuditService, times(1)).sendExtendedEvent(psrSubmissionAuditEventCaptor.capture())(any(), any())
          verify(mockSessionRepository, never).get(UNCHANGED_SESSION_PREFIX + request.userAnswers.id)

          psrSubmissionCaptor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
          psrSubmissionCaptor.getValue.checkReturnDates mustBe false
          psrSubmissionCaptor.getValue.loans mustBe None
          psrSubmissionCaptor.getValue.assets mustBe None
          psrSubmissionCaptor.getValue.shares mustBe None
          psrSubmissionCaptor.getValue.psrDeclaration mustBe Some(declaration)
          result mustBe Some(())
      }
    }
  }
}

object PsrSubmissionServiceSpec {

  val psrSubmissionCaptor: ArgumentCaptor[PsrSubmission] = ArgumentCaptor.forClass(classOf[PsrSubmission])
  val psrSubmissionAuditEventCaptor: ArgumentCaptor[PSRSubmissionAuditEvent] =
    ArgumentCaptor.forClass(classOf[PSRSubmissionAuditEvent])
  val psrCompileAuditEventCaptor: ArgumentCaptor[PSRCompileAuditEvent] =
    ArgumentCaptor.forClass(classOf[PSRCompileAuditEvent])

  val sampleDate: LocalDate = LocalDate.now
  val minimalRequiredSubmission: MinimalRequiredSubmission = MinimalRequiredSubmission(
    ReportDetails(
      fbVersion = None,
      fbstatus = None,
      pstr = "testPstr",
      periodStart = sampleDate,
      periodEnd = sampleDate,
      compilationOrSubmissionDate = None
    ),
    AccountingPeriodDetails(Some("001"), NonEmptyList.of(sampleDate -> sampleDate)),
    SchemeDesignatory(
      Some("001"),
      openBankAccount = true,
      Some("reasonForNoBankAccount"),
      1,
      2,
      3,
      None,
      None,
      None,
      None,
      None
    )
  )
  val optLoans: Option[Loans] = Some(
    Loans(recordVersion = Some("001"), schemeHadLoans = true, loanTransactions = Seq.empty)
  )
  val optMemberPayments: Option[MemberPayments] = Some(
    MemberPayments(
      recordVersion = Some("001"),
      memberDetails = List.empty,
      employerContributionsDetails = SectionDetails(made = true, completed = true),
      transfersInMade = Some(true),
      transfersOutMade = Some(true),
      unallocatedContribsMade = Some(true),
      unallocatedContribAmount = None,
      memberContributionMade = Some(true),
      lumpSumReceived = Some(true),
      pensionReceived = SectionDetails(made = true, completed = true),
      benefitsSurrenderedDetails = SectionDetails(made = true, completed = true)
    )
  )
  val optAssets: Option[Assets] = Some(
    Assets(optLandOrProperty = None, optBorrowing = None, optBonds = None, optOtherAssets = None)
  )
  val optShares: Option[Shares] = Some(
    Shares(recordVersion = Some("001"), optShareTransactions = None, optTotalValueQuotedShares = None)
  )

  val declaration: PsrDeclaration = PsrDeclaration(
    submittedBy = PSP,
    submitterId = "submitterId",
    optAuthorisingPSAID = Some("authorisingPSAID"),
    declaration1 = true,
    declaration2 = true
  )
}
