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

package services

import controllers.TestValues
import cats.data.NonEmptyList
import generators.WrappedMemberDetails
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.hmrc.domain.Nino
import models._
import play.api.i18n.Messages
import org.apache.pekko.util.ByteString
import play.api.test.FakeRequest
import utils.BaseSpec
import play.api.test.Helpers.stubMessagesApi
import forms.{NameDOBFormProvider, TextFormProvider}
import models.ValidationErrorType._

import scala.concurrent.ExecutionContext.Implicits.global

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MemberDetailsUploadValidatorSpec extends BaseSpec with TestValues {

  private val mockSchemeDateService = mock[SchemeDateService]
  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  val validator =
    new MemberDetailsUploadValidator(nameDOBFormProvider, textFormProvider, mockSchemeDateService)

  def formatDate(d: LocalDate): String = {
    val df = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    df.format(d)
  }

  def formatNino(e: Either[String, Nino]): String =
    e match {
      case Right(v) => s"$v,"
      case Left(v) => s",$v"
    }

  val validHeaders: String =
    "What you need to do  Enter your data from row 3 onwards. Complete the questions per member marked horizontally across the columns.," +
      "First name of scheme member (mandatory),Last name of scheme member (mandatory)," +
      "Member National Insurance number,\"If no National Insurance number for member, give reason\",Member date of birth (mandatory)"

  def validRow: String =
    detailsToRow(wrappedMemberDetailsGen.sample.get)

  def detailsToRow(details: WrappedMemberDetails): String =
    s"${details.nameDob.firstName},${details.nameDob.lastName},${formatDate(details.nameDob.dob)},${formatNino(details.nino)}"

  "validateCSV" - {
    List(
      ("windows", "\r\n"),
      ("*nix", "\n")
    ).foreach { case (name, lineEndings) =>
      s"$name line endings: successfully validates and saves the correct user answers" in {

        val csv =
          s"$validHeaders$lineEndings" +
            s"Help,Sample-Name,Sample-Surname,Sample-Nino,Sample-Reason,Sample-Date$lineEndings" +
            s",Jason,Lawrence,AB123456A,,6/10/1989$lineEndings" +
            s",Pearl,Parsons,,reason,12/4/1990$lineEndings" +
            s",Katherine,Kennedy,,reason,30/01/1985$lineEndings"

        val source = Source.single(ByteString(csv))

        val actual = validator.validateCSV(source, None).futureValue
        actual._1 mustBe UploadSuccess(
          List(
            UploadMemberDetails(1, NameDOB("Jason", "Lawrence", LocalDate.of(1989, 10, 6)), Right(Nino("AB123456A"))),
            UploadMemberDetails(2, NameDOB("Pearl", "Parsons", LocalDate.of(1990, 4, 12)), Left("reason")),
            UploadMemberDetails(3, NameDOB("Katherine", "Kennedy", LocalDate.of(1985, 1, 30)), Left("reason"))
          )
        )
        actual._2 mustBe 3
      }
    }

    "successfully collect date errors" in {

      val csv =
        s"$validHeaders\r\n" +
          s"Help,Sample-Name,Sample-Surname,Sample-Nino,Sample-Reason,Sample-Date\r\n" +
          ",Jason,Lawrence,AB123456A,,35/10/1989\r\n" +
          ",Pearl,Parsons,,reason,19901012\r\n" +
          ",Katherine,Kennedy,,reason,6/10/1989\r\n"

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, Option(localDate)).futureValue
      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("F3", ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.upload.error.invalid.date"),
          ValidationError("F4", ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.upload.error.format")
        )
      )
      actual._2 mustBe 3
    }

    "successfully collect required errors when mandatory csv values is missing" in {

      val csv =
        s"$validHeaders\r\n" +
          s"Help,Sample-Name,Sample-Surname,Sample-Nino,Sample-Reason,Sample-Date\r\n" +
          ",Jason,Lawrence,AB123456A,,6/10/1989\r\n" +
          ",Pearl,,,reason,12/04/1990\r\n" +
          ",,Kennedy,,reason,30/01/1995\r\n" +
          ",Jenny,Jennifer,,reason,30/01/1985\r\n"

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("C4", ValidationErrorType.LastName, "memberDetails.lastName.upload.error.required"),
          ValidationError("B5", ValidationErrorType.FirstName, "memberDetails.firstName.upload.error.required")
        )
      )
      actual._2 mustBe 4
    }

    "successfully collect duplicate Nino error" in {

      val csv =
        s"$validHeaders\r\n" +
          s"Help,Sample-Name,Sample-Surname,Sample-Nino,Sample-Reason,Sample-Date\r\n" +
          ",Jason,Lawrence,AB123456A,,06/10/1989\r\n" +
          ",Pearl,Parsons,AB123456A,,12/04/1990\r\n" +
          ",Katherine,Kennedy,,reason,30/01/1985\r\n"

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, Option(localDate)).futureValue
      actual._1 mustBe UploadErrors(
        NonEmptyList(
          ValidationError("F4", DateOfBirth, "memberDetails.dateOfBirth.upload.error.future"),
          List(ValidationError("D4", NinoFormat, "memberDetailsNino.upload.error.duplicate"))
        )
      )
      actual._2 mustBe 3
    }

    "successfully collect invalid Nino error" in {

      val csv =
        s"$validHeaders\r\n" +
          s"Help,Sample-Name,Sample-Surname,Sample-Nino,Sample-Reason,Sample-Date\r\n" +
          ",Jason,Lawrence,AB123456A,,06/10/1989\r\n" +
          ",Pearl,Parsons,invalidNino,,12/04/1990\r\n" +
          ",Katherine,Kennedy,,reason,30/01/1985\r\n"

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("D4", ValidationErrorType.NinoFormat, "memberDetailsNino.upload.error.invalid")
        )
      )
      actual._2 mustBe 3
    }

    "fails when both Nino and No Nino reason are present" in {

      val csv =
        s"$validHeaders\r\n" +
          s"Help,Sample-Name,Sample-Surname,Sample-Nino,Sample-Reason,Sample-Date\r\n" +
          ",Jason,Lawrence,AB123456A,,06/10/1989\r\n" +
          ",Pearl,Parsons,AB123789A,reason,12/04/1990\r\n" +
          ",Katherine,Kennedy,,reason,30/01/1985\r\n"

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadFormatError
      actual._2 mustBe 2
    }

    "fails when no rows provided" in {

      val csv = validHeaders

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadFormatError
      actual._2 mustBe 0
    }

    "fails when empty file sent" in {

      val csv = ""

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadFormatError
      actual._2 mustBe 0
    }

    "fail when there are more than 300 entries" in {

      val csv =
        (validHeaders :: List.fill(302)(validRow)).mkString("\n")

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadMaxRowsError
      actual._2 mustBe 301
    }

    "successfully collects different errors" in {

      val csv =
        s"$validHeaders\r\n" +
          s"Help,Sample-Name,Sample-Surname,Sample-Nino,Sample-Reason,Sample-Date\r\n" +
          ",Jason,Lawrence,AB123456A,,06/10/1989\r\n" +
          ",123,Parsons,,reason,12/04/1990\r\n" +
          ",Katherine,123,,reason,30/01/1985\r\n" +
          ",Ron,Phelps,,Invalid@|^reason,12/04/1990\r\n" +
          ",Bentley,,AC123456C,,06/10/1989\r\n" +
          ",Micheal,Beasley,InvalidNino,,30/01/1985\r\n"

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, Option(localDate)).futureValue
      actual._1 mustBe UploadErrors(
        NonEmptyList(
          ValidationError("B4", FirstName, "memberDetails.firstName.upload.error.invalid"),
          List(
            ValidationError("F4", DateOfBirth, "memberDetails.dateOfBirth.upload.error.future"),
            ValidationError("C5", LastName, "memberDetails.lastName.upload.error.invalid"),
            ValidationError("F6", DateOfBirth, "memberDetails.dateOfBirth.upload.error.future"),
            ValidationError("E6", NoNinoReason, "noNINO.upload.upload.error.invalid"),
            ValidationError("C7", LastName, "memberDetails.lastName.upload.error.required"),
            ValidationError("D8", NinoFormat, "memberDetailsNino.upload.error.invalid")
          )
        )
      )
      actual._2 mustBe 6
    }
  }
}
