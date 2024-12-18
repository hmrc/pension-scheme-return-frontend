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

package views

import forms.mappings.Mappings
import views.html.TextInputView
import play.api.data
import viewmodels.models.TextInputViewModel

class TextInputViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[TextInputView]

    val textInputForm = data.Form(
      "value" -> text("text.error.required")
    )

    val invalidForm = textInputForm.bind(Map("value" -> ""))

    val viewModelGen = formPageViewModelGen[TextInputViewModel]

    "TextInputView" - {

      act.like(renderTitle(viewModelGen)(view(textInputForm, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(textInputForm, _), _.heading))
      act.like(renderInputWithH1Label(viewModelGen)("value", view(textInputForm, _), _.heading, _.page.label))
      act.like(renderErrors(viewModelGen)(view(invalidForm, _), _ => "text.error.required"))
      act.like(renderForm(viewModelGen)(view(textInputForm, _), _.onSubmit))
      act.like(renderButtonText(viewModelGen)(view(textInputForm, _), _.buttonText))
    }
  }
}
