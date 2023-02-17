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

import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import views.html.ContentPageView

class StartPageControllerSpec extends ControllerBaseSpec {

  def onwardRoute = Call("GET", "/foo")

  val srn = srnGen.sample.value

  lazy val onPageLoad = routes.StartPageController.onPageLoad(srn).url
  lazy val onSubmit = routes.StartPageController.onSubmit(srn).url

  "StartPageController" should {

    "return OK and the correct view for a GET" in runningApplication { implicit app =>

      val view = injected[ContentPageView]
      val request = FakeRequest(GET, onPageLoad)

      val result = route(app, request).value
      val expectedView = view(StartPageController.viewModel(srn))(request, messages(app))

      status(result) mustEqual OK
      contentAsString(result) mustEqual expectedView.toString
    }

    "redirect to the next page" in {

      val fakeNavigatorApplication =
        applicationBuilder()
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
  }
}