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
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito.{times, verify, when}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class DataCreationActionSpec extends BaseSpec {

  class Harness(request: AllowedAccessRequest[AnyContentAsEmpty.type], sessionRepository: SessionRepository)(
    implicit ec: ExecutionContext
  ) extends DataCreationActionImpl(sessionRepository)(ec) {
    def callTransform(): Future[DataRequest[AnyContentAsEmpty.type]] =
      transform(request)
  }

  val request: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  val userAnswers: UserAnswers = arbitraryUserData.arbitrary.sample.value

  "Data Creation Action" - {

    "add user answers to repository" - {
      "when there is no data in the cache" in {

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.set(any())).thenReturn(Future.successful(()))

        val action = new Harness(request, sessionRepository)

        val result = action.callTransform().futureValue

        result.request mustBe request
        result.userAnswers.id mustBe request.getUserId + request.srn
        verify(sessionRepository, times(1)).set(any())
      }
    }
  }
}
