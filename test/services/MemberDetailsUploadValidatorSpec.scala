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
import forms.{NameDOBFormProvider, TextFormProvider}
import models.{NameDOB, UploadErrors, UploadFormatError, UploadMemberDetails, UploadSuccess, ValidationError}
import uk.gov.hmrc.domain.Nino
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class MemberDetailsUploadValidatorSpec extends BaseSpec with TestValues {

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}

  val validator = new MemberDetailsUploadValidator(nameDOBFormProvider, textFormProvider)

  "validateCSV" - {
    "successfully validates and saves the correct user answers" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,6/10/1989,AB123456A,\r\n" +
          "Pearl,Parsons,12/4/1990,,reason\r\n" +
          "Katherine,Kennedy,30/01/1985,,reason\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadSuccess(
        List(
          UploadMemberDetails(1, NameDOB("Jason", "Lawrence", LocalDate.of(1989, 10, 6)), Right(Nino("AB123456A"))),
          UploadMemberDetails(2, NameDOB("Pearl", "Parsons", LocalDate.of(1990, 4, 12)), Left("reason")),
          UploadMemberDetails(3, NameDOB("Katherine", "Kennedy", LocalDate.of(1985, 1, 30)), Left("reason"))
        )
      )
    }

    "successfully validates and collects errors" in {

      val csv =
        "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number\r\n" +
          "Jason,Lawrence,06/10/1989,AB123456A,\r\n" +
          "123,Parsons,12/04/1990,,reason\r\n" +
          "Katherine,123,30/1/1985,,reason\r\n" +
          "Ron,Phelps,4/11/1972,,Invalid@|^reason\r\n" +
          "Micheal,Beasley,15/04/1990,InvalidNino,\r\n"

      val source = Source.single(ByteString(csv))

      validator.validateCSV(source).futureValue mustBe UploadErrors(
        List(
          ValidationError("A2", "memberDetails.firstName.error.invalid"),
          ValidationError("B3", "memberDetails.lastName.error.invalid"),
          ValidationError("E4", "noNINO.error.invalid"),
          ValidationError("D5", "memberDetailsNino.error.invalid")
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
        List(
          ValidationError("D2", "memberDetailsNino.error.duplicate")
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
  }
}
