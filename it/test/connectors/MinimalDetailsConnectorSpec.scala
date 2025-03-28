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

package connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.Constants
import connectors.MinimalDetailsError.{DelimitedAdmin, DetailsNotFound}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class MinimalDetailsConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionAdministrator.port" -> wireMockPort)

  val url = "/pension-administrator/get-minimal-details-self"

  def stubGet(loggedInAsPsa: Boolean, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo(url))
        .withHeader("loggedInAsPsa", equalTo(String.valueOf(loggedInAsPsa)))
        .willReturn(response)
    )

  def connector(implicit app: Application): MinimalDetailsConnector = injected[MinimalDetailsConnector]

  "fetch" - {

    "return psa minimal details" in runningApplication { implicit app =>
      val md = minimalDetailsGen.sample.value
      stubGet(loggedInAsPsa = true, ok(Json.stringify(Json.toJson(md))))

      val result = connector.fetch(loggedInAsPsa = true).futureValue

      result mustBe Right(md)
    }

    "return psp minimal details" in runningApplication { implicit app =>
      val md = minimalDetailsGen.sample.value
      stubGet(loggedInAsPsa = false, ok(Json.stringify(Json.toJson(md))))

      val result = connector.fetch(loggedInAsPsa = false).futureValue

      result mustBe Right(md)
    }

    "return a details not found when 404 returned with message" in runningApplication { implicit app =>
      stubGet(loggedInAsPsa = true, notFound.withBody(Constants.detailsNotFound))

      val result = connector.fetch(loggedInAsPsa = true).futureValue

      result mustBe Left(DetailsNotFound)
    }

    "return a delimited admin error when forbidden with delimited admin error returned" in runningApplication {
      implicit app =>
        val body = stringContains(Constants.delimitedPSA).sample.value

        stubGet(loggedInAsPsa = true, forbidden.withBody(body))

        val result = connector.fetch(loggedInAsPsa = true).futureValue

        result mustBe Left(DelimitedAdmin)
    }

    "fail future when a 404 returned" in runningApplication { implicit app =>
      stubGet(loggedInAsPsa = true, notFound)

      assertThrows[Exception] {
        connector.fetch(loggedInAsPsa = true).futureValue
      }
    }

    "fail future for any other http failure code" in runningApplication { implicit app =>
      stubGet(loggedInAsPsa = true, badRequest)

      assertThrows[Exception] {
        connector.fetch(loggedInAsPsa = true).futureValue
      }
    }
  }
}
