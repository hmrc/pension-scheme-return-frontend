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

package services

import play.api.test.FakeRequest
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import connectors.PSRConnector
import controllers.TestValues
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.ArgumentMatchers.any
import utils.{BaseSpec, CommonTestValues}
import org.mockito.Mockito._
import models.backend.responses.{PsrVersionsForYearsResponse, PsrVersionsResponse}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PsrVersionsServiceSpec extends BaseSpec with TestValues with CommonTestValues {

  override def beforeEach(): Unit = {
    when(mockReq.schemeDetails).thenReturn(allowedAccessRequest.schemeDetails)
    when(mockReq.pensionSchemeId).thenReturn(allowedAccessRequest.pensionSchemeId)
    reset(mockConnector)
  }

  private val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value
  private val mockConnector = mock[PSRConnector]
  private val mockReq = mock[DataRequest[AnyContent]]
  private val service = new PsrVersionsService(mockConnector)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "PsrVersionsService" - {

    "should get versions" in {
      when(
        mockConnector.getVersions(any(), any(), any(), any())(using any(), any(), any())
      ).thenReturn(Future.successful(versionsResponse))

      whenReady(
        service.getVersions(pstr, commonStartDate, srn)(using mockReq, implicitly, implicitly)
      ) { (result: Seq[PsrVersionsResponse]) =>
        result mustBe versionsResponse
      }
    }

    "should not get versions when connector returns empty seq" in {
      when(
        mockConnector.getVersions(any(), any(), any(), any())(using any(), any(), any())
      ).thenReturn(Future.successful(Seq.empty))

      whenReady(
        service.getVersions(pstr, commonStartDate, srn)(using mockReq, implicitly, implicitly)
      ) { (result: Seq[PsrVersionsResponse]) =>
        result mustBe Seq.empty
      }
    }

    "should get versions for years" in {
      when(
        mockConnector.getVersionsForYears(any(), any(), any(), any())(using any(), any(), any())
      ).thenReturn(Future.successful(versionsForYearsResponse))

      whenReady(
        service.getVersionsForYears(pstr, Seq(commonStartDate, commonEndDate), srn)(using
          mockReq.request,
          implicitly,
          implicitly
        )
      ) { (result: Seq[PsrVersionsForYearsResponse]) =>
        result mustBe versionsForYearsResponse
      }
    }

    "should not get versions for years when connector returns empty seq" in {
      when(
        mockConnector.getVersionsForYears(any(), any(), any(), any())(using any(), any(), any())
      ).thenReturn(Future.successful(Seq.empty))

      whenReady(
        service.getVersionsForYears(pstr, Seq(commonStartDate, commonEndDate), srn)(using
          mockReq.request,
          implicitly,
          implicitly
        )
      ) { (result: Seq[PsrVersionsForYearsResponse]) =>
        result mustBe Seq.empty
      }
    }
  }
}
