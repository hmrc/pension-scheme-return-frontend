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

import services.{AuditService, PsrSubmissionService, SchemeDateService}
import play.api.inject.guice.GuiceableModule
import controllers.nonsipp.declaration.PspDeclarationController._
import connectors.EmailConnector
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.PsaIdInputView
import forms.TextFormProvider
import models.DateRange
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import pages.nonsipp.declaration.PspDeclarationPage
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.Future

class PspDeclarationControllerSpec extends ControllerBaseSpec with BeforeAndAfterEach {
  private val populatedUserAnswers = {
    defaultUserAnswers.unsafeSet(PspDeclarationPage(srn), psaId.value)
  }
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockEmailConnector = mock[EmailConnector]
  private val mockAuditService = mock[AuditService]
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val schemeDatePeriod: DateRange = dateRangeGen.sample.value
  private val templateId = "pods_event_report_submitted" // TODO change as per PSR-1139

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    reset(mockAuditService)
    reset(mockEmailConnector)
    reset(mockSchemeDateService)
    super.beforeEach()
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService),
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[AuditService].toInstance(mockAuditService),
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "PspDeclarationController" - {

    lazy val viewModel = PspDeclarationController.viewModel(srn)

    lazy val onPageLoad = routes.PspDeclarationController.onPageLoad(srn)
    lazy val onSubmit = routes.PspDeclarationController.onSubmit(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[PsaIdInputView]
        .apply(form(injected[TextFormProvider], Some(psaId.value)), viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      agreeAndContinue(onSubmit, populatedUserAnswers, "value" -> psaId.value)
        .before({
          MockPSRSubmissionService.submitPsrDetails()
          MockEmailConnector.sendEmail(email, templateId)
          when(mockSchemeDateService.returnPeriodsAsJsonString(any())(any())).thenReturn("")
          when(mockSchemeDateService.submissionDateAsString(any())).thenReturn("")
          when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDatePeriod))
          when(mockAuditService.sendEvent(any)(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        })
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
          verify(mockEmailConnector, times(1))
            .sendEmail(any(), any(), any(), any(), any(), any(), any(), any())(any(), any())
          verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
        })
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
