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

package controllers

import play.api.test.FakeRequest
import play.api.inject.bind
import repositories.SessionRepository
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

class KeepAliveControllerSpec extends ControllerBaseSpec {

  "keepAlive" - {

    "keep the answers alive and return OK" - {
      "the user has answered some questions" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.keepAlive(any())).thenReturn(Future.successful(()))

        val application =
          applicationBuilder(Some(emptyUserAnswers))
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {

          val request = FakeRequest(GET, routes.KeepAliveController.keepAlive().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          verify(mockSessionRepository, times(1)).keepAlive(emptyUserAnswers.id)
        }
      }
    }

    "return OK" - {
      "the user has not answered any questions" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.keepAlive(any())).thenReturn(Future.successful(()))

        val application =
          applicationBuilder(None)
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {

          val request = FakeRequest(GET, routes.KeepAliveController.keepAlive().url)

          val result = route(application, request).value

          status(result) mustEqual OK
          verify(mockSessionRepository, times(1)).keepAlive(userAnswersId)
        }
      }
    }
  }
}
