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

package controllers.nonsipp.memberdetails.upload

import play.api.test.FakeRequest
import config.Refined.OneTo300
import controllers.ControllerBaseSpec
import play.api.inject.bind
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountPage
import models._
import controllers.nonsipp.memberdetails.upload.FileUploadSuccessController._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.employercontributions.{EmployerContributionsProgress, EmployerNamePage}
import services.{PsrSubmissionService, SaveService, UploadService}
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails._
import org.mockito.Mockito.{reset, verify, when}
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import pages.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountPage
import views.html.ContentPageView
import models.SchemeId.Srn
import cats.implicits.catsSyntaxOptionId
import pages.nonsipp.receivetransfer.{TransferringSchemeNamePage, TransfersInSectionCompleted}
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import eu.timepit.refined.{refineMV, refineV}
import models.UploadStatus.UploadStatus
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePage, TransfersOutSectionCompleted}
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}

import scala.concurrent.Future
import scala.util.Try

class FileUploadSuccessControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FileUploadSuccessController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FileUploadSuccessController.onSubmit(srn, NormalMode)

  private val mockUploadService = mock[UploadService]
  private val mockSaveService = mock[SaveService]
  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[UploadService].toInstance(mockUploadService),
    bind[SaveService].toInstance(mockSaveService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  def addMemberDetails(userAnswers: UserAnswers, srn: Srn, total: Int): UserAnswers = {
    def addMemberDetails(userAnswers: UserAnswers, index: Int): Try[UserAnswers] = {
      val memberDetails = wrappedMemberDetailsGen.sample.value
      val refinedIndex = refineV[OneTo300](index).toOption.value

      UserAnswers.compose(
        UserAnswers.set(MemberDetailsPage(srn, refinedIndex), memberDetails.nameDob),
        UserAnswers.set(DoesMemberHaveNinoPage(srn, refinedIndex), memberDetails.nino.isRight),
        memberDetails.nino.fold(
          UserAnswers.set(NoNINOPage(srn, refinedIndex), _),
          UserAnswers.set(MemberDetailsNinoPage(srn, refinedIndex), _)
        )
      )(userAnswers)
    }

    (1 to total).foldLeft(userAnswers)((acc, curr) => addMemberDetails(acc, curr).get)
  }

  override def beforeEach(): Unit = {
    reset(mockUploadService)
    reset(mockSaveService)
    when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
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
        .before(mockGetUploadStatus(Some(UploadStatus.Failed(ErrorDetails("reason", "message")))))
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

    "onSubmit should replace all records" in {

      val userAnswers = addMemberDetails(emptyUserAnswers, srn, 20)
      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      val upload: UploadSuccess = UploadSuccess(
        List(
          UploadMemberDetails(1, NameDOB("A", "A", localDate), Right(Nino("AB123456A")))
        )
      )

      mockGetUploadResult(upload.some)

      running(_ => applicationBuilder(userAnswers = Some(userAnswers))) { implicit app =>
        route(app, FakeRequest(onSubmit)).value.futureValue

        verify(mockSaveService).save(captor.capture())(any(), any())

        val userAnswers = captor.getValue

        userAnswers.get(MemberDetailsPage(srn, refineMV(1))) mustBe Some(NameDOB("A", "A", localDate))
        userAnswers.get(DoesMemberHaveNinoPage(srn, refineMV(1))) mustBe Some(true)
        userAnswers.get(MemberDetailsNinoPage(srn, refineMV(1))) mustBe Some(Nino("AB123456A"))

        userAnswers.get(MemberDetailsPage(srn, refineMV(2))) mustBe None
        userAnswers.get(DoesMemberHaveNinoPage(srn, refineMV(2))) mustBe None
        userAnswers.get(NoNINOPage(srn, refineMV(2))) mustBe None
        userAnswers.get(MemberDetailsNinoPage(srn, refineMV(2))) mustBe None
      }
    }

    "onSubmit should remove all member payments" in {

      val userAnswers = addMemberDetails(emptyUserAnswers, srn, 5)
        .unsafeSet(EmployerNamePage(srn, refineMV(1), refineMV(1)), employerName)
        .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
        .unsafeSet(TotalMemberContributionPage(srn, refineMV(1)), money)
        .unsafeSet(TransferringSchemeNamePage(srn, refineMV(1), refineMV(1)), schemeName)
        .unsafeSet(ReceivingSchemeNamePage(srn, refineMV(1), refineMV(1)), schemeName)
        .unsafeSet(PensionCommencementLumpSumAmountPage(srn, refineMV(1)), pensionCommencementLumpSumGen.sample.value)
        .unsafeSet(TotalAmountPensionPaymentsPage(srn, refineMV(1)), money)
        .unsafeSet(SurrenderedBenefitsAmountPage(srn, refineMV(1)), money)
        .unsafeSet(EmployerContributionsProgress(srn, refineMV(1), refineMV(1)), SectionJourneyStatus.Completed)
        .unsafeSet(TransfersInSectionCompleted(srn, refineMV(1), refineMV(1)), SectionCompleted)
        .unsafeSet(TransfersOutSectionCompleted(srn, refineMV(1), refineMV(1)), SectionCompleted)

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      val upload: UploadSuccess = UploadSuccess(
        List(
          UploadMemberDetails(1, NameDOB("A", "A", localDate), Right(Nino("AB123456A")))
        )
      )

      mockGetUploadResult(upload.some)

      running(_ => applicationBuilder(userAnswers = Some(userAnswers))) { implicit app =>
        route(app, FakeRequest(onSubmit)).value.futureValue

        verify(mockSaveService).save(captor.capture())(any(), any())

        val userAnswers = captor.getValue

        userAnswers.get(EmployerNamePage(srn, refineMV(1), refineMV(1))) mustBe None
        userAnswers.get(UnallocatedEmployerAmountPage(srn)) mustBe None
        userAnswers.get(TotalMemberContributionPage(srn, refineMV(1))) mustBe None
        userAnswers.get(TransferringSchemeNamePage(srn, refineMV(1), refineMV(1))) mustBe None
        userAnswers.get(ReceivingSchemeNamePage(srn, refineMV(1), refineMV(1))) mustBe None
        userAnswers.get(PensionCommencementLumpSumAmountPage(srn, refineMV(1))) mustBe None
        userAnswers.get(TotalAmountPensionPaymentsPage(srn, refineMV(1))) mustBe None
        userAnswers.get(SurrenderedBenefitsAmountPage(srn, refineMV(1))) mustBe None
      }
    }

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
