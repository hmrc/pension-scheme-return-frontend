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
import cats.data.NonEmptyList
import controllers.TestValues
import forms.{NameDOBFormProvider, TextFormProvider}
import models.{NameDOB, UploadErrors, UploadFormatError, UploadMemberDetails, UploadSuccess, ValidationError}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.domain.Nino
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class MemberDetailsUploadValidatorSpec extends BaseSpec with TestValues {

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  val validator = new MemberDetailsUploadValidator(nameDOBFormProvider, textFormProvider)

  "validateCSV" - {
    List(
      ("windows", "\r\n"),
      ("*nix", "\n")
    ).foreach {
      case (name, lineEndings) =>
        s"$name line endings: successfully validates and saves the correct user answers" in {

          val csv =
            s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number$lineEndings" +
              s"Jason,Lawrence,6/10/1989,AB123456A,,$lineEndings" +
              s"Pearl,Parsons,12/4/1990,,reason$lineEndings" +
              s"Katherine,Kennedy,30/01/1985,,reason$lineEndings"

          val source = Source.single(ByteString(csv))

          validator.validateCSV(source).futureValue mustBe UploadSuccess(
            List(
              UploadMemberDetails(1, NameDOB("Jason", "Lawrence", LocalDate.of(1989, 10, 6)), Right(Nino("AB123456A"))),
              UploadMemberDetails(2, NameDOB("Pearl", "Parsons", LocalDate.of(1990, 4, 12)), Left("reason")),
              UploadMemberDetails(3, NameDOB("Katherine", "Kennedy", LocalDate.of(1985, 1, 30)), Left("reason"))
            )
          )
        }
    }

    "successfully collect date errors" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,35/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,19901012,,reason\r\n" +
          "Katherine,Kennedy,6/10/1989,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("C1", "memberDetails.dateOfBirth.error.invalid.date"),
          ValidationError("C2", "memberDetails.dateOfBirth.error.format")
        )
      )
    }

    "successfully collect required errors when mandatory csv values is missing" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,6/10/1989,AB123456A,\r\n" +
          "Pearl,,12/04/1990,,reason\r\n" +
          ",Kennedy,30/01/1995,,reason\r\n" +
          "Jenny,Jennifer,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("B2", "memberDetails.lastName.error.required"),
          ValidationError("A3", "memberDetails.firstName.error.required")
        )
      )
    }

    "successfully collect duplicate Nino error" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,12/04/1990,AB123456A,\r\n" +
          "Katherine,Kennedy,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("D2", "memberDetailsNino.error.duplicate")
        )
      )
    }

    "successfully collect invalid Nino error" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,12/04/1990,invalidNino,\r\n" +
          "Katherine,Kennedy,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("D2", "memberDetailsNino.error.invalid")
        )
      )
    }

    "fails when both Nino and No Nino reason are present" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,12/04/1990,AB123789A,reason\r\n" +
          "Katherine,Kennedy,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadFormatError
    }

    "fails when no rows provided" in {

      val csv = "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadFormatError
    }

    "fails when empty file sent" in {

      val csv = ""

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadFormatError
    }

    "successfully collects different errors" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "123,Parsons,12/04/1990,,reason\r\n" +
          "Katherine,123,30/01/1985,,reason\r\n" +
          "Ron,Phelps,12/04/1990,,Invalid@|^reason\r\n" +
          "Bentley,,06/10/1989,AC123456C,\r\n" +
          "Micheal,Beasley,30/01/1985,InvalidNino,\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("A2", "memberDetails.firstName.error.invalid"),
          ValidationError("B3", "memberDetails.lastName.error.invalid"),
          ValidationError("E4", "noNINO.error.invalid"),
          ValidationError("B5", "memberDetails.lastName.error.required"),
          ValidationError("D6", "memberDetailsNino.error.invalid")
        )
      )
    }
  }
}
