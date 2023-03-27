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
import play.api.data.FormError
import play.api.test.FakeRequest
import views.html.TextAreaView

class TextAreaViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[TextAreaView]

    implicit val request = FakeRequest()

    "TextInputView" - {

      val form = data.Form(
        "value" -> text("text.error.required")
      )
      val invalidForm = form.withError(FormError("value", "error reason"))

      act.like(renderTitle(textAreaViewModelGen)(view(form, _), _.title.key))
      act.like(renderHeading(textAreaViewModelGen)(view(form, _), _.heading))
      act.like(renderErrors(textAreaViewModelGen)(view(invalidForm, _), "error reason"))
      act.like(renderTextArea(textAreaViewModelGen)(view(form, _), "value"))
      act.like(renderForm(textAreaViewModelGen)(view(form, _), _.onSubmit))
      act.like(renderSaveAndContinueButton(textAreaViewModelGen)(view(form, _)))
    }
  }
}
