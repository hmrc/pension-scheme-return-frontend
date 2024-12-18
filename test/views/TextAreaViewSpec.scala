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
import views.html.TextAreaView
import play.api.data
import viewmodels.models.TextAreaViewModel
import play.api.data.FormError

class TextAreaViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[TextAreaView]

    val viewModelGen = formPageViewModelGen[TextAreaViewModel]

    "TextInputView" - {

      val form = data.Form(
        "value" -> text("text.error.required")
      )
      val invalidForm = form.withError(FormError("value", "error reason"))

      act.like(renderTitle(viewModelGen)(view(form, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(form, _), _.heading))
      act.like(renderErrors(viewModelGen)(view(invalidForm, _), _ => "error reason"))
      act.like(renderTextArea(viewModelGen)(view(form, _), "value"))
      act.like(renderForm(viewModelGen)(view(form, _), _.onSubmit))
      act.like(renderButtonText(viewModelGen)(view(form, _), _.buttonText))
    }
  }
}
