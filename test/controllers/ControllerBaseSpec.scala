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
import models.{Establisher, EstablisherKind, SchemeDetails, SchemeId, SchemeStatus, UserAnswers}
import navigation.Navigator
import play.api.http._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test._
import play.api.{Application, data}
import services.{FakeTaxYearService, TaxYearService}
import uk.gov.hmrc.time.TaxYear
import utils.BaseSpec

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
    with TestValues {

  val baseUrl = "/pension-scheme-return"

  val userAnswersId: String = "id"

  val testOnwardRoute: Call = Call("GET", "/foo")

  val defaultTaxYear = TaxYear(2022)

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val defaultUserAnswers: UserAnswers = UserAnswers(userAnswersId, Json.obj("non" -> "empty"))

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    schemeDetails: SchemeDetails = defaultSchemeDetails,
                                    taxYear: TaxYear = defaultTaxYear
                                  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .bindings(
        bind[Navigator].toInstance(new Navigator()).eagerly()
      ).overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[AllowAccessActionProvider].toInstance(new FakeAllowAccessActionProvider(schemeDetails)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[DataCreationAction].toInstance(new FakeDataCreationAction(userAnswers.getOrElse(emptyUserAnswers))),
        bind[TaxYearService].toInstance(new FakeTaxYearService(taxYear.starts))
      )

  def runningApplication[T](block: Application => T): T = {
    running(_ => applicationBuilder())(block)
  }

  def formData[A](form: data.Form[A], range: A) = form.fill(range).data.toList
}

trait TestValues { _ : BaseSpec =>
  val accountNumber = "12345678"
  val sortCode = "123456"
  val srn: SchemeId.Srn = srnGen.sample.value

  val defaultSchemeDetails: SchemeDetails = SchemeDetails(
    "testSRN",
    "testSchemeName",
    "testPSTR",
    SchemeStatus.Open,
    "testSchemeType",
    Some("testAuthorisingPSAID"),
    List(Establisher("testFirstName testLastName", EstablisherKind.Individual))
  )
}
