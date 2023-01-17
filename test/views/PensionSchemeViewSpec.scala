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


  running(_ => applicationBuilder()) { implicit app =>

    val view = app.injector.instanceOf[PensionSchemeView]

    implicit val request = FakeRequest()
    implicit val mess = messages(app)

    "test 1" in {

      val form = new PensionSchemeForm()
      val viewModel = PensionSchemeViewModel(
        DisplayMessage("value 1"),
        DisplayMessage("header"),
        //DisplayMessage("value 3")
      Call("GET", "/value")
      )

   val failingForm = form("value")

      val result = view(failingForm, viewModel)
      Jsoup.parse(result.toString()).body(). toString must include("header")
//      Jsoup.parse(result.toString()).body().must include("no bank name")
    }
  }

}