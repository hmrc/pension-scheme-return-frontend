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

import forms.RadioListFormProvider
import models.Enumerable
import play.api.test.FakeRequest
import viewmodels.models.RadioListViewModel
import views.html.RadioListView

class RadioListViewSpec extends ViewSpec {

  implicit val enumerable = Enumerable(
    ("1", 1),
    ("2", 2),
    ("3", 3)
  )

  runningApplication { implicit app =>
    implicit val request = FakeRequest()

    val view = injected[RadioListView]

    val viewModelGen = pageViewModelGen[RadioListViewModel]

    "RadioListView" - {

      val requiredKey = "radio.error.required"

      val radioListForm = new RadioListFormProvider().apply[Int](requiredKey)
      val invalidForm = radioListForm.bind(Map("value" -> "4"))

      act.like(renderTitle(viewModelGen)(view(radioListForm, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(radioListForm, _), _.heading))
      act.like(renderDescription(viewModelGen)(view(radioListForm, _), _.description))
      act.like(renderButtonText(viewModelGen)(view(radioListForm, _), _.buttonText))
      act.like(renderForm(viewModelGen)(view(radioListForm, _), _.onSubmit))
      act.like(renderErrors(viewModelGen)(view(invalidForm, _), _ => requiredKey))

      "have legend when present" in {

        forAll(viewModelGen) { viewmodel =>
          whenever(viewmodel.page.legend.nonEmpty) {
            legend(view(radioListForm, viewmodel)) mustBe List(viewmodel.page.legend.value.toMessage)
          }
        }
      }

      "no have a legend when not present" in {

        forAll(viewModelGen) { viewmodel =>
          legend(view(radioListForm, viewmodel.copy(page = viewmodel.page.copy(legend = None)))) mustBe Nil
        }
      }

      "have radio list values" in {

        forAll(viewModelGen) { viewmodel =>
          radios(view(radioListForm, viewmodel)).map(_.value) mustBe viewmodel.page.items.map(_.value)
        }
      }

      "have radio button labels" in {

        forAll(viewModelGen) { viewmodel =>
          radios(view(radioListForm, viewmodel)).map(_.label) mustBe viewmodel.page.items.map(_.content.key)
        }
      }
    }
  }
}
