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

package controllers.actions

import models.UserAnswers
import models.requests.OptionalDataRequest
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.SessionRepository
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends BaseSpec {

  class Harness(sessionRepository: SessionRepository) extends DataRetrievalActionImpl(sessionRepository) {
    def callTransform(): Future[OptionalDataRequest[AnyContentAsEmpty.type]] =
      transform(request)
  }

  val request = allowedAccessRequestGen(FakeRequest()).sample.value

  "Data Retrieval Action" should {

    "set userAnswers to 'None' in the request" when {
      "there is no data in the cache" in {

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(request.request.getUserId)) thenReturn Future(None)
        val action = new Harness(sessionRepository)

        val result = action.callTransform().futureValue

        result.userAnswers must not be defined
      }
    }

    "build a userAnswers object and add it to the request" when {
      "when there is data in the cache" in {

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(request.request.getUserId)) thenReturn Future(Some(UserAnswers("id")))
        val action = new Harness(sessionRepository)

        val result = action.callTransform().futureValue

        result.userAnswers mustBe defined
      }
    }
  }
}
