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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.AnyContentAsEmpty
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import repositories.SessionRepository
import models.UserAnswers
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito.{times, verify, when}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class DataSavingActionSpec extends ControllerBaseSpec with ControllerBehaviours with ScalaCheckPropertyChecks {

  class Harness(sessionRepository: SessionRepository, dataRequest: DataRequest[AnyContentAsEmpty.type])(implicit
    ec: ExecutionContext
  ) extends DataSavingActionImpl(sessionRepository)(using ec) {
    def callTransform(): Future[DataRequest[AnyContentAsEmpty.type]] =
      transform(dataRequest)
  }

  val userAnswers: UserAnswers = arbitraryUserData.arbitrary.sample.value

  "Data Saving Action" - {
    "saves data" - {
      "when previousUserAnswers does not exist" in {

        val dataRequest: DataRequest[AnyContentAsEmpty.type] =
          DataRequest(request = allowedAccessRequestGen(FakeRequest()).sample.value, userAnswers = emptyUserAnswers)
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.set(any())).thenReturn(Future.successful(()))
        val action = new Harness(sessionRepository, dataRequest)

        action.callTransform().futureValue

        verify(sessionRepository, times(2)).set(any())
      }

      "when previousUserAnswers exists" in {

        val dataRequest: DataRequest[AnyContentAsEmpty.type] = DataRequest(
          request = allowedAccessRequestGen(FakeRequest()).sample.value,
          userAnswers = emptyUserAnswers,
          previousUserAnswers = Some(emptyUserAnswers)
        )
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.set(any())).thenReturn(Future.successful(()))
        val action = new Harness(sessionRepository, dataRequest)

        action.callTransform().futureValue

        verify(sessionRepository, times(3)).set(any())
      }
    }
  }
}
