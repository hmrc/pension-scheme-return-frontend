package connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{PreparedUpload, Reference, UploadForm, UpscanFileReference, UpscanInitiateResponse}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier

class UpscanConnectorSpec extends BaseConnectorSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit override lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("urls.upscan.baseUrl" -> wireMockUrl)

  private val callBackUrl = "http://localhost:9000/test-callback-url"
  private val successRedirectUrl = "/test-successful-redirect-url"
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
