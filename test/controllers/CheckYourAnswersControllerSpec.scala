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

package controllers

import models.{SchemeDetails, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import views.html.CheckYourAnswersView


class CheckYourAnswersControllerSpec extends ControllerBaseSpec {

  def onwardRoute = Call("GET", "/foo")

  val srn = srnGen.sample.value

  lazy val onPageLoad = routes.CheckYourAnswersController.onPageLoad(srn).url
  lazy val onSubmit = routes.CheckYourAnswersController.onSubmit(srn).url


  "SchemeBankAccountCheckYourAnswersController.onPageLoad" should {

    "return OK and the correct view for a GET" in {

      running(_ => applicationBuilder(userAnswers = Some(emptyUserAnswers))) { implicit app =>

        val view = injected[CheckYourAnswersView]
        val request = FakeRequest(GET, onPageLoad)

        val result = route(app, request).value
        val expectedView = view(SchemeBankAccountCheckYourAnswersController.viewModel(srn))(request, messages(app))

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {

        val request = FakeRequest(GET, onPageLoad)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "SchemeBankAccountCheckYourAnswers.onSubmit" should {

    "redirect to the next page" in {

      val fakeNavigatorApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
        )

      running(_ => fakeNavigatorApplication) { app =>

        val request = FakeRequest(GET, onSubmit)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {

        val request = FakeRequest(GET, onSubmit)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "SchemeBankAccountCheckYourAnswers.viewModel" should {


  }
}

