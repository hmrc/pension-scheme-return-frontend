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
import models.{PreparedUpload, Reference, UploadForm, UpscanFileReference, UpscanInitiateResponse}
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class UpscanConnectorSpec extends BaseConnectorSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.upscan.port" -> wireMockServer.port())

  private val callBackUrl = "http://localhost:9000/test-callback-url"
  private val successRedirectUrl = "/test-successful-redxirect-url"
  private val failureRedirectUrl = "/test-failure-redirect-url"

  private val reference = "test-ref"
  private val postTarget = "test-post-target"
  private val formFields = Map("test" -> "fields")

  def connector(implicit app: Application): UpscanConnector = injected[UpscanConnector]

  "UpscanConnector" - {
    ".initiate" - {
      "return an upscan initiation response" in runningApplication { implicit app =>
        val httpResponse = PreparedUpload(Reference(reference), UploadForm(postTarget, formFields))
        val expectedResponse = UpscanInitiateResponse(UpscanFileReference(reference), postTarget, formFields)

        UpscanHelper.stubPost(ok(Json.toJson(httpResponse).toString))

        val result = connector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl).futureValue

        result mustBe expectedResponse
      }
    }


    "return an error if the file size is larger than 1MB" in runningApplication { implicit app =>
      val errorResponseJson = Json.obj(
        "code" -> "EntityTooLarge",
        "message" -> "The uploaded file is too large. The maximum file size is 1MB."
      )

      UpscanHelper.stubPost(
        aResponse()
          .withStatus(Status.REQUEST_ENTITY_TOO_LARGE)
          .withBody(errorResponseJson.toString())
      )

      val exception = intercept[UpstreamErrorResponse] {
        connector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl).futureValue
      }

      exception.statusCode mustBe Status.REQUEST_ENTITY_TOO_LARGE
      exception.message must include("The uploaded file is too large. The maximum file size is 1MB.")
    }

  }

  object UpscanHelper {
    val url = "/upscan/v2/initiate"

    def stubPost(response: ResponseDefinitionBuilder): StubMapping =
      wireMockServer
        .stubFor(
          post(urlEqualTo(url))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/json"))
            .willReturn(response)
        )
  }
}
