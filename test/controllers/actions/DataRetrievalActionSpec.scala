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

package controllers.actions

import play.api.test.FakeRequest
import play.api.mvc.AnyContentAsEmpty
import repositories.SessionRepository
import models.UserAnswers
import models.requests.{AllowedAccessRequest, OptionalDataRequest}
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito.when

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DataRetrievalActionSpec extends BaseSpec {

  class Harness(sessionRepository: SessionRepository) extends DataRetrievalActionImpl(sessionRepository) {
    def callTransform(): Future[OptionalDataRequest[AnyContentAsEmpty.type]] =
      transform(request)
  }

  val request: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value

  "Data Retrieval Action" - {

    "set userAnswers to 'None' in the request" - {
      "there is no data in the cache" in {

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any())).thenReturn(Future(None))
        val action = new Harness(sessionRepository)

        val result = action.callTransform().futureValue

        result.userAnswers must not be defined
      }
    }

    "build a userAnswers object and add it to the request" - {
      "when there is data in the cache" in {

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any()))
          .thenReturn(Future(Some(UserAnswers("id"))))
        val action = new Harness(sessionRepository)

        val result = action.callTransform().futureValue

        result.userAnswers mustBe defined
      }
    }
  }
}
