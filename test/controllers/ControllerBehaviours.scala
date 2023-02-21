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

import models.UserAnswers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Writes
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.Html
import queries.Settable

trait ControllerBehaviours {
  _: ControllerBaseSpec =>

  def renderView(onPageLoadUrl: String, userAnswers: UserAnswers = defaultUserAnswers)(view: Application => Request[_] => Html): Unit =
    "return OK and the correct view for a GET" in {
      val appBuilder = applicationBuilder(Some(userAnswers))
      render(appBuilder, onPageLoadUrl)(view)
    }

  def renderPrePopView[A: Writes](onPageLoadUrl: String, page: Settable[A], value: A)(view: Application => Request[_] => Html): Unit =
    "return OK and the correct pre-populated view for a GET" in {
      val appBuilder = applicationBuilder(Some(defaultUserAnswers.set(page, value).success.value))
      render(appBuilder, onPageLoadUrl)(view)
    }

  private def render(appBuilder: GuiceApplicationBuilder, onPageLoadUrl: String)(view: Application => Request[_] => Html): Unit =
    running(_ => appBuilder) { app =>
      val request = FakeRequest(GET, onPageLoadUrl)
      val result = route(app, request).value
      val expectedView = view(app)(request)

      status(result) mustEqual OK
      contentAsString(result) mustEqual expectedView.toString
    }

  def invalidForm(onSubmitUrl: String, form: (String, String)*): Unit =
    "return BAD_REQUEST for a POST with invalid form data" in {
      val appBuilder = applicationBuilder(Some(defaultUserAnswers))
      running(_ => appBuilder) { app =>
        val request = FakeRequest(POST, onSubmitUrl).withFormUrlEncodedBody(form: _*)
        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

  def redirectNextPage(onSubmitUrl: String, redirectUrl: String, httpVerb: String, userAnswers: UserAnswers, form: (String, String)*): Unit =
    s"redirect to $redirectUrl on $httpVerb when submitting ${form.toList}" in {
      val appBuilder = applicationBuilder(Some(userAnswers))
      running(_ => appBuilder) { app =>
        val request = FakeRequest(httpVerb, onSubmitUrl).withFormUrlEncodedBody(form: _*)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual redirectUrl
      }
    }

  def redirectNextPage(onSubmitUrl: String, redirectUrl: String, userAnswers: UserAnswers, form: (String, String)*): Unit =
    redirectNextPage(onSubmitUrl, redirectUrl, POST, userAnswers, form: _*)

  def redirectNextPage(onSubmitUrl: String, redirectUrl: String, form: (String, String)*): Unit =
    redirectNextPage(onSubmitUrl, redirectUrl, POST, defaultUserAnswers, form: _*)

  def redirectOnPageLoad(onPageLoadUrl: String, redirectUrl: String): Unit =
    redirectNextPage(onPageLoadUrl, redirectUrl, GET, defaultUserAnswers, Nil: _*)
}
