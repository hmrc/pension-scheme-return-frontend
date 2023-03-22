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

import forms.mappings.Mappings
import play.api.data
import play.api.test.FakeRequest
import views.html.TextInputView

class TextInputViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[TextInputView]

    val textInputForm = data.Form(
      "value" -> text("text.error.required")
    )

    val invalidForm = textInputForm.bind(Map("value" -> ""))

    implicit val request = FakeRequest()

    "TextInputView" should {

      act.like(renderTitle(textInputViewModelGen)(view(textInputForm, _), _.title.key))
      act.like(renderHeading(textInputViewModelGen)(view(textInputForm, _), _.heading))
      act.like(renderInputWithH1Label(textInputViewModelGen)("value", view(textInputForm, _), _.heading, _.label))
      act.like(renderErrors(textInputViewModelGen)(view(invalidForm, _), "text.error.required"))
      act.like(renderForm(textInputViewModelGen)(view(textInputForm, _), _.onSubmit))
      act.like(renderSaveAndContinueButton(textInputViewModelGen)(view(textInputForm, _)))
    }
  }
}
