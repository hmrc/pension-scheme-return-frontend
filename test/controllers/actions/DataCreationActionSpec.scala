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

import models.requests.{DataRequest, OptionalDataRequest}
import org.mockito.ArgumentMatchers.any
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.SessionRepository
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DataCreationActionSpec extends BaseSpec {

  class Harness(request: OptionalDataRequest[AnyContentAsEmpty.type], sessionRepository: SessionRepository)(implicit ec: ExecutionContext)
    extends DataCreationActionImpl(sessionRepository)(ec) {
      def callTransform(): Future[DataRequest[AnyContentAsEmpty.type]] = {
        transform(request)
      }
  }

  val request = allowedAccessRequestGen(FakeRequest()).sample.value
  val userAnswers = arbitraryUserData.arbitrary.sample.value

  "Data Creation Action" should {

    "return user answers" when {
      "there is data in the cache" in {

        val optionalDataRequest = OptionalDataRequest(request, Some(userAnswers))
        val action = new Harness(optionalDataRequest, mock[SessionRepository])

        val result = action.callTransform().futureValue

        result mustBe DataRequest(request, userAnswers)
      }
    }

    "add user answers to repository" when {
      "when there is no data in the cache" in {

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.set(any())).thenReturn(Future.successful(()))

        val optionalDataRequest = OptionalDataRequest(request, None)
        val action = new Harness(optionalDataRequest, sessionRepository)

        val result = action.callTransform().futureValue


        result.request mustBe request
        result.userAnswers.id mustBe request.getUserId + request.schemeDetails.srn
        verify(sessionRepository, times(1)).set(any())
      }
    }
  }
}
