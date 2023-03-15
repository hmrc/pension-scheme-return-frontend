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
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Writes
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import queries.Settable
import services.SaveService

import scala.concurrent.Future

trait ControllerBehaviours {
  _: ControllerBaseSpec =>

  def renderView(call: => Call, userAnswers: UserAnswers = defaultUserAnswers)(
    view: Application => Request[_] => Html
  ): Unit =
    "return OK and the correct view" in {
      val appBuilder = applicationBuilder(Some(userAnswers))
      render(appBuilder, call)(view)
    }

  def renderPrePopView[A: Writes](call: => Call, page: Settable[A], value: A)(
    view: Application => Request[_] => Html
  ): Unit =
    renderPrePopView(call, page, value, defaultUserAnswers)(view)

  def renderPrePopView[A: Writes](call: => Call, page: Settable[A], value: A, userAnswers: UserAnswers)(
    view: Application => Request[_] => Html
  ): Unit =
    "return OK and the correct pre-populated view for a GET" in {
      val appBuilder = applicationBuilder(Some(userAnswers.set(page, value).success.value))
      render(appBuilder, call)(view)
    }

  def redirectWhenCacheEmpty(call: => Call, nextPage: => Call): Unit =
    s"redirect to $nextPage when cache empty" in {
      running(_ => applicationBuilder(userAnswers = Some(emptyUserAnswers))) { app =>
        val request = FakeRequest(call)
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe nextPage.url
      }
    }

  def journeyRecoveryPage(name: String, call: => Call): Unit =
    s"$name must redirect to Journey Recovery if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {

        val result = route(application, FakeRequest(call)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  private def render(appBuilder: GuiceApplicationBuilder, call: => Call)(
    view: Application => Request[_] => Html
  ): Unit =
    running(_ => appBuilder) { app =>
      val request = FakeRequest(call)
      val result = route(app, request).value
      val expectedView = view(app)(request)

      status(result) mustEqual OK
      contentAsString(result) mustEqual expectedView.body
    }

  def invalidForm(call: => Call, userAnswers: UserAnswers, form: (String, String)*): Unit =
    s"return BAD_REQUEST for a POST with invalid form data ${form.toString()}" in {
      val appBuilder = applicationBuilder(Some(userAnswers))

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)
        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

  def invalidForm(call: => Call, form: (String, String)*): Unit =
    invalidForm(call, defaultUserAnswers, form: _*)

  def redirectNextPage(call: => Call, userAnswers: UserAnswers, form: (String, String)*): Unit =
    s"redirect to the next page with form ${form.toList}" in {

      val appBuilder = applicationBuilder(Some(userAnswers)).overrides(
        bind[Navigator].toInstance(new FakeNavigator(testOnwardRoute))
      )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url
      }
    }

  def redirectNextPage(call: => Call, form: (String, String)*): Unit =
    redirectNextPage(call, defaultUserAnswers, form: _*)

  def redirectToPage(call: => Call, page: => Call, userAnswers: UserAnswers, form: (String, String)*): Unit =
    s"redirect to page with form $form" in {
      val appBuilder = applicationBuilder(Some(userAnswers))

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual page.url
      }
    }

  def redirectToPage(call: => Call, page: => Call, form: (String, String)*): Unit =
    redirectToPage(call, page, defaultUserAnswers, form: _*)

  def saveAndContinue(call: => Call, userAnswers: UserAnswers, form: (String, String)*): Unit =
    "save data and continue to next page" in {

      val saveService = mock[SaveService]
      when(saveService.save(any())(any(), any())).thenReturn(Future.successful(()))

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SaveService].toInstance(saveService),
          bind[Navigator].toInstance(new FakeNavigator(testOnwardRoute))
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

        verify(saveService, times(1)).save(any())(any(), any())
      }
    }

  def saveAndContinue(call: => Call, form: (String, String)*): Unit =
    saveAndContinue(call, defaultUserAnswers, form: _*)

  def continueNoSave(call: => Call, userAnswers: UserAnswers, form: (String, String)*): Unit =
    "continue to the next page without saving" in {
      val saveService = mock[SaveService]
      when(saveService.save(any())(any(), any())).thenReturn(Future.failed(new Exception("Unreachable code")))

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SaveService].toInstance(saveService),
          bind[Navigator].toInstance(new FakeNavigator(testOnwardRoute))
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

        verify(saveService, never).save(any())(any(), any())
      }
    }

  def continueNoSave(call: => Call, form: (String, String)*): Unit =
    continueNoSave(call, defaultUserAnswers, form: _*)

  def agreeAndContinue(call: => Call, userAnswers: UserAnswers): Unit =
    "agree and continue to next page" in {

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(testOnwardRoute))
        )

      running(_ => appBuilder) { app =>
        val result = route(app, FakeRequest(call)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

      }
    }
  def agreeAndContinue(call: => Call): Unit =
    agreeAndContinue(call, defaultUserAnswers)
}
