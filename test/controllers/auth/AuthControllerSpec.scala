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

package controllers.auth

import config.FrontendAppConfig
import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.inject.bind
import play.api.test.FakeRequest
import repositories.SessionRepository

import scala.concurrent.Future

class AuthControllerSpec extends ControllerBaseSpec {

  "signOut" - {

    "clear user answers and redirect to sign out, specifying the exit survey as the continue URL" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value

        val expectedRedirectUrl = appConfig.exitSurveyUrl

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
        verify(mockSessionRepository, times(1)).clear(eqTo(userAnswersId))
      }
    }
  }

  "signOutNoSurvey" - {

    "clear users answers and redirect to sign out, specifying SignedOut as the continue URL" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {

        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val request = FakeRequest(GET, routes.AuthController.signOutNoSurvey().url)

        val result = route(application, request).value

        val expectedRedirectUrl = s"${appConfig.urls.signOutNoSurveyUrl}"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
        verify(mockSessionRepository, times(1)).clear(eqTo(userAnswersId))
      }
    }
  }
}
