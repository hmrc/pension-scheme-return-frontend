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

package controllers.nonsipp.declaration

import services._
import controllers.nonsipp.declaration.PspDeclarationController._
import connectors.EmailConnector
import controllers.{ControllerBaseSpec, TestUserAnswers}
import play.api.inject.bind
import views.html.PsaIdInputView
import forms.TextFormProvider
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import models.DateRange
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import pages.nonsipp.declaration.PspDeclarationPage
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import models.audit.PSRSubmissionEmailAuditEvent
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.FbVersionPage
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future

import java.time.LocalDateTime

class PspDeclarationControllerSpec
    extends ControllerBaseSpec
    with BeforeAndAfterEach
    with TestUserAnswers
    with CommonTestValues {
  private val populatedUserAnswers = {
    defaultUserAnswers.unsafeSet(PspDeclarationPage(srn), psaId.value)
    defaultUserAnswers.unsafeSet(FbVersionPage(srn), version)
  }
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private implicit val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]
  private val mockAuditService = mock[AuditService]
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val schemeDatePeriod: DateRange = dateRangeGen.sample.value
  private val templateId = "pods_pension_scheme_return_submitted"

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    reset(mockAuditService)
    reset(mockEmailConnector)
    reset(mockSchemeDateService)
    reset(mockPsrVersionsService)
    reset(mockPsrRetrievalService)
    super.beforeEach()
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService),
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[AuditService].toInstance(mockAuditService),
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrVersionsService].toInstance(mockPsrVersionsService),
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
  )

  "PspDeclarationController" - {

    lazy val viewModel = PspDeclarationController.viewModel(srn)

    lazy val onPageLoad = routes.PspDeclarationController.onPageLoad(srn)
    lazy val onSubmit = routes.PspDeclarationController.onSubmit(srn)
    lazy val emailAuditEventCaptor: ArgumentCaptor[PSRSubmissionEmailAuditEvent] =
      ArgumentCaptor.forClass(classOf[PSRSubmissionEmailAuditEvent])

    act.like(renderView(onPageLoad, isPsa = false) { implicit app => implicit request =>
      injected[PsaIdInputView]
        .apply(form(injected[TextFormProvider], Some(psaId.value)), viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      agreeAndContinue(onSubmit, populatedUserAnswers, emptyUserAnswers, "value" -> psaId.value)
        .before({
          MockPsrSubmissionService.submitPsrDetails()
          MockEmailConnector.sendEmail(email, templateId)
          when(mockSchemeDateService.returnPeriodsAsJsonString(any())(any())).thenReturn("")
          when(mockSchemeDateService.submissionDateAsString(any())).thenReturn("")
          when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDatePeriod))
          when(mockSchemeDateService.now()).thenReturn(LocalDateTime.now())

          when(mockAuditService.sendEvent(emailAuditEventCaptor.capture())(any(), any()))
            .thenReturn(Future.successful(AuditResult.Success))
        })
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
          verify(mockEmailConnector, times(1))
            .sendEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any())
          verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
          emailAuditEventCaptor.getValue.schemeAdministratorOrPractitionerName mustEqual defaultMinimalDetails.individualDetails.get.fullName
        })
        .withName("agree and continue should submit PSR details, send email and audit")
    )

    act.like(
      agreeAndContinue(
        onSubmit,
        currentTaxYearUserAnswersWithManyMembers
          .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
          .unsafeSet(LoansMadeOrOutstandingPage(srn), false),
        emptyUserAnswers,
        "value" -> psaId.value
      ).before({
          when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDatePeriod))
          when(mockSchemeDateService.returnPeriodsAsJsonString(any())(any())).thenReturn("")
          when(mockSchemeDateService.submissionDateAsString(any())).thenReturn("")
          when(mockSchemeDateService.now()).thenReturn(LocalDateTime.now())
          when(mockAuditService.sendEvent(emailAuditEventCaptor.capture())(any(), any()))
            .thenReturn(Future.successful(AuditResult.Success))
          when(
            mockPsrRetrievalService
              .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
          ).thenReturn(Future.successful(emptyUserAnswers))
            .thenReturn(Future.successful(emptyUserAnswers))
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(versionsResponse))
          MockPsrSubmissionService.submitPsrDetailsBypassed()
          MockEmailConnector.sendEmail(email, templateId)
        })
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetails(any(), any(), any())(any(), any(), any())
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsBypassed(any(), any())(any(), any(), any())
          verify(mockEmailConnector, times(1))
            .sendEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any())
          verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
          emailAuditEventCaptor.getValue.schemeAdministratorOrPractitionerName mustEqual defaultMinimalDetails.individualDetails.get.fullName
        })
        .withName(
          "when there are no members in the previous returns, agree and continue should submit PSR details bypassed, send email and audit "
        )
    )

    act.like(
      agreeAndContinue(
        onSubmit,
        currentTaxYearUserAnswersWithManyMembers
          .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
          .unsafeSet(LoansMadeOrOutstandingPage(srn), false),
        emptyUserAnswers,
        "value" -> psaId.value
      ).before({
          when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDatePeriod))
          when(mockSchemeDateService.returnPeriodsAsJsonString(any())(any())).thenReturn("")
          when(mockSchemeDateService.submissionDateAsString(any())).thenReturn("")
          when(mockSchemeDateService.now()).thenReturn(LocalDateTime.now())
          when(mockAuditService.sendEvent(emailAuditEventCaptor.capture())(any(), any()))
            .thenReturn(Future.successful(AuditResult.Success))
          when(
            mockPsrRetrievalService
              .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
          ).thenReturn(Future.successful(fullUserAnswers))
            .thenReturn(Future.successful(fullUserAnswers))
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(versionsResponse))
          MockPsrSubmissionService.submitPsrDetails()
          MockEmailConnector.sendEmail(email, templateId)
        })
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
          verify(mockPsrSubmissionService, never).submitPsrDetailsBypassed(any(), any())(any(), any(), any())
          verify(mockEmailConnector, times(1))
            .sendEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any())
          verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
          emailAuditEventCaptor.getValue.schemeAdministratorOrPractitionerName mustEqual defaultMinimalDetails.individualDetails.get.fullName
        })
        .withName(
          "when there are members in the previous returns, agree and continue should submit PSR details, send email and audit "
        )
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
