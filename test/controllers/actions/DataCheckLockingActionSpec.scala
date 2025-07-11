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
import config.FrontendAppConfig
import handlers.PsrLockedException
import org.scalatest.time.Span
import repositories.SessionRepository
import models.UserAnswers
import models.requests.AllowedAccessRequest
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class DataCheckLockingActionSpec extends BaseSpec {

  class Harness(
    request: AllowedAccessRequest[AnyContentAsEmpty.type],
    sessionRepository: SessionRepository,
    appConfig: FrontendAppConfig
  )(implicit
    ec: ExecutionContext
  ) extends DataCheckLockingActionImpl(sessionRepository, appConfig)(using ec) {
    def callTransform(): Future[AllowedAccessRequest[AnyContentAsEmpty.type]] =
      transform(request)
  }

  val request: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  val userAnswers: UserAnswers = arbitraryUserData.arbitrary.sample.value

  "Data check locking action" - {

    "when locking is disabled" - {
      "should not check data in sessionRepository" in {

        val sessionRepository = mock[SessionRepository]
        val mockConfig = mock[FrontendAppConfig]
        when(mockConfig.lockingEnabled).thenReturn(false)

        val action = new Harness(request, sessionRepository, mockConfig)

        action.callTransform().futureValue
        verify(sessionRepository, never()).getBySrnAndIdNotEqual(any(), any())
      }
    }

    "when locking is enabled" - {
      "and data does not exist in sessionRepository" - {
        "should not throw exception" in {
          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.getBySrnAndIdNotEqual(any(), any())).thenReturn(Future.successful(None))
          val mockConfig = mock[FrontendAppConfig]
          when(mockConfig.lockingEnabled).thenReturn(true)

          val action = new Harness(request, sessionRepository, mockConfig)

          action.callTransform().futureValue
          verify(sessionRepository, times(1)).getBySrnAndIdNotEqual(any(), any())
        }
      }
      "and data exist in sessionRepository" - {
        "should throw exception" in {
          val sessionRepository = mock[SessionRepository]
          when(sessionRepository.getBySrnAndIdNotEqual(any(), any())).thenReturn(Future.successful(Some(userAnswers)))
          val mockConfig = mock[FrontendAppConfig]
          when(mockConfig.lockingEnabled).thenReturn(true)

          val action = new Harness(request, sessionRepository, mockConfig)

          assertThrows[PsrLockedException](
            Await.result(action.callTransform(), patienceConfig.timeout)
          )
          verify(sessionRepository, times(1)).getBySrnAndIdNotEqual(any(), any())
        }
      }
    }
  }
}
