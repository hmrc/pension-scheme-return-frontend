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

package controllers

import cats.data.NonEmptyList
import controllers.actions._
import generators.ModelGenerators._
import models.PensionSchemeId.PsaId
import models.UserAnswers.SensitiveJsObject
import models.requests.IdentifierRequest
import models.{NameDOB, _}
import org.scalatest.OptionValues
import play.api.Application
import play.api.data.Form
import play.api.http._
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Call, PlayBodyParsers}
import play.api.test._
import queries.Settable
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseSpec, DisplayMessageUtils}

import java.time.{LocalDate, LocalDateTime}

trait ControllerBaseSpec
    extends BaseSpec
    with ControllerBehaviours
    with MockBehaviours
    with DefaultAwaitTimeout
    with HttpVerbs
    with Writeables
    with HeaderNames
    with Status
    with PlayRunners
    with RouteInvokers
    with ResultExtractors
    with TestValues
    with DisplayMessageUtils {

  val baseUrl = "/pension-scheme-return"

  val testOnwardRoute: Call = Call("GET", "/foo")

  val defaultTaxYear = TaxYear(2022)

  protected def applicationBuilder(
    userAnswers: Option[UserAnswers] = None,
    schemeDetails: SchemeDetails = defaultSchemeDetails,
    minimalDetails: MinimalDetails = defaultMinimalDetails
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        List[GuiceableModule](
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[AllowAccessActionProvider].toInstance(new FakeAllowAccessActionProvider(schemeDetails, minimalDetails)),
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
          bind[DataCreationAction].toInstance(new FakeDataCreationAction(userAnswers.getOrElse(emptyUserAnswers)))
        ) ++ additionalBindings: _*
      )
      .configure("play.filters.csp.nonce.enabled" -> false)

  protected val additionalBindings: List[GuiceableModule] = List()

  def runningApplication[T](block: Application => T): T =
    running(_ => applicationBuilder())(block)

  def formData[A](form: Form[A], data: A): List[(String, String)] = form.fill(data).data.toList

  implicit class UserAnswersOps(ua: UserAnswers) {
    def unsafeSet[A: Writes](page: Settable[A], value: A): UserAnswers = ua.set(page, value).success.value
  }
}

trait TestValues { _: OptionValues =>
  val accountNumber = "12345678"
  val sortCode = "123456"
  val srn: SchemeId.Srn = srnGen.sample.value
  val schemeName = "testSchemeName"
  val email = "testEmail"
  val uploadKey: UploadKey = UploadKey("test-userid", srn)
  val reference: Reference = Reference("test-ref")
  val uploadFileName = "test-file-name"
  val psaId: PsaId = PsaId("testPSAId")
  val individualName = "testIndividualName"
  val nino: Nino = ninoGen.sample.get
  val utr: Utr = utrGen.sample.get
  val money: Money = Money(123456)
  val double: Double = 7.7
  val percentage: Percentage = Percentage(7.7)
  val companyName = "testCompanyName"
  val partnershipName = "testPartnershipName"
  val otherName = "testOtherName"
  val crn: Crn = crnGen.sample.get

  val individualDetails: IndividualDetails = IndividualDetails("testFirstName", Some("testMiddleName"), "testLastName")

  val userAnswersId: String = "id"
  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val defaultUserAnswers: UserAnswers =
    UserAnswers(userAnswersId, SensitiveJsObject(Json.obj("non" -> "empty")))

  val dateRange: DateRange = DateRange(
    from = LocalDate.of(2020, 4, 6),
    to = LocalDate.of(2021, 4, 5)
  )

  val localDateTime: LocalDateTime =
    LocalDateTime.of(2020, 12, 12, 10, 30, 15)

  val localDate: LocalDate = LocalDate.of(1989, 10, 6)

  val defaultSchemeDetails: SchemeDetails = SchemeDetails(
    "testSRN",
    schemeName,
    "testPSTR",
    SchemeStatus.Open,
    "testSchemeType",
    Some("testAuthorisingPSAID"),
    List(Establisher("testFirstName testLastName", EstablisherKind.Individual))
  )

  val defaultMinimalDetails: MinimalDetails = MinimalDetails(
    email,
    isPsaSuspended = false,
    Some("testOrganisation"),
    Some(individualDetails),
    rlsFlag = false,
    deceasedFlag = false
  )

  val memberDetails: NameDOB = NameDOB(
    "testFirstName",
    "testLastName",
    LocalDate.of(1990, 12, 12)
  )

  val schemeMemberNumbers: SchemeMemberNumbers = SchemeMemberNumbers(1, 2, 3)

  val uploadSuccessful: UploadStatus.Success = UploadStatus.Success(uploadFileName, "text/csv", "test-url", None)
  val uploadFailure: UploadStatus.Failed.type = UploadStatus.Failed

  val uploadResultSuccess: UploadSuccess = UploadSuccess(
    List(
      UploadMemberDetails(1, NameDOB("Jason", "Lawrence", LocalDate.of(1989, 10, 6)), Right(Nino("AB123456A"))),
      UploadMemberDetails(2, NameDOB("Pearl", "Parsons", LocalDate.of(1990, 4, 12)), Left("reason")),
      UploadMemberDetails(3, NameDOB("Katherine", "Kennedy", LocalDate.of(1985, 1, 30)), Left("reason"))
    )
  )

  val uploadResultErrors: UploadErrors = UploadErrors(
    NonEmptyList.of(
      ValidationError("A1", ValidationErrorType.FirstName, "error A1"),
      ValidationError("C3", ValidationErrorType.LastName, "error C3"),
      ValidationError("F2", ValidationErrorType.DateOfBirth, "error F2")
    )
  )

  val over10UploadResultErrors: UploadErrors = UploadErrors(
    NonEmptyList.of(
      ValidationError("A1", ValidationErrorType.FirstName, "error A1"),
      ValidationError("C3", ValidationErrorType.LastName, "error C3"),
      ValidationError("F2", ValidationErrorType.DateOfBirth, "error F2"),
      ValidationError("G4", ValidationErrorType.DateOfBirth, "error G4"),
      ValidationError("V6", ValidationErrorType.DateOfBirth, "error V6"),
      ValidationError("A7", ValidationErrorType.DateOfBirth, "error A7"),
      ValidationError("L3", ValidationErrorType.DateOfBirth, "error L3"),
      ValidationError("C9", ValidationErrorType.DateOfBirth, "error C9"),
      ValidationError("M10", ValidationErrorType.DateOfBirth, "error M10"),
      ValidationError("E2", ValidationErrorType.DateOfBirth, "error E2"),
      ValidationError("S11", ValidationErrorType.DateOfBirth, "error S11")
    )
  )
}
