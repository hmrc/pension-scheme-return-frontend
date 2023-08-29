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

package services

import cats.data.NonEmptyList
import connectors.PSRConnector
import controllers.TestValues
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PSRSubmissionServiceSpec extends BaseSpec with TestValues {

  private val mockConnector = mock[PSRConnector]

  private val service = new PSRSubmissionService(mockConnector)

  private implicit val hc = HeaderCarrier()

  "PSRSubmissionService" - {
    "proxy request successfully" in {

      when(mockConnector.submitMinimalRequiredDetails(any())(any(), any())).thenReturn(Future.successful(()))

      val result = service.submitMinimalRequiredDetails(
        pstr,
        periodStart = localDate,
        periodEnd = localDate,
        NonEmptyList.of(dateRange),
        reasonForNoBankAccount = None,
        schemeMemberNumbers
      )

      await(result) mustEqual (())
    }
  }
}
