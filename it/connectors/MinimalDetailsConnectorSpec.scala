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

package connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{badRequest, equalTo, forbidden, get, notFound, ok, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.Constants
import connectors.MinimalDetailsError.{DelimitedAdmin, DetailsNotFound}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class MinimalDetailsConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionAdministrator.port" -> wireMockPort)

  val url = "/pension-administrator/get-minimal-psa"

  def stubGet(key: String, id: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo(url))
        .withHeader(key, equalTo(id))
        .willReturn(response)
    )

  val psaId = psaIdGen.sample.value
  val pspId = pspIdGen.sample.value

  running(_ => applicationBuilder) { implicit app =>

    lazy val connector: MinimalDetailsConnector = injected[MinimalDetailsConnector]

    "fetch" should {

      "return psa minimal details" in {

        val md = minimalDetailsGen.sample.value
        stubGet("psaId", psaId.value, ok(Json.stringify(Json.toJson(md))))

        val result = connector.fetch(psaId).futureValue

        result mustBe Right(md)
      }

      "return psp minimal details" in {

        val md = minimalDetailsGen.sample.value
        stubGet("pspId", pspId.value, ok(Json.stringify(Json.toJson(md))))

        val result = connector.fetch(pspId).futureValue

        result mustBe Right(md)
      }

      "return a details not found when 404 returned with message" in {

        stubGet("psaId", psaId.value, notFound.withBody(Constants.detailsNotFound))

        val result = connector.fetch(psaId).futureValue

        result mustBe Left(DetailsNotFound)
      }

      "return a delimited admin error when forbidden with delimited admin error returned" in {

        val body = stringContains(Constants.delimitedPSA).sample.value

        stubGet("psaId", psaId.value, forbidden.withBody(body))

        val result = connector.fetch(psaId).futureValue

        result mustBe Left(DelimitedAdmin)
      }

      "fail future when a 404 returned" in {
        stubGet("psaId", psaId.value, notFound)

        assertThrows[Exception] {
          connector.fetch(psaId).futureValue
        }
      }

      "fail future for any other http failure code" in {
        stubGet("psaId", psaId.value, badRequest)

        assertThrows[Exception] {
          connector.fetch(psaId).futureValue
        }
      }
    }
  }
}