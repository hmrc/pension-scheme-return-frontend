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
import services.PsrRetrievalService
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import play.api.mvc.Call
import play.api.inject.bind
import views.html.ContentPageView
import navigation.{FakeNavigator, Navigator}
import models.{CheckMode, SchemeMemberNumbers}
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.reset

class WhatYouWillNeedControllerSpec extends ControllerBaseSpec {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val onPageLoad: String = routes.WhatYouWillNeedController.onPageLoad(srn, fbNumber, "", "").url
  lazy val onSubmit: String = routes.WhatYouWillNeedController.onSubmit(srn, fbNumber, "", "").url

  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrRetrievalService)

  private val dashboardUrlPsa =
    s"http://localhost:8204/manage-pension-schemes/pension-scheme-summary/${srn.value}"

  private val dashboardUrlPsp =
    s"http://localhost:8204/manage-pension-schemes/${srn.value}/dashboard/pension-scheme-details"

  "WhatYouWillNeedController" - {

    "return OK and the correct view for a GET (with PSA breadcrumb link)" in runningApplication { implicit app =>
      val view = injected[ContentPageView]
      val request = FakeRequest(GET, onPageLoad)
      val result = route(app, request).value
      val expectedView = view(WhatYouWillNeedController.viewModel(srn, fbNumber, "", "", schemeName, dashboardUrlPsa))(
        request,
        createMessages(app)
      )

      status(result) mustEqual OK
      contentAsString(result) mustEqual expectedView.toString
    }

    "return OK and the correct view for a GET (with PSP breadcrumb link)" in {
      val application = applicationBuilder(isPsa = false).build()

      running(application) {

        val view = application.injector.instanceOf[ContentPageView]
        val request = FakeRequest(GET, onPageLoad)
        val result = route(application, request).value
        val expectedView =
          view(WhatYouWillNeedController.viewModel(srn, fbNumber, "", "", schemeName, dashboardUrlPsp))(
            request,
            createMessages(application)
          )

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedView.toString
      }
    }

    "redirect to the next page" in {
      val fakeNavigatorApplication =
        applicationBuilder()
          .overrides(
            bind[Navigator].qualifiedWith("root").toInstance(new FakeNavigator(onwardRoute))
          )

      running(_ => fakeNavigatorApplication) { app =>
        val request = FakeRequest(GET, onSubmit)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "redirect to the next page more than 100 members" in {
      val pensionSchemeId = pensionSchemeIdGen.sample.value
      val ua = defaultUserAnswers.unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(50, 60, 70))
      val fakeNavigatorApplication =
        applicationBuilder(userAnswers = Some(ua))
          .overrides(
            bind[Navigator].qualifiedWith("root").toInstance(new FakeNavigator(onwardRoute))
          )

      running(_ => fakeNavigatorApplication) { app =>
        val request = FakeRequest(GET, onSubmit)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
          .onPageLoad(srn, CheckMode)
          .url
      }
    }
  }
}
