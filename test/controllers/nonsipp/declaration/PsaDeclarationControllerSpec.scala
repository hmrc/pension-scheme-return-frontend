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

import connectors.EmailConnector
import controllers.ControllerBaseSpec
import models.DateRange
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{AuditService, PsrSubmissionService, SchemeDateService}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import views.html.ContentPageView

import scala.concurrent.Future

class PsaDeclarationControllerSpec extends ControllerBaseSpec with BeforeAndAfterEach {

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
    bind[AuditService].toInstance(mockAuditService),
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[EmailConnector].toInstance(mockEmailConnector)
  )

  "PsaDeclarationController" - {

    lazy val viewModel = PsaDeclarationController.viewModel(srn)

    lazy val onPageLoad = routes.PsaDeclarationController.onPageLoad(srn)
    lazy val onSubmit = routes.PsaDeclarationController.onSubmit(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[ContentPageView]
      view(viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      agreeAndContinue(onSubmit)
        .before({
          when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDatePeriod))
          when(mockSchemeDateService.returnPeriodsAsJsonString(any())(any())).thenReturn("")
          when(mockSchemeDateService.submissionDateAsString(any())).thenReturn("")
          when(mockAuditService.sendEvent(any)(any(), any())).thenReturn(Future.successful(AuditResult.Success))
          MockPSRSubmissionService.submitPsrDetails()
          MockEmailConnector.sendEmail(email, templateId)
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
