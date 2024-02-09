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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.backend.responses._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext.Implicits.global

class PSRConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit override lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionSchemeReturn.port" -> wireMockPort)

  private val pstr = "testPstr"
  private val startDate = LocalDate.of(2020, 4, 6)
  private val url = s"/pension-scheme-return/psr/versions/$pstr?startDate=$startDate"

  def connector(implicit app: Application): PSRConnector = injected[PSRConnector]

//  "getVersions" - {
//    "return the seq of PsrVersionsResponse returned from BE" in runningApplication {  implicit app =>
//
//      val sampleVersionsResponse: Seq[PsrVersionsResponse] = Seq(
//        PsrVersionsResponse(
//          reportFormBundleNumber = "123456785012",
//          reportVersion = 1,
//          reportStatus = ReportStatusCompiled,
//          compilationOrSubmissionDate = LocalDateTime.parse("2023-04-02T09:30:47"),
//          reportSubmitterDetails = ReportSubmitterDetails(
//            reportSubmittedBy = "PSP",
//            organisationOrPartnershipDetails = Some(
//              OrganisationOrPartnershipDetails(
//                organisationOrPartnershipName = "ABC Limited"
//              )
//            ),
//            individualDetails = None
//          ),
//          psaDetails = PsaDetails(
//            psaOrganisationOrPartnershipDetails = Some(
//              PsaOrganisationOrPartnershipDetails(
//                organisationOrPartnershipName = "XYZ Limited"
//              )
//            ),
//            psaIndividualDetails = None
//          )
//        )
//      )
//
//      wireMockServer.stubFor(
//        get(urlEqualTo(url))
//          .willReturn(
//            ok
//              .withHeader("Content-Type", "application/json")
//              .withBody(sampleVersionsResponse.toString())
//          )
//      )
//      val result = connector.getVersions(pstr, "2022-04-06").futureValue
//      result mustBe sampleVersionsResponse
//    }
//  }
}