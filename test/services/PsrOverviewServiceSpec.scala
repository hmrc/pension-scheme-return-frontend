/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import play.api.test.FakeRequest
import models.backend.responses.YesNo.{No, Yes}
import play.api.mvc.AnyContentAsEmpty
import connectors.PSRConnector
import controllers.TestValues
import models.SchemeId.Srn
import models.backend.responses.PsrReportType.Standard
import models.requests.AllowedAccessRequest
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito._
import models.backend.responses.OverviewResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import java.time.LocalDate

class PsrOverviewServiceSpec extends BaseSpec with TestValues {
  private val mockConnector = mock[PSRConnector]

  private val service = new PsrOverviewService(mockConnector)

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value

  override def beforeEach(): Unit =
    reset(mockConnector)

  "PSROverviewService" - {
    "getOverview should return the empty overview when ETMP response is empty" in {
      when(mockConnector.getOverview(any(), any(), any(), any())(any(), any(), any())).thenReturn(
        Future.successful(Some(Nil))
      )

      whenReady(
        service.getOverview("test123", "2023", "2025", new Srn("Test12345"))
      ) { result =>
        verify(mockConnector, times(1)).getOverview(any(), any(), any(), any())(any(), any(), any())
        result mustBe Some(Nil)
      }

    }

    "getOverview should return ab overview when ETMP response has data" in {
      val overviewResponse = OverviewResponse(
        periodStartDate = LocalDate.of(2024, 4, 6),
        periodEndDate = LocalDate.of(2025, 4, 5),
        numberOfVersions = Some(1),
        submittedVersionAvailable = Some(Yes),
        compiledVersionAvailable = Some(No),
        tpssReportPresent = Some(No),
        ntfDateOfIssue = Some(LocalDate.of(2025, 5, 1)),
        psrDueDate = Some(LocalDate.of(2026, 1, 31)),
        psrReportType = Some(Standard)
      )

      when(mockConnector.getOverview(any(), any(), any(), any())(any(), any(), any())).thenReturn(
        Future.successful(Some(List(overviewResponse)))
      )

      whenReady(
        service.getOverview("test123", "2023", "2025", new Srn("Test12345"))
      ) { result =>
        verify(mockConnector, times(1)).getOverview(any(), any(), any(), any())(any(), any(), any())
        result mustBe Some(List(overviewResponse))
      }

    }

//    "getOverview should exclude date ranges that begin before the service start date" - {
//      Seq(
//        (2021, 2021),
//        (2022, 2021),
//        (2023, 2021),
//        (2028, 2021),
//        (2030, 2023)
//      ).foreach {
//        case (currentPeriodStartYear, earliestStartYear) =>
//          s"when the current year is $currentPeriodStartYear" in {
//            val earliestStartDate = s"$earliestStartYear-04-06"
//            val currentPeriodStartDate = s"$currentPeriodStartYear-04-06"
//            when(mockConnector.getOverview(any(), any(), any(), any())(any(), any(), any())).thenReturn(
//              Future.successful(Some(Nil))
//            )
//            when(mockConfig.allowedStartDateRange).thenReturn(LocalDate.of(currentPeriodStartYear, 4, 6))
//
//            whenReady(
//              service.getOverview(
//                "test123",
//                earliestStartDate,
//                currentPeriodStartDate,
//                new Srn("Test12345")
//              )
//            ) { result =>
//              verify(mockConnector, times(1))
//                .getOverview(
//                  any(),
//                  same(earliestStartDate),
//                  same(currentPeriodStartDate),
//                  any()
//                )(any(), any(), any())
//              result mustBe Some(Nil)
//            }
//          }
//      }
//    }
  }
}
