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
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestValues

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

class PsrConnectorSpec extends BaseConnectorSpec with CommonTestValues {

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
      )

      val result =
        connector
          .getStandardPsrDetails(commonPstr, None, Some(commonStartDate), Some(commonVersion), commonFallbackCall)
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
      )

      val result =
        connector.getStandardPsrDetails(commonPstr, Some(commonFbNumber), None, None, commonFallbackCall).futureValue

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
      )

      val result =
        connector.getStandardPsrDetails(commonPstr, Some(commonFbNumber), None, None, commonFallbackCall).futureValue

      result mustBe None
    }
  }

  "submitStandardPsrDetails" - {

    "submit standard Psr" in runningApplication { implicit app =>
      stubPost(submitStandardUrl, noContent())

      val result: Either[String, Unit] = connector.submitPsrDetails(minimalSubmissionData).futureValue

      result mustBe Right(())
    }
  }

  "get getVersionsForYears" - {

    "return getVersionsForYears for start date and end date" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        ok(Json.stringify(getVersionsForYearsJson))
      )

      val result = connector
        .getVersionsForYears(commonPstr, Seq(commonStartDate))
        .futureValue

      result mustBe versionsForYearsResponse
    }

    "return empty list when getVersionsForYears called and 403 returned" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        forbidden().withBody(Json.stringify(getVersionsForYears403Json))
      )

      val result = connector.getVersionsForYears(commonPstr, Seq(commonStartDate)).futureValue

      result mustBe List()
    }

    "return empty list when getVersionsForYears called and data not found" in runningApplication { implicit app =>
      stubGet(
        getVersionsForYearsUrl,
        forbidden().withBody(Json.stringify(getVersionsForYearsNotFoundJson))
      )

      val result = connector.getVersionsForYears(commonPstr, Seq(commonStartDate)).futureValue

      result mustBe List()
    }
  }

  "get getVersions" - {

    "return getVersions data for a start date" in runningApplication { implicit app =>
      stubGet(
        getVersionsUrl,
        ok(Json.stringify(getVersionsJson))
      )

      val result = connector
        .getVersions(commonPstr, commonStartDate)
        .futureValue

      result mustBe versionsResponse
    }

    "return empty list when getVersionsForYears called and 403 returned" in runningApplication { implicit app =>
      stubGet(
        getVersionsUrl,
        forbidden().withBody(Json.stringify(getVersionsForYears403Json))
      )

      val result = connector.getVersions(commonPstr, commonStartDate).futureValue

      result mustBe List()
    }

    "return empty list when getVersions called and data not found" in runningApplication { implicit app =>
      stubGet(
        getVersionsUrl,
        forbidden().withBody(Json.stringify(getVersionsForYearsNotFoundJson))
      )

      val result = connector.getVersionsForYears(commonPstr, Seq(commonStartDate)).futureValue

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
        ok(Json.stringify(overviewJson))
      )

      val result = connector.getOverview(commonPstr, commonStartDate, commonEndDate).futureValue

      result mustBe Some(overviewResponse)
    }

    "return no overview details when 403" in runningApplication { implicit app =>
      stubGet(
        getOverviewUrl,
        Map(
          "fromDate" -> commonEndDate,
          "toDate" -> commonStartDate
        ),
        forbidden().withBody(Json.stringify(overviewJson))
      )

      val result = connector.getOverview(commonPstr, commonEndDate, commonStartDate).futureValue

      result mustBe None
    }
  }
}
