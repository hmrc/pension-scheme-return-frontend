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

import models.{CheckMode, SchemeMemberNumbers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.FakeRequest
import services.PsrRetrievalService
import views.html.ContentPageView

import scala.concurrent.Future

class WhatYouWillNeedPageControllerSpec extends ControllerBaseSpec {

  def onwardRoute = Call("GET", "/foo")

  lazy val onPageLoad = routes.WhatYouWillNeedController.onPageLoad(srn).url
  lazy val onSubmit = routes.WhatYouWillNeedController.onSubmit(srn).url

  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrRetrievalService)

  "WhatYouWillNeedController" - {

    "return OK and the correct view for a GET" in runningApplication { implicit app =>
      val view = injected[ContentPageView]
      val request = FakeRequest(GET, onPageLoad)

      val result = route(app, request).value
      val expectedView = view(WhatYouWillNeedController.viewModel(srn))(request, createMessages(app))

      status(result) mustEqual OK
      contentAsString(result) mustEqual expectedView.toString
    }

    "redirect to the next page" in {
      when(mockPsrRetrievalService.getStandardPsrDetails(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.successful(defaultUserAnswers)
      )
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

      when(mockPsrRetrievalService.getStandardPsrDetails(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.successful(
          defaultUserAnswers
            .unsafeSet(HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(50, 60, 70))
        )
      )
      val fakeNavigatorApplication =
        applicationBuilder()
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
