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

package views

import base.SpecBase
import forms.PensionSchemeForm
import org.jsoup.Jsoup
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import viewmodels.models.PensionSchemeViewModel
import viewmodels.DisplayMessage
import views.html.PensionSchemeView
import org.jsoup.Jsoup
import play.api.mvc.Call


class PensionSchemeViewSpec extends SpecBase {

  val application = applicationBuilder(userAnswers = None).build()

  running(application) {
    val view = application.injector.instanceOf[PensionSchemeView]

    implicit val request = FakeRequest()
    implicit val mess = messages(application)

    "PensionsSchemeView" - {

      "view should render correctly" in {

        val form = new PensionSchemeForm()
        val viewModel = PensionSchemeViewModel(
          title = DisplayMessage("testTitle"),
          heading = DisplayMessage("testHeading"),
          Call("GET", "/value")
        )

        val result = view(form("value"), viewModel)
        Jsoup.parse(result.toString()).body().toString must include("Continue")
      }

      "view should fail to render the error page" in {

        val form = new PensionSchemeForm()
        val viewModel = PensionSchemeViewModel(
          title = DisplayMessage("testTitle"),
          heading = DisplayMessage("testHeading"),
          Call("GET", "/value")
        )

        val failingForm = form("value").bind(Map("failing" -> "data"))

        val result = view(failingForm, viewModel)
        Jsoup.parse(result.toString()).body().getElementById("error-summary-title").text() mustBe "There is a problem"
      }
    }
  }
}