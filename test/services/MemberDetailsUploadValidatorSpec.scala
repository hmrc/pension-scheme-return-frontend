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

package services

import akka.stream.scaladsl.Source
import akka.util.ByteString
import controllers.TestValues
import eu.timepit.refined.refineMV
import forms.{NameDOBFormProvider, TextFormProvider}
import models.{NameDOB, UploadErrors, UploadFormatError, UserAnswers, ValidationError}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.Assertion
import pages.nonsipp.memberdetails._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MemberDetailsUploadValidatorSpec extends BaseSpec with TestValues {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockSaveService = mock[SaveService]

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}

  val validator = new MemberDetailsUploadValidator(nameDOBFormProvider, mockSaveService, textFormProvider)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSaveService)
  }

  "validateCSV" - {
    "successfully validates and saves the correct user answers" in {

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,1989-10-06,AB123456A,\r\n" +
          "Pearl,Parsons,1990-04-12,,reason\r\n" +
          "Katherine,Kennedy,1985-01-30,,reason\r\n"
      val source = Source.single(ByteString(csv))

      when(mockSaveService.save(captor.capture())(any(), any())).thenReturn(Future.successful(()))

      validator.validateCSV(srn, emptyUserAnswers, source).futureValue mustBe (())

      val result = captor.getValue

      result.get(MemberDetailsPage(srn, refineMV(1))) mustBe Some(
        NameDOB("Jason", "Lawrence", LocalDate.of(1989, 10, 6))
      )
      result.get(DoesMemberHaveNinoPage(srn, refineMV(1))) mustBe Some(true)
      result.get(MemberDetailsNinoPage(srn, refineMV(1))) mustBe Some(Nino("AB123456A"))

      result.get(MemberDetailsPage(srn, refineMV(2))) mustBe Some(
        NameDOB("Pearl", "Parsons", LocalDate.of(1990, 4, 12))
      )
      result.get(DoesMemberHaveNinoPage(srn, refineMV(2))) mustBe Some(false)
      result.get(NoNINOPage(srn, refineMV(2))) mustBe Some("reason")

      result.get(MemberDetailsPage(srn, refineMV(3))) mustBe Some(
        NameDOB("Katherine", "Kennedy", LocalDate.of(1985, 1, 30))
      )
      result.get(DoesMemberHaveNinoPage(srn, refineMV(3))) mustBe Some(false)
      result.get(NoNINOPage(srn, refineMV(3))) mustBe Some("reason")
    }

    "successfully validates and collects errors" in {

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,1989-10-06,AB123456A,\r\n" +
          "123,Parsons,1990-04-12,,reason\r\n" +
          "Katherine,123,1985-01-30,,reason\r\n" +
          "Ron,Phelps,1972-11-04,,Invalid@|^reason\r\n" +
          "Micheal,Beasley,1990-04-15,InvalidNino,\r\n"

      val source = Source.single(ByteString(csv))

      when(mockSaveService.save(captor.capture())(any(), any())).thenReturn(Future.successful(()))

      validator.validateCSV(srn, emptyUserAnswers, source).futureValue mustBe (())

      val result = captor.getValue

      verifyNoSave(result)

      result.get(MembersDetailsFileErrors(srn)) mustBe Some(
        UploadErrors(
          List(
            ValidationError("A2", "memberDetails.firstName.error.invalid"),
            ValidationError("B3", "memberDetails.lastName.error.invalid"),
            ValidationError("E4", "noNINO.error.invalid"),
            ValidationError("D5", "memberDetailsNino.error.invalid")
          )
        )
      )
    }

    "successfully collect duplicate Nino error" in {

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,1989-10-06,AB123456A,\r\n" +
          "Pearl,Parsons,1990-04-12,AB123456A,\r\n" +
          "Katherine,Kennedy,1985-01-30,,reason\r\n"

      val source = Source.single(ByteString(csv))

      when(mockSaveService.save(captor.capture())(any(), any())).thenReturn(Future.successful(()))

      validator.validateCSV(srn, emptyUserAnswers, source).futureValue mustBe (())

      val result = captor.getValue

      verifyNoSave(result)

      result.get(MembersDetailsFileErrors(srn)) mustBe Some(
        UploadErrors(
          List(
            ValidationError("D2", "memberDetailsNino.error.duplicate")
          )
        )
      )
    }

    "fails when both Nino and No Nino reason are present" in {

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,1989-10-06,AB123456A,\r\n" +
          "Pearl,Parsons,1990-04-12,AB123789A,reason\r\n" +
          "Katherine,Kennedy,1985-01-30,,reason\r\n"

      val source = Source.single(ByteString(csv))

      when(mockSaveService.save(captor.capture())(any(), any())).thenReturn(Future.successful(()))

      validator.validateCSV(srn, emptyUserAnswers, source).futureValue mustBe (())

      val result = captor.getValue

      verifyNoSave(result)

      result.get(MembersDetailsFileErrors(srn)) mustBe Some(UploadFormatError)
    }
  }

  private def verifyNoSave(userAnswers: UserAnswers): Assertion = {
    userAnswers.get(MemberDetailsPage(srn, refineMV(1))) mustBe None
    userAnswers.get(DoesMemberHaveNinoPage(srn, refineMV(1))) mustBe None
    userAnswers.get(MemberDetailsNinoPage(srn, refineMV(1))) mustBe None
    userAnswers.get(NoNINOPage(srn, refineMV(1))) mustBe None
  }
}
