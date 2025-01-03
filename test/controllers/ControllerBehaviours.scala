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
import services.SaveService
import queries.Settable
import play.api.mvc.{Call, Request}
import play.twirl.api.Html
import play.api.inject.bind
import config.Constants.PREPOPULATION_FLAG
import models.UserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import org.mockito.Mockito._
import play.api.libs.json.{JsPath, Writes}
import play.api.Application
import navigation.{FakeNavigator, Navigator}

import scala.concurrent.Future

trait ControllerBehaviours {
  _: ControllerBaseSpec =>

  import Behaviours._

  private def navigatorBindings(onwardRoute: Call): List[GuiceableModule] =
    List(
      bind[Navigator].qualifiedWith("root").toInstance(new FakeNavigator(onwardRoute)),
      bind[Navigator].qualifiedWith("non-sipp").toInstance(new FakeNavigator(onwardRoute))
    )

  def renderView(
    call: => Call,
    userAnswers: UserAnswers = defaultUserAnswers,
    pureUserAnswers: UserAnswers = defaultUserAnswers,
    optPreviousAnswers: Option[UserAnswers] = None,
    isPsa: Boolean = true
  )(
    view: Application => Request[_] => Html
  ): BehaviourTest =
    "return OK and the correct view".hasBehaviour {
      val appBuilder = applicationBuilder(
        userAnswers = Some(userAnswers),
        pureUserAnswers = Some(pureUserAnswers),
        previousUserAnswers = optPreviousAnswers,
        isPsa = isPsa
      )
      render(appBuilder, call)(view)
    }

  def renderViewWithPrePopSession(
    call: => Call,
    userAnswers: UserAnswers = defaultUserAnswers,
    pureUserAnswers: UserAnswers = defaultUserAnswers,
    optPreviousAnswers: Option[UserAnswers] = None,
    isPsa: Boolean = true
  )(
    view: Application => Request[_] => Html
  ): BehaviourTest =
    "return OK and the correct view with PrePop session".hasBehaviour {
      val appBuilder = applicationBuilder(
        userAnswers = Some(userAnswers),
        pureUserAnswers = Some(pureUserAnswers),
        previousUserAnswers = optPreviousAnswers,
        isPsa = isPsa
      )
      render(appBuilder, call, (PREPOPULATION_FLAG, "true"))(view)
    }

  def renderPrePopView[A: Writes](call: => Call, page: Settable[A], value: A)(
    view: Application => Request[_] => Html
  ): BehaviourTest =
    renderPrePopView(call, page, value, defaultUserAnswers)(view)

  def renderPrePopView[A: Writes](call: => Call, page: Settable[A], value: A, userAnswers: UserAnswers)(
    view: Application => Request[_] => Html
  ): BehaviourTest =
    "return OK and the correct pre-populated view for a GET".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers.set(page, value).success.value))
      render(appBuilder, call)(view)
    }

  def redirectWhenCacheEmpty(call: => Call, nextPage: => Call): BehaviourTest =
    s"redirect to $nextPage when cache empty".hasBehaviour {
      running(_ => applicationBuilder(userAnswers = Some(emptyUserAnswers))) { app =>
        val request = FakeRequest(call)
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe nextPage.url
      }
    }

  def journeyRecoveryPage(call: => Call, userAnswers: Option[UserAnswers]): BehaviourTest =
    s"must redirect to Journey Recovery if no existing data is found".hasBehaviour {
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {

        val result = route(application, FakeRequest(call)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  def journeyRecoveryPage(call: => Call): BehaviourTest =
    journeyRecoveryPage(call, None)

  def unauthorisedPage(call: => Call, userAnswers: Option[UserAnswers]): BehaviourTest =
    s"must redirect to unauthorised".hasBehaviour {
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {

        val result = route(application, FakeRequest(call)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad().url
      }
    }

  private def render(appBuilder: GuiceApplicationBuilder, call: => Call, session: (String, String)*)(
    view: Application => Request[_] => Html
  ): Unit =
    running(_ => appBuilder) { app =>
      val request = FakeRequest(call).withSession(session: _*)
      val result = route(app, request).value
      val expectedView = view(app)(request)

      status(result) shouldMatchTo OK
      val x = contentAsString(result)
      val y = expectedView.body
      x shouldMatchTo y
    }

  def invalidForm(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    s"return BAD_REQUEST for a POST with invalid form data ${form.toString()}".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers))

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)
        val result = route(app, request).value

        status(result) shouldMatchTo BAD_REQUEST
      }
    }

  def invalidForm(call: => Call, form: (String, String)*): BehaviourTest =
    invalidForm(call, defaultUserAnswers, form: _*)

  def redirectNextPage(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    s"redirect to the next page with form ${form.toList}".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers)).overrides(
        navigatorBindings(testOnwardRoute): _*
      )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url
      }
    }

  def redirectNextPage(call: => Call, form: (String, String)*): BehaviourTest =
    redirectNextPage(call, defaultUserAnswers, form: _*)

  def redirectToPage(call: => Call, page: => Call, form: (String, String)*): BehaviourTest =
    redirectToPage(call, page, defaultUserAnswers, form: _*)

  def redirectToPage(call: => Call, page: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    redirectToPage(call, page, userAnswers, emptyUserAnswers, form: _*)

  def redirectToPage(
    call: => Call,
    page: => Call,
    userAnswers: UserAnswers,
    previousUserAnswers: UserAnswers,
    form: (String, String)*
  ): BehaviourTest = redirectToPage(call, page, userAnswers, previousUserAnswers, None, form: _*)

  def redirectToPage(
    call: => Call,
    page: => Call,
    userAnswers: UserAnswers,
    previousUserAnswers: UserAnswers,
    mockSaveService: Option[SaveService],
    form: (String, String)*
  ): BehaviourTest =
    s"redirect to page with form $form".hasBehaviour {
      val appBuilder =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          previousUserAnswers = Some(previousUserAnswers),
          saveService = mockSaveService
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual page.url
      }
    }

  def redirectToPageWithPrePopSession(
    call: => Call,
    page: => Call,
    userAnswers: UserAnswers,
    previousUserAnswers: UserAnswers,
    mockSaveService: Option[SaveService],
    form: (String, String)*
  ): BehaviourTest =
    s"redirect to page with form $form when isPrePopulation = true".hasBehaviour {
      val appBuilder =
        applicationBuilder(
          userAnswers = Some(userAnswers),
          previousUserAnswers = Some(previousUserAnswers),
          saveService = mockSaveService
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call)
          .withFormUrlEncodedBody(form: _*)
          .withSession((PREPOPULATION_FLAG, "true"))

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual page.url
      }
    }

  def saveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    pureUserAnswers: Option[UserAnswers],
    expectedDataPath: Option[JsPath],
    expectations: UserAnswers => List[Boolean],
    form: (String, String)*
  ): BehaviourTest =
    s"save data and continue to next page with ${form.toList.toString()}".hasBehaviour {

      val saveService = mock[SaveService]
      val userDetailsCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(saveService.save(userDetailsCaptor.capture())(any(), any())).thenReturn(Future.successful(()))

      val appBuilder = applicationBuilder(Some(userAnswers), pureUserAnswers = pureUserAnswers)
        .overrides(
          bind[SaveService].toInstance(saveService)
        )
        .overrides(
          navigatorBindings(testOnwardRoute): _*
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

        verify(saveService, times(1)).save(any())(any(), any())
        if (expectedDataPath.nonEmpty) {
          val data = userDetailsCaptor.getValue.data.decryptedValue
          assert(expectedDataPath.get(data).nonEmpty)
        }

        expectations(userDetailsCaptor.getValue).foreach(e => assert(e))
      }
    }

  def saveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    expectedDataPath: Option[JsPath],
    form: (String, String)*
  ): BehaviourTest =
    saveAndContinue(call, userAnswers, None, expectedDataPath, _ => Nil, form: _*)

  def saveAndContinue(
    call: => Call,
    userAnswers: UserAnswers = defaultUserAnswers,
    pureUserAnswers: Option[UserAnswers] = None,
    expectations: UserAnswers => List[Boolean] = _ => Nil,
    form: List[(String, String)] = Nil
  ): BehaviourTest =
    saveAndContinue(call, userAnswers, pureUserAnswers, None, expectations, form: _*)

  def saveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    expectations: UserAnswers => List[Boolean]
  ): BehaviourTest =
    saveAndContinue(call, userAnswers, None, None, expectations, Nil: _*)

  def saveAndContinue(call: => Call, form: (String, String)*): BehaviourTest =
    saveAndContinue(call, defaultUserAnswers, None, defaultExpectedDataPath, _ => Nil, form: _*)

  def saveAndContinue(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    saveAndContinue(call, userAnswers, None, defaultExpectedDataPath, _ => Nil, form: _*)

  def saveAndContinue(call: => Call, jsPathOption: Option[JsPath], form: (String, String)*): BehaviourTest =
    saveAndContinue(call, defaultUserAnswers, None, jsPathOption, _ => Nil, form: _*)

  def continueNoSave(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    "continue to the next page without saving".hasBehaviour {

      val saveService = mock[SaveService]
      when(saveService.save(any())(any(), any())).thenReturn(Future.failed(new Exception("Unreachable code")))

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SaveService].toInstance(saveService)
        )
        .overrides(
          navigatorBindings(testOnwardRoute): _*
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

        verify(saveService, never).save(any())(any(), any())
      }
    }

  def continueNoSave(call: => Call, form: (String, String)*): BehaviourTest =
    continueNoSave(call, defaultUserAnswers, form: _*)

  def agreeAndContinue(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    agreeAndContinue(call, userAnswers, emptyUserAnswers)
  def agreeAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    previousUserAnswers: UserAnswers,
    form: (String, String)*
  ): BehaviourTest =
    "agree and continue to next page".hasBehaviour {

      val appBuilder =
        applicationBuilder(userAnswers = Some(userAnswers), previousUserAnswers = Some(previousUserAnswers))
          .overrides(
            navigatorBindings(testOnwardRoute): _*
          )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form: _*)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

      }
    }

  def agreeAndContinue(call: => Call): BehaviourTest =
    agreeAndContinue(call, defaultUserAnswers)

  def continue(call: => Call, userAnswers: UserAnswers): BehaviourTest =
    "continue to next page".hasBehaviour {

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          navigatorBindings(testOnwardRoute): _*
        )

      running(_ => appBuilder) { app =>
        val result = route(app, FakeRequest(call)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

      }
    }

  def continue(call: => Call): BehaviourTest =
    continue(call, defaultUserAnswers)
}
