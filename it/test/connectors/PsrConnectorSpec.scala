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
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import handlers.GetPsrException
import models.SchemeId.Srn
import models.UserAnswers
import models.UserAnswers.SensitiveJsObject
import models.requests.{AllowedAccessRequest, DataRequest}
import org.scalatest.exceptions.TestFailedException
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{__, JsResultException, Json, JsonValidationError}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestValues

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

class PsrConnectorSpec extends BaseConnectorSpec with CommonTestValues {

  implicit val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] =
    DataRequest(allowedAccessRequest, UserAnswers("id", SensitiveJsObject(Json.obj("non" -> "empty"))))

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionSchemeReturn.port" -> wireMockPort)

  private implicit val queryParamsToJava: Map[String, String] => java.util.Map[String, StringValuePattern] = _.map {
    case (k, v) => k -> equalTo(v)
  }.asJava

  def stubGet(url: String, queryParams: Map[String, String], response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      get(urlPathTemplate(url))
        .withQueryParams(queryParams)
        .willReturn(response)
    )

  def stubPost(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer
      .stubFor(
        post(urlEqualTo(url))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/json"))
          .willReturn(response)
      )

  private val getStandardPsrDetailsUrl = s"/pension-scheme-return/psr/standard/$commonPstr"
  private val submitStandardUrl = s"/pension-scheme-return/psr/standard"
  private val submitPrePopulatedUrl = s"/pension-scheme-return/psr/pre-populated"
  private val getOverviewUrl = s"/pension-scheme-return/psr/overview/$commonPstr"
  private val startDates = Seq(commonStartDate)
  private val getVersionsForYearsUrl =
    s"/pension-scheme-return/psr/versions/years/$commonPstr?startDates=${startDates.mkString("&startDates=")}"
  private val getVersionsUrl = s"/pension-scheme-return/psr/versions/$commonPstr?startDate=$commonStartDate"

  def connector(implicit app: Application): PSRConnector = injected[PSRConnector]

  "getStandardPsrDetails" - {

    "return standard Psr for start date and version" in runningApplication { implicit app =>
      stubGet(
        getStandardPsrDetailsUrl,
        Map(
          "periodStartDate" -> commonStartDate,
          "psrVersion" -> commonVersion
        ),
        ok(Json.stringify(minimalSubmissionJson))
          .withHeader("Content-Type", "application/json")
          .withHeader("userName", "userName")
          .withHeader("schemeName", "schemeName")
          .withHeader("srn", "S0000000042")
      )

      val result =
        connector
          .getStandardPsrDetails(
            commonPstr,
            None,
            Some(commonStartDate),
            Some(commonVersion),
            commonFallbackCall,
            commonUserName,
            commonSchemeName,
            Srn(commonSrn).get
          )
          .futureValue

      result mustBe Some(minimalSubmissionData)
    }

    "return standard Psr for fb number" in runningApplication { implicit app =>
      stubGet(
        getStandardPsrDetailsUrl,
        Map(
          "fbNumber" -> commonFbNumber
        ),
        ok(Json.stringify(minimalSubmissionJson))
          .withHeader("Content-Type", "application/json")
          .withHeader("userName", "userName")
          .withHeader("schemeName", "schemeName")
          .withHeader("srn", "S0000000042")
      )

      val result =
        connector
          .getStandardPsrDetails(
            commonPstr,
            Some(commonFbNumber),
            None,
            None,
            commonFallbackCall,
            commonUserName,
            commonSchemeName,
            Srn(commonSrn).get
          )
          .futureValue

      result mustBe Some(minimalSubmissionData)
    }

    "return a details not found when 404 returned" in runningApplication { implicit app =>
      stubGet(
        getStandardPsrDetailsUrl,
        Map(
          "periodStartDate" -> commonStartDate,
          "psrVersion" -> commonVersion
        ),
        notFound()
          .withHeader("Content-Type", "application/json")
          .withHeader("userName", "userName")
          .withHeader("schemeName", "schemeName")
          .withHeader("srn", "S0000000042")
      )

      val result =
        connector
          .getStandardPsrDetails(
            commonPstr,
            Some(commonFbNumber),
            None,
            None,
            commonFallbackCall,
            commonUserName,
            commonSchemeName,
            Srn(commonSrn).get
          )
          .futureValue

      result mustBe None
    }
  }

  "submitStandardPsrDetails" - {

    "submit standard Psr" in runningApplication { implicit app =>
      stubPost(
        submitStandardUrl,
        noContent()
          .withHeader("Content-Type", "application/json")
          .withHeader("userName", "userName")
          .withHeader("schemeName", "schemeName")
          .withHeader("srn", "S0000000042")
      )

      val result: Either[String, Unit] =
        connector
          .submitPsrDetails(minimalSubmissionData, commonUserName, commonSchemeName, Srn(commonSrn).get)
          .futureValue

      result mustBe Right(())
    }
  }

  "submitPrePopulatedPsrDetails" - {

    "submit pre-populated Psr" in runningApplication { implicit app =>
      stubPost(
        submitPrePopulatedUrl,
        noContent()
          .withHeader("Content-Type", "application/json")
          .withHeader("userName", "userName")
          .withHeader("schemeName", "schemeName")
          .withHeader("srn", "S0000000042")
      )

      val result: Either[String, Unit] =
        connector
          .submitPrePopulatedPsr(minimalSubmissionData, commonUserName, commonSchemeName, Srn(commonSrn).get)
          .futureValue

      result mustBe Right(())
    }
  }
  "get getVersionsForYears" - {

    "return getVersionsForYears for start date and end date" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        ok(Json.stringify(getVersionsForYearsJson)).withHeader("srn", "S0000000042")
      )

      val result = connector
        .getVersionsForYears(
          commonPstr,
          Seq(commonStartDate),
          Srn(commonSrn).get,
          controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
        )
        .futureValue

      result mustBe versionsForYearsResponse
    }

    "throw GetPsrException when getVersionsForYears called and 403 returned" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        forbidden().withBody(Json.stringify(getVersionsForYears403Json)).withHeader("srn", "S0000000042")
      )
      val err: TestFailedException = intercept[TestFailedException](
        connector
          .getVersionsForYears(
            commonPstr,
            Seq(commonStartDate),
            Srn(commonSrn).get,
            controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
          )
          .futureValue
      )

      err.cause.get mustBe a[GetPsrException]
    }

    "return empty list when getVersionsForYears called and 503 returned" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        serviceUnavailable().withBody(Json.stringify(getVersions503Json)).withHeader("srn", "S0000000042")
      )

      val result = connector
        .getVersionsForYears(
          commonPstr,
          Seq(commonStartDate),
          Srn(commonSrn).get,
          controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
        )
        .futureValue

      result mustBe List()
    }

    "return empty list when getVersionsForYears called and data not found" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        notFound().withBody(Json.stringify(getVersionsForYearsNotFoundJson)).withHeader("srn", "S0000000042")
      )

      val result = connector
        .getVersionsForYears(
          commonPstr,
          Seq(commonStartDate),
          Srn(commonSrn).get,
          controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
        )
        .futureValue

      result mustBe List()
    }

    "return JsResultException when the first name is invalid" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        ok(Json.stringify(getVersionsForYearsJsonWithInvalidFirstName)).withHeader("srn", "S0000000042")
      )

      val err: TestFailedException = intercept[TestFailedException](
        connector
          .getVersionsForYears(
            commonPstr,
            Seq(commonStartDate),
            Srn(commonSrn).get,
            controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
          )
          .futureValue
      )

      err.cause match {
        case Some(JsResultException(List((path, List(JsonValidationError(List(jsErr))))))) =>
          path mustBe __ \ 0 \ "data" \ 0 \ "reportSubmitterDetails" \ "individualDetails" \ "firstName"
          jsErr mustBe "error.expected.jsstring"
        case _ => fail("Expected JsResultException with a single error")
      }
    }
  }

  "get getVersions" - {

    "return getVersions data for a start date" in runningApplication { implicit app =>
      stubGet(
        getVersionsUrl,
        ok(Json.stringify(getVersionsJson)).withHeader("srn", "S0000000042")
      )

      val result = connector
        .getVersions(
          commonPstr,
          commonStartDate,
          Srn(commonSrn).get,
          controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
        )
        .futureValue

      result mustBe versionsResponse
    }

    "throw GetPsrException when getVersionsForYears called and 403 returned" in runningApplication { implicit app =>
      stubGet(
        getVersionsUrl,
        forbidden().withBody(Json.stringify(getVersionsForYears403Json)).withHeader("srn", "S0000000042")
      )

      val err: TestFailedException = intercept[TestFailedException](
        connector
          .getVersions(
            commonPstr,
            commonStartDate,
            Srn(commonSrn).get,
            controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
          )
          .futureValue
      )

      err.cause.get mustBe a[GetPsrException]
    }

    "return empty list when getVersionsForYears called and 503 returned" in runningApplication { implicit app =>
      stubGet(
        getVersionsUrl,
        serviceUnavailable().withBody(Json.stringify(getVersions503Json)).withHeader("srn", "S0000000042")
      )

      val result = connector
        .getVersions(
          commonPstr,
          commonStartDate,
          Srn(commonSrn).get,
          controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
        )
        .futureValue

      result mustBe List()
    }

    "return empty list when getVersions called and data not found" in runningApplication { implicit app =>
      stubGet(
        getVersionsUrl,
        forbidden().withBody(Json.stringify(getVersionsForYearsNotFoundJson)).withHeader("srn", "S0000000042")
      )

      val result = connector
        .getVersionsForYears(
          commonPstr,
          Seq(commonStartDate),
          Srn(commonSrn).get,
          controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
        )
        .futureValue

      result mustBe List()
    }
  }

  "get overview" - {

    "return overview details for start date and end date" in runningApplication { implicit app =>
      stubGet(
        getOverviewUrl,
        Map(
          "fromDate" -> commonStartDate,
          "toDate" -> commonEndDate
        ),
        ok(Json.stringify(overviewJson)).withHeader("srn", "S0000000042")
      )

      val result = connector
        .getOverview(
          commonPstr,
          commonStartDate,
          commonEndDate,
          Srn(commonSrn).get,
          controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
        )
        .futureValue

      result mustBe Some(overviewResponse)
    }

    "throw GetPsrException when no overview details" in runningApplication { implicit app =>
      stubGet(
        getOverviewUrl,
        Map(
          "fromDate" -> commonEndDate,
          "toDate" -> commonStartDate
        ),
        forbidden().withBody(Json.stringify(overviewJson)).withHeader("srn", "S0000000042")
      )

      val err: TestFailedException = intercept[TestFailedException](
        connector
          .getOverview(
            commonPstr,
            commonEndDate,
            commonStartDate,
            Srn(commonSrn).get,
            controllers.routes.OverviewController.onPageLoad(Srn(commonSrn).get)
          )
          .futureValue
      )

      err.cause.get mustBe a[GetPsrException]
    }
  }
}
