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

import org.apache.pekko.util.ByteString
import services._
import config.Refined.Max3
import controllers.ControllerBaseSpec
import play.api.inject.bind
import org.apache.pekko.stream.scaladsl.Source
import forms.YesNoPageFormProvider
import org.mockito.stubbing.OngoingStubbing
import models._
import models.UploadStatus.UploadStatus
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.CheckMemberDetailsFilePage
import org.mockito.Mockito._
import controllers.nonsipp.memberdetails.CheckMemberDetailsFileController._
import cats.data.NonEmptyList
import views.html.YesNoPageView

import scala.concurrent.Future

class CheckMemberDetailsFileControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.CheckMemberDetailsFileController.onSubmit(srn, NormalMode)

  private val fileName = "test-file-name"
  private val byteString = ByteString("test-content")

  private val uploadedSuccessfully = UploadStatus.Success(
    fileName,
    "text/csv",
    "/test-download-url",
    Some(123L)
  )

  private val mockUploadService = mock[UploadService]
  private val mockMemberDetailsUploadValidator = mock[MemberDetailsUploadValidator]
  private val mockSchemeDateService = mock[SchemeDateService]
  private val mockAuditService = mock[AuditService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[UploadService].toInstance(mockUploadService),
    bind[MemberDetailsUploadValidator].toInstance(mockMemberDetailsUploadValidator),
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def beforeEach(): Unit = {
    reset(mockUploadService)
    reset(mockMemberDetailsUploadValidator)
    reset(mockSchemeDateService)
    reset(mockAuditService)
    mockStream()
    mockSaveValidatedUpload()
    mockValidateCSV((UploadFormatError, 0, 0L))
  }

  "CheckMemberDetailsFileController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, Some(fileName), NormalMode))
      }.before({
          mockTaxYear(dateRange)
          mockGetUploadStatus(Some(uploadedSuccessfully))
        })
        .after({
          verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
          reset(mockAuditService)
        })
    )

    act.like(renderPrePopView(onPageLoad, CheckMemberDetailsFilePage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, Some(fileName), NormalMode))
    }.before({
      mockTaxYear(dateRange)
      mockGetUploadStatus(Some(uploadedSuccessfully))
    }))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(onSubmit, "value" -> "true")
        .before({
          mockTaxYear(dateRange)
          mockGetUploadStatus(Some(uploadedSuccessfully))
        })
        .after({
          verify(mockAuditService, times(2)).sendEvent(any())(any(), any())
          reset(mockAuditService)
        })
    )

    act.like(invalidForm(onSubmit, "invalid" -> "form").before(mockGetUploadStatus(Some(uploadedSuccessfully))))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockGetUploadStatus(uploadStatus: Option[UploadStatus]): Unit =
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(uploadStatus))

  private def mockStream(): Unit =
    when(mockUploadService.stream(any())(any())).thenReturn(Future.successful((200, Source.single(byteString))))

  private def mockValidateCSV(result: (Upload, Int, Long)): Unit =
    when(mockMemberDetailsUploadValidator.validateCSV(any(), any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(result))

  private def mockSaveValidatedUpload(): Unit =
    when(mockUploadService.saveValidatedUpload(any(), any())).thenReturn(Future.successful(()))

  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))

}
