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
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, ok, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{ALFAddress, ALFAddressResponse, ALFCountry, PensionSchemeId}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.baseApplicationBuilder.injector
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier

class AddressLookupConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.address-lookup.port" -> wireMockPort)

  val url = "/lookup"

  def stubGet(key: String, id: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo(url))
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/json"))
        .willReturn(response)
    )

  val psaId: PensionSchemeId.PsaId = psaIdGen.sample.value
  val pspId: PensionSchemeId.PspId = pspIdGen.sample.value

  def connector(implicit app: Application): AddressLookupConnector = injected[AddressLookupConnector]

  "lookup" - {

    "return address lookup details" in runningApplication { implicit app =>
      val md = addressGen.sample.value
      stubGet("psaId", psaId.value,
        ok(
          Json.stringify(
            Json.arr(
              Json.obj(
                "id" -> "test id", "address" -> Json.obj(
                  "lines" -> Json.arr("line one"),
                  "town" -> "town",
                  "postcode" -> "ZZ1 1ZZ",
                  "country" -> Json.obj("code" -> "code", "name" -> "address")
                )
              )
            )
          )
        )
      )

      val result = connector.lookup("ZZ1 1ZZ", None).futureValue

      val resultOne = connector.lookup("ZZ1 1ZZ", Some("town")).futureValue

      result mustBe List(ALFAddressResponse("test id", ALFAddress(List("line one"), "town", "ZZ1 1ZZ", ALFCountry("code", "address"))))

      resultOne mustBe List(ALFAddressResponse("test id", ALFAddress(List("line one"), "town", "ZZ1 1ZZ", ALFCountry("code", "address"))))

    }

  }
}