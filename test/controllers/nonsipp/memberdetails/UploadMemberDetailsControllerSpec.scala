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

package controllers.nonsipp.memberdetails

import play.api.test.FakeRequest
import services.{AuditService, SchemeDateService, UploadService}
import play.api.mvc.Call
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.UploadView
import controllers.nonsipp.memberdetails.UploadMemberDetailsController.viewModel
import models.{DateRange, UpscanFileReference, UpscanInitiateResponse}
import play.api.data.FormError
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import play.api.inject.bind
import config.FrontendAppConfig

import scala.concurrent.Future

class UploadMemberDetailsControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.UploadMemberDetailsController.onPageLoad(srn, false)
  private def onPageLoad(errorCode: String, errorMessage: String): Call =
    onPageLoad.copy(url = onPageLoad.url + s"?errorCode=$errorCode&errorMessage=$errorMessage")

  private lazy val onSubmit = routes.UploadMemberDetailsController.onSubmit(srn)

  private val postTarget = "test-post-target"
  private val formFields = Map("test1" -> "field1", "test2" -> "field2")

  private val upscanInitiateResponse = UpscanInitiateResponse(UpscanFileReference("test-ref"), postTarget, formFields)

  private val mockUploadService = mock[UploadService]
  private val mockSchemeDateService = mock[SchemeDateService]
  private val mockAuditService = mock[AuditService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[UploadService].toInstance(mockUploadService),
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def beforeEach(): Unit = {
    reset(mockUploadService)
    reset(mockSchemeDateService)
    reset(mockAuditService)
    when(mockUploadService.registerUploadRequest(any(), any()))
      .thenReturn(Future.successful((): Unit))
  }

  "onPageLoad should use the right upscan config URLs" in {
    running(_ => applicationBuilder(Some(defaultUserAnswers))) { implicit app =>
      when(mockUploadService.initiateUpscan(any(), any(), any())(using any()))
        .thenReturn(Future.successful(upscanInitiateResponse))

      val appConfig = app.injector.instanceOf[FrontendAppConfig]
      val expectedCallbackUrl = appConfig.upscanCallbackEndpoint
      val callbackCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val successCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val failureCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val request = FakeRequest(onPageLoad)
      route(app, request).value.futureValue

      verify(mockUploadService).initiateUpscan(
        callbackCaptor.capture(),
        successCaptor.capture(),
        failureCaptor.capture()
      )(using any())

      val actualCallbackUrl = callbackCaptor.getValue
      val actualSuccessUrl = successCaptor.getValue
      val actualFailureUrl = failureCaptor.getValue
      actualCallbackUrl mustBe expectedCallbackUrl
      actualSuccessUrl must endWith("/submit-upload-member-details-file")
      actualFailureUrl must endWith("/upload-member-details-file")
    }
  }

  "UploadMemberDetailsController" - {
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[UploadView].apply(viewModel(postTarget, formFields, None, "1MB"))
    }.before(mockInitiateUpscan()))

    act.like(
      renderView(onPageLoad("EntityTooLarge", "file too large")) { implicit app => implicit request =>
        injected[UploadView].apply(
          viewModel(
            postTarget,
            formFields,
            Some(FormError("file-input", "uploadMemberDetails.error.size", Seq("1MB"))),
            "1MB"
          )
        )
      }.before {
        mockTaxYear(dateRange)
        mockInitiateUpscan()
      }.after {
        verify(mockAuditService, times(1)).sendEvent(any())(using any(), any())
        reset(mockAuditService)
      }.updateName(_ + " with error EntityTooLarge")
    )

    act.like(
      renderView(onPageLoad("InvalidArgument", "'file' field not found")) { implicit app => implicit request =>
        injected[UploadView].apply(
          viewModel(postTarget, formFields, Some(FormError("file-input", "uploadMemberDetails.error.required")), "1MB")
        )
      }.updateName(_ + " with error InvalidArgument")
        .before {
          mockTaxYear(dateRange)
          mockInitiateUpscan()
        }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }

  private def mockInitiateUpscan(): Unit =
    when(mockUploadService.initiateUpscan(any(), any(), any())(using any()))
      .thenReturn(Future.successful(upscanInitiateResponse))

  private def mockTaxYear(taxYear: DateRange) =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(using any())).thenReturn(Some(Left(taxYear)))
}
