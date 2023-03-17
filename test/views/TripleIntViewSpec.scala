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
import play.api.data.Forms.mapping
import play.api.test.FakeRequest
import views.html.{TextInputView, TripleIntView}

class TripleIntViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[TripleIntView]

    val tripleIntForm = data.Form(
      mapping(
        "value.1" -> int("value.1.error.required"),
        "value.2" -> int(),
        "value.3" -> int()
      )((a, b, c) => (a, b, c))(Some(_))
    )

    val invalidForm = tripleIntForm.bind(Map("value.1" -> ""))

    implicit val request = FakeRequest()

    "TextInputView" should {

      behave.like(renderTitle(tripleIntViewModelGen)(view(tripleIntForm, _), _.title.key))
      behave.like(renderHeading(tripleIntViewModelGen)(view(tripleIntForm, _), _.heading))
      behave.like(renderInputWithLabel(tripleIntViewModelGen)("value.1", view(tripleIntForm, _), _.field1Label))
      behave.like(renderInputWithLabel(tripleIntViewModelGen)("value.2", view(tripleIntForm, _), _.field2Label))
      behave.like(renderInputWithLabel(tripleIntViewModelGen)("value.3", view(tripleIntForm, _), _.field3Label))
      behave.like(renderErrors(tripleIntViewModelGen)(view(invalidForm, _), "value.1.error.required"))
      behave.like(renderForm(tripleIntViewModelGen)(view(tripleIntForm, _), _.onSubmit))
      behave.like(renderSaveAndContinueButton(tripleIntViewModelGen)(view(tripleIntForm, _)))
    }
  }
}
