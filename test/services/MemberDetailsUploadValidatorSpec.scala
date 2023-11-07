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
import generators.WrappedMemberDetails
import models._
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.domain.Nino
import utils.BaseSpec

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global

class MemberDetailsUploadValidatorSpec extends BaseSpec with TestValues {

  private val mockSchemeDateService = mock[SchemeDateService]
  private val mockReq = mock[DataRequest[AnyContent]]
  private val mockSrn = srnGen.sample.value
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

  val validHeaders =
    "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number"

  def validRow =
    detailsToRow(wrappedMemberDetailsGen.sample.get)

  def detailsToRow(details: WrappedMemberDetails): String =
    s"${details.nameDob.firstName},${details.nameDob.lastName},${formatDate(details.nameDob.dob)},${formatNino(details.nino)}"

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

          val actual = validator.validateCSV(source, mockSrn, mockReq).futureValue
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
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,35/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,19901012,,reason\r\n" +
          "Katherine,Kennedy,6/10/1989,,reason\r\n"

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, mockSrn, mockReq).futureValue
      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("C1", ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.error.invalid.date"),
          ValidationError("C2", ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.error.format")
        )
      )
      actual._2 mustBe 3
    }

    "successfully collect required errors when mandatory csv values is missing" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,6/10/1989,AB123456A,\r\n" +
          "Pearl,,12/04/1990,,reason\r\n" +
          ",Kennedy,30/01/1995,,reason\r\n" +
          "Jenny,Jennifer,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadErrors(
        NonEmptyList.of(
          ValidationError("B2", ValidationErrorType.LastName, "memberDetails.lastName.error.required"),
          ValidationError("A3", ValidationErrorType.FirstName, "memberDetails.firstName.error.required")
        )
      ), 4)
    }

    "successfully collect duplicate Nino error" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,12/04/1990,AB123456A,\r\n" +
          "Katherine,Kennedy,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadErrors(
        NonEmptyList.of(
          ValidationError("D2", ValidationErrorType.DuplicateNino, "memberDetailsNino.error.duplicate")
        )
      ), 3)
    }

    "successfully collect invalid Nino error" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,12/04/1990,invalidNino,\r\n" +
          "Katherine,Kennedy,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadErrors(
        NonEmptyList.of(
          ValidationError("D2", ValidationErrorType.NinoFormat, "memberDetailsNino.error.invalid")
        )
      ), 3)
    }

    "fails when both Nino and No Nino reason are present" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,12/04/1990,AB123789A,reason\r\n" +
          "Katherine,Kennedy,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadFormatError, 2)
    }

    "fails when no rows provided" in {

      val csv = validHeaders

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadFormatError, 0)
    }

    "fails when empty file sent" in {

      val csv = ""

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadFormatError, 0)
    }

    "fail when there are more than 300 entries" in {

      val csv =
        (validHeaders :: List.fill(301)(validRow)).mkString("\n")

      print(csv)

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadMaxRowsError, 301)
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

      validator.validateCSV(source, mockSrn, mockReq).futureValue mustBe (UploadErrors(
        NonEmptyList.of(
          ValidationError("A2", ValidationErrorType.FirstName, "memberDetails.firstName.error.invalid"),
          ValidationError("B3", ValidationErrorType.LastName, "memberDetails.lastName.error.invalid"),
          ValidationError("E4", ValidationErrorType.NoNinoReason, "noNINO.upload.error.invalid"),
          ValidationError("B5", ValidationErrorType.LastName, "memberDetails.lastName.error.required"),
          ValidationError("D6", ValidationErrorType.NinoFormat, "memberDetailsNino.error.invalid")
        )
      ), 6)
    }
  }
}
