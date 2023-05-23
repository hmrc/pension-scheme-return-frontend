/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.nonsipp.memberdetails.upload

import controllers.nonsipp.memberdetails.upload.FileUploadSuccessController._
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import models.{
  NameDOB,
  NormalMode,
  Upload,
  UploadFormatError,
  UploadMemberDetails,
  UploadStatus,
  UploadSuccess,
  UserAnswers
}
import models.UploadStatus.UploadStatus
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyInt}
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsNinoPage, MemberDetailsPage, NoNINOPage}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.{SaveService, UploadService}
import uk.gov.hmrc.domain.Nino
import views.html.ContentPageView

import scala.concurrent.Future

class FileUploadSuccessControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FileUploadSuccessController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FileUploadSuccessController.onSubmit(srn, NormalMode)

  private val mockUploadService = mock[UploadService]
  private val mockSaveService = mock[SaveService]

  override val additionalBindings: List[GuiceableModule] = List(
    inject.bind[UploadService].toInstance(mockUploadService),
    inject.bind[SaveService].toInstance(mockSaveService)
  )

  override def beforeEach(): Unit = {
    reset(mockUploadService)
    reset(mockSaveService)
    when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
  }

  "FileUploadSuccessController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ContentPageView].apply(viewModel(srn, uploadFileName, NormalMode))
    }.before(mockGetUploadStatus(Some(uploadSuccessful))))

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadStatus(Some(UploadStatus.InProgress)))
        .updateName("onPageLoad when upload status in progress" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadStatus(Some(UploadStatus.Failed)))
        .updateName("onPageLoad when upload status failed" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadStatus(None))
        .updateName("onPageLoad when upload status doesn't exist" + _)
    )

    act.like(redirectNextPage(onSubmit).before(mockGetUploadResult(Some(uploadResultSuccess))))

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(mockGetUploadResult(Some(UploadFormatError)))
        .updateName("onSubmit when upload result has a format error" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(mockGetUploadResult(Some(uploadResultErrors)))
        .updateName("onSubmit when upload result has errors" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(mockGetUploadResult(None))
        .updateName("onSubmit when upload result doesn't exist" + _)
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "onSubmit should persist the member details in user services in alphabetical order" - {
      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      val upload: UploadSuccess = UploadSuccess(
        List(
          UploadMemberDetails(1, NameDOB("A", "A", localDate), Right(Nino("AB123456A"))),
          UploadMemberDetails(2, NameDOB("C", "C", localDate), Left("reason C")),
          UploadMemberDetails(3, NameDOB("B", "B", localDate), Left("reason B"))
        )
      )

      act.like(
        redirectNextPage(onSubmit, emptyUserAnswers)
          .before({
            when(mockSaveService.save(captor.capture())(any(), any())).thenReturn(Future.successful(()))
            mockGetUploadResult(Some(upload))
          })
          .after {
            val userAnswers = captor.getValue
            userAnswers.get(MemberDetailsPage(srn, refineMV(1))) mustBe Some(NameDOB("A", "A", localDate))
            userAnswers.get(MemberDetailsNinoPage(srn, refineMV(1))) mustBe Some(Nino("AB123456A"))
            userAnswers.get(NoNINOPage(srn, refineMV(1))) mustBe None
            userAnswers.get(DoesMemberHaveNinoPage(srn, refineMV(1))) mustBe Some(true)

            userAnswers.get(MemberDetailsPage(srn, refineMV(2))) mustBe Some(NameDOB("B", "B", localDate))
            userAnswers.get(MemberDetailsNinoPage(srn, refineMV(2))) mustBe None
            userAnswers.get(NoNINOPage(srn, refineMV(2))) mustBe Some("reason B")
            userAnswers.get(DoesMemberHaveNinoPage(srn, refineMV(2))) mustBe Some(false)

            userAnswers.get(MemberDetailsPage(srn, refineMV(3))) mustBe Some(NameDOB("C", "C", localDate))
            userAnswers.get(NoNINOPage(srn, refineMV(3))) mustBe Some("reason C")
            userAnswers.get(MemberDetailsNinoPage(srn, refineMV(3))) mustBe None
            userAnswers.get(DoesMemberHaveNinoPage(srn, refineMV(3))) mustBe Some(false)
          }
      )
    }
  }

  private def mockGetUploadStatus(uploadStatus: Option[UploadStatus]): Unit =
    when(mockUploadService.getUploadStatus(any())).thenReturn(Future.successful(uploadStatus))

  private def mockGetUploadResult(upload: Option[Upload]): Unit =
    when(mockUploadService.getUploadResult(any())).thenReturn(Future.successful(upload))
}
