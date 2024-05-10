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

package controllers

import play.api.test._
import queries.{Removable, Settable}
import config.Refined.Max5000
import play.api.inject.bind
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import controllers.actions._
import models.PensionSchemeId.PsaId
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators._
import models._
import uk.gov.hmrc.time.TaxYear
import pages.nonsipp.moneyborrowed.LenderNamePage
import viewmodels.models._
import play.api.data.Form
import utils.{BaseSpec, DisplayMessageUtils}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.Call
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
import models.UserAnswers.SensitiveJsObject
import play.api.http._
import cats.data.NonEmptyList
import models.SchemeId.Srn
import org.scalatest.OptionValues
import play.api.Application
import uk.gov.hmrc.domain.Nino
import play.api.libs.json.{Json, Reads, Writes}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

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
    with DiffShouldMatcher
    with DisplayMessageUtils {

  val baseUrl = "/pension-scheme-return"

  val testOnwardRoute: Call = Call("GET", "/foo")

  val defaultTaxYear: TaxYear = TaxYear(2022)

  protected def applicationBuilder(
    userAnswers: Option[UserAnswers] = Some(emptyUserAnswers),
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
    def unsafeSet[A: Writes](page: Settable[A], value: A): UserAnswers = ua.set(page, value).get

    def unsafeRemove[A: Reads](page: Removable[A]): UserAnswers = ua.remove(page).get
  }
}

trait TestValues {
  _: OptionValues =>
  val accountNumber = "12345678"
  val sortCode = "123456"
  val srn: SchemeId.Srn = srnGen.sample.value
  val schemeName = "testSchemeName"
  val email = "testEmail"
  val uploadKey: UploadKey = UploadKey("test-userid", srn)
  val reference: Reference = Reference("test-ref")
  val uploadFileName = "test-file-name"
  val psaId: PsaId = PsaId("A1234567")
  val pspId: PsaId = PsaId("testPSPId")
  val individualName = "testIndividualName"
  val nino: Nino = ninoGen.sample.get
  val noninoReason: String = "reason"
  val utr: Utr = utrGen.sample.get
  val noUtrReason: String = "no utr reason"
  val leaseName = "testLeaseName"
  val money: Money = Money(123456)
  val moneyNegative: Money = Money(1123456)
  val moneyInPeriod: MoneyInPeriod = MoneyInPeriod(money, Money(1))
  val security: Security = Security("securityGivenForLoan")
  val double: Double = 7.7
  val percentage: Percentage = Percentage(7.7)
  val loanPeriod = 5
  val companyName = "testCompanyName"
  val partnershipName = "testPartnershipName"
  val otherName = "testOtherName"
  val crn: Crn = crnGen.sample.get
  val noCrnReason: String = "no crn reason"
  val recipientName = "testRecipientName"
  val employerName = "testEmployerName"
  val individualRecipientName: String = "individual " + recipientName
  val companyRecipientName: String = "company " + recipientName
  val partnershipRecipientName: String = "partnership " + recipientName
  val otherRecipientName: String = "other " + recipientName
  val otherRecipientDescription = "other description"
  val otherRecipientDetails: RecipientDetails = RecipientDetails(otherRecipientName, otherRecipientDescription)
  val pstr = "testPstr"
  val qropsReferenceNumber = "Q123456"
  val version = "001"
  val titleNumber = "AB123456"
  val buyerName = "testBuyerName"
  val lenderName = "testLenderName"
  val amountBorrowed: (Money, Percentage) = (money, percentage)
  val reasonBorrowed = "test reason borrowed"
  val transferringSchemeName = "transferring scheme"
  val receivingSchemeName = "receiving scheme"
  val pcls: PensionCommencementLumpSum = PensionCommencementLumpSum(money, money)
  val surrenderedBenefitsAmount: Money = money
  val classOfShares = "testSharesClass"
  val totalShares = 5
  val reasonSurrenderedBenefits = "test reason surrendered benefits"
  val fbNumber = "123456785011"
  val startDate = "2023-04-06"
  val fromYearUi = "6 April 2020"
  val toYearUi = "5 April 2021"
  val otherDetails = "other details"
  val bondsStillHeld = 5
  val nameOfBonds = "name of bonds"
  val nameOfAsset = "name of asset"
  val otherAssetDescription = "other asset description"
  val fallbackUrl = "fallbackUrl"
  val fallbackCall: Call = Call("GET", fallbackUrl)

  val address: Address = Address(
    "test-id",
    "testAddressLine1",
    Some("testAddressLine2"),
    Some("testAddressLine3"),
    "testTown",
    Some("testPostCode"),
    "United Kingdom",
    "GB",
    ManualAddress
  )

  val internationalAddress: Address = Address(
    "test-id",
    "testAddressLine1",
    None,
    None,
    "testTown",
    None,
    "Japan",
    "JP",
    ManualAddress
  )

  val postcodeLookup: PostcodeLookup = PostcodeLookup("ZZ1 1ZZ", None)

  val individualDetails: IndividualDetails = IndividualDetails("testFirstName", Some("testMiddleName"), "testLastName")

  val userAnswersId: String = "id"

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val defaultExpectedDataPath = None

  val defaultUserAnswers: UserAnswers =
    UserAnswers(userAnswersId, SensitiveJsObject(Json.obj("non" -> "empty")))

  val dateRange: DateRange = DateRange(
    from = LocalDate.of(2020, 4, 6),
    to = LocalDate.of(2021, 4, 5)
  )

  val yearString: String = dateRange.from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  val localDateTime: LocalDateTime =
    LocalDateTime.of(2020, 12, 12, 10, 30, 15)

  val localDate: LocalDate = LocalDate.of(1989, 10, 6)

  val tooEarlyDate: LocalDate = LocalDate.of(1899, 12, 31)

  val defaultSchemeDetails: SchemeDetails = SchemeDetails(
    schemeName,
    "testPSTR",
    SchemeStatus.Open,
    "testSchemeType",
    Some("A1234567"),
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

  val taskListInAuditEvent: ListTaskListLevel1 = ListTaskListLevel1(
    List(
      TaskListCipViewModel(
        "Section1",
        ListTaskListLevel2(
          List(
            TaskListLevel2("Sub-section 1-1.cip", "tasklist.completed"),
            TaskListLevel2("Sub-section 1-2.cip", "tasklist.notStarted"),
            TaskListLevel2(
              "Sub-section 1-3.cip",
              "tasklist.unableToStart"
            )
          )
        )
      ),
      TaskListCipViewModel(
        "Declaration incomplete",
        ListTaskListLevel2(
          List(
            TaskListLevel2("Sub-section 2-1.declaration.cip.incomplete", "tasklist.completed")
          )
        )
      ),
      TaskListCipViewModel(
        "Declaration complete",
        ListTaskListLevel2(
          List(
            TaskListLevel2("Declaration complete", "Disabled")
          )
        )
      )
    )
  )

  def userAnswersWithAddress(srn: Srn, index: Max5000): UserAnswers =
    defaultUserAnswers.unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)

  def userAnswersWithNameAndAmount(srn: Srn, index: Max5000): UserAnswers =
    defaultUserAnswers.unsafeSet(LenderNamePage(srn, index), lenderName)

  val accountingPeriods: NonEmptyList[(LocalDate, LocalDate)] = NonEmptyList.of(
    LocalDate.of(2020, 4, 6) ->
      LocalDate.of(2020, 5, 5),
    LocalDate.of(2020, 5, 6) ->
      LocalDate.of(2020, 6, 5)
  )
}
