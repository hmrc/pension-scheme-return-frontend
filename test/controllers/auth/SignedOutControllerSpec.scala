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

package controllers.auth

import play.api.test.FakeRequest
import views.html.auth.SignedOutView
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class SignedOutControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  "SignedOut Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SignedOutController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignedOutView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(using request, createMessages(using application)).toString
      }
    }
  }
}
