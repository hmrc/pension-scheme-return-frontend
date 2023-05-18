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

import controllers.actions._
import models.UserAnswers.SensitiveJsObject
import models._
import play.api.Application
import play.api.data.Form
import play.api.http._
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Call
import play.api.test._
import queries.Settable
import uk.gov.hmrc.time.TaxYear
import utils.{BaseSpec, DisplayMessageUtils}

import java.time.{LocalDate, LocalDateTime}

trait ControllerBaseSpec
    extends BaseSpec
    with ControllerBehaviours
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

trait TestValues { _: BaseSpec =>
  val accountNumber = "12345678"
  val sortCode = "123456"
  val srn: SchemeId.Srn = srnGen.sample.value
  val schemeName = "testSchemeName"
  val email = "testEmail"
  val uploadKey: UploadKey = UploadKey("test-userid", srn)

  val userAnswersId: String = "id"
  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val defaultUserAnswers: UserAnswers =
    UserAnswers(userAnswersId, SensitiveJsObject(Json.obj("non" -> "empty")))

  val localDateTime: LocalDateTime =
    LocalDateTime.of(2020, 12, 12, 10, 30, 15)

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
    Some(IndividualDetails("testFirstName", Some("testMiddleName"), "testLastName")),
    rlsFlag = false,
    deceasedFlag = false
  )

  val memberDetails: NameDOB = NameDOB(
    "testFirstName",
    "testLastName",
    LocalDate.of(1990, 12, 12)
  )
}
