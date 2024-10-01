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
import org.apache.pekko.stream.Materializer
import config.Refined.Max3
import controllers.ControllerBaseSpec
import play.api.inject.bind
import org.apache.pekko.stream.scaladsl.Source
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import org.mockito.stubbing.OngoingStubbing
import models.UploadStatus.UploadStatus
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import play.api.test.Helpers._
import pages.nonsipp.memberdetails.CheckMemberDetailsFilePage
import org.mockito.Mockito._
import controllers.nonsipp.memberdetails.CheckMemberDetailsFileController._
import cats.data.NonEmptyList
import views.html.YesNoPageView
import forms.YesNoPageFormProvider
import pages.nonsipp.memberdetails.upload.UploadStatusPage
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import java.time.LocalDate

class CheckMemberDetailsFileControllerSpec extends ControllerBaseSpec with GuiceOneAppPerSuite with MockitoSugar {

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

  private def mockGetUploadStatus(uploadStatus: Option[UploadStatus]): Unit =
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(uploadStatus))

  private def mockStream(): Unit =
    when(mockUploadService.stream(any())(any())).thenReturn(Future.successful((200, Source.single(byteString))))

  private def mockValidateCSV(result: (Upload, Int, Long)): Unit =
    when(mockMemberDetailsUploadValidator.validateCSV(any(), any())(any(), any()))
      .thenReturn(Future.successful(result))

  private def mockSaveValidatedUpload(): Unit =
    when(mockUploadService.saveValidatedUpload(any(), any())).thenReturn(Future.successful(()))

  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))

  private val mockMessagesApi = stubMessagesApi()
  private val mockSaveService = mock[SaveService]
  private val mockNavigator = mock[Navigator]
  private val mockIdentifyAndRequireData = mock[IdentifyAndRequireData]
  private val mockFormProvider = mock[YesNoPageFormProvider]
  private val mockView = mock[YesNoPageView]
  private val controllerComponents = stubMessagesControllerComponents()

  override implicit val mat: Materializer = app.materializer

  private val controller = new CheckMemberDetailsFileController(
    messagesApi = mockMessagesApi,
    saveService = mockSaveService,
    navigator = mockNavigator,
    identifyAndRequireData = mockIdentifyAndRequireData,
    formProvider = mockFormProvider,
    uploadService = mockUploadService,
    uploadValidator = mockMemberDetailsUploadValidator,
    schemeDateService = mockSchemeDateService,
    auditService = mockAuditService,
    controllerComponents = controllerComponents,
    view = mockView
  )

  "CheckMemberDetailsFileController" - {

    "CheckMemberDetailsFileController onSubmit" - {

      act.like(
        saveAndContinue(
          onSubmit,
          "value" -> "true"
        ).before({
            mockGetUploadStatus(Some(uploadedSuccessfully))
            mockStream()
            mockValidateCSV((UploadSuccess(List()), 0, 0L))
            mockSaveValidatedUpload()

            val dateRange = DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))
            mockTaxYear(dateRange)
          })
          .after({
            verify(mockAuditService, times(2)).sendEvent(any())(any(), any())
            reset(mockAuditService)
          })
          .withName("should redirect to the next page when the form submission is valid and upload is successful")
      )

      act.like(
        redirectToPage(
          onSubmit,
          routes.UploadMemberDetailsController.onPageLoad(srn),
          defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted),
          "value" -> "true"
        ).before({
            mockGetUploadStatus(Some(UploadStatus.Failed(ErrorDetails("errorCode", "errorMessage"))))
            val dateRange = DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))
            mockTaxYear(dateRange)
          })
          .after({
            verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
            reset(mockAuditService)
          })
          .withName("should redirect to the error page when upload status is failed during onSubmit")
      )

      act.like(
        redirectToPage(
          onSubmit,
          routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode),
          defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted),
          "value" -> "true"
        ).before({
            mockGetUploadStatus(Some(UploadStatus.InProgress))
          })
          .withName("should redirect to onPageLoad when upload status is InProgress during onSubmit")
      )

      act.like(
        redirectToPage(
          onSubmit,
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted),
          "value" -> "true"
        ).before({
            mockGetUploadStatus(None)
          })
          .withName("should redirect to JourneyRecoveryController when getUploadedFile returns None during onSubmit")
      )

      "CheckMemberDetailsFileController onPageLoad" - {

        act.like(
          renderView(
            onPageLoad,
            defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted)
          ) { implicit app => implicit request =>
            injected[YesNoPageView]
              .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, Some(fileName), NormalMode))
          }.before {
              mockTaxYear(dateRange)
              mockGetUploadStatus(Some(uploadedSuccessfully))
            }
            .after {
              verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
              reset(mockAuditService)
            }
            .withName("should render the view when upload status is Success")
        )

        act.like(
          redirectToPage(
            onPageLoad,
            routes.UploadMemberDetailsController.onPageLoad(srn),
            defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted)
          ).before {
              val dateRange = DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))
              mockTaxYear(dateRange)
              mockGetUploadStatus(Some(UploadStatus.Failed(ErrorDetails("errorCode", "errorMessage"))))
            }
            .withName("should redirect to the error page when upload status is failed during onPageLoad")
        )

        act.like(
          renderView(
            onPageLoad,
            defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted)
          ) { implicit app => implicit request =>
            injected[YesNoPageView]
              .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, None, NormalMode))
          }.before {
              mockTaxYear(dateRange)
              mockGetUploadStatus(Some(UploadStatus.InProgress))
            }
            .withName("should render the view when upload status is InProgress")
        )

        act.like(
          redirectToPage(
            onPageLoad,
            controllers.routes.JourneyRecoveryController.onPageLoad(),
            defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted)
          ).before {
              mockTaxYear(dateRange)
              mockGetUploadStatus(None)
            }
            .withName("should redirect to JourneyRecoveryController when getUploadStatus returns None")
        )

        act.like(
          redirectToPage(
            onPageLoad,
            routes.UploadMemberDetailsController.onPageLoad(srn),
            defaultUserAnswers
          ).withName("should redirect to UploadMemberDetailsController when UploadStatusPage is not UploadSubmitted")
        )

        act.like(
          renderView(onPageLoad, defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted)) {
            implicit app => implicit request =>
              injected[YesNoPageView]
                .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, Some(fileName), NormalMode))
          }.before({
              mockTaxYear(dateRange)
              mockGetUploadStatus(Some(uploadedSuccessfully))
            })
            .after({
              verify(mockAuditService, times(1)).sendEvent(any())(any(), any())
              reset(mockAuditService)
            })
        )

        act.like(
          renderPrePopView(
            onPageLoad,
            CheckMemberDetailsFilePage(srn),
            true,
            defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted)
          ) { implicit app => implicit request =>
            injected[YesNoPageView]
              .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, Some(fileName), NormalMode))
          }.before({
            mockTaxYear(dateRange)
            mockGetUploadStatus(Some(uploadedSuccessfully))
          })
        )

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
    }
  }
}
