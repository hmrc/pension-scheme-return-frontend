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
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class JourneyRecoveryControllerSpec extends ControllerBaseSpec {

  "JourneyRecovery Controller" - {

    "return OK and the continue view" - {
      "a relative continue Url is supplied" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("/foo")
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val continueView = application.injector.instanceOf[JourneyRecoveryContinueView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual continueView(continueUrl.unsafeValue)(request, createMessages(application)).toString
        }
      }
    }

    "return OK and the start again view" - {
      "an absolute continue Url is supplied" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("https://foo.com")
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual startAgainView()(request, createMessages(application)).toString
        }
      }
    }

    "return OK and the start again view" - {
      "no continue Url is supplied" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual startAgainView()(request, createMessages(application)).toString
        }
      }
    }
  }
}
