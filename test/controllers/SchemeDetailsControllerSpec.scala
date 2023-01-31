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

import models.{Establisher, EstablisherKind}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import viewmodels.ComplexMessageElement.Message
import viewmodels.Delimiter
import viewmodels.DisplayMessage.{ComplexMessage, SimpleMessage}
import views.html.ContentTablePageView

class SchemeDetailsControllerSpec extends ControllerBaseSpec {

  def onwardRoute = Call("GET", "/foo")

  val srn = srnGen.sample.value

  lazy val onPageLoad = routes.SchemeDetailsController.onPageLoad(srn).url
  lazy val onSubmit = routes.SchemeDetailsController.onSubmit(srn).url

  lazy val app = applicationBuilder(Some(userAnswers))

  private val schemeDetailsTwoEstablishers = defaultSchemeDetails.copy(establishers =
    List(
      Establisher("testFirstName testLastName", EstablisherKind.Partnership),
      Establisher("testFirstName2 testLastName2", EstablisherKind.Partnership)
    )
  )

  private val schemeDetailsThreeEstablishers = defaultSchemeDetails.copy(establishers =
    List(
      Establisher("testFirstName testLastName", EstablisherKind.Partnership),
      Establisher("testFirstName2 testLastName2", EstablisherKind.Partnership),
      Establisher("testFirstName3 testLastName3", EstablisherKind.Partnership)
    )
  )

  "SchemeDetailsController" should {

    List(
      ("a single establisher", defaultSchemeDetails, 4),
      ("two establishers", schemeDetailsTwoEstablishers, 5)
    ).foreach { case (testName, schemeDetails, numRows) =>
      s"build the correct view model with $testName" in running(_ => app) { implicit app =>

        val controller = injected[SchemeDetailsController]
        val viewModel = controller.viewModel(srn, schemeDetails)

        viewModel.rows.size mustEqual numRows
      }
    }

    "build the correct view model with three establishers" in running(_ => app){ implicit app =>

      val controller = injected[SchemeDetailsController]
      val viewModel = controller.viewModel(srn, schemeDetailsThreeEstablishers)

      viewModel.rows.size mustEqual 5
      viewModel.rows.last mustEqual
        SimpleMessage("schemeDetails.row5") ->
          ComplexMessage(List(Message("testFirstName2 testLastName2"), Message("testFirstName3 testLastName3")), Delimiter.Newline)
    }

    "return OK and the correct view for a GET" when {
      List(
        ("a single establisher", defaultSchemeDetails),
        ("two establishers", schemeDetailsTwoEstablishers),
        ("three establishers", schemeDetailsThreeEstablishers)
      ).foreach { case (testName, schemeDetails) =>
        s"scheme details contains $testName" in {

          val app = applicationBuilder(Some(userAnswers), schemeDetails)

          running(_ => app) { implicit app =>

            val view = injected[ContentTablePageView]
            val controller = injected[SchemeDetailsController]
            val request = FakeRequest(GET, onPageLoad)

            val result = route(app, request).value
            val expectedView = view(controller.viewModel(srn, schemeDetails))(request, messages(app))

            status(result) mustEqual OK
            contentAsString(result) mustEqual expectedView.toString
          }
        }
      }
    }

    "redirect to the next page" in {

      val fakeNavigatorApplication =
        applicationBuilder(Some(userAnswers))
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
