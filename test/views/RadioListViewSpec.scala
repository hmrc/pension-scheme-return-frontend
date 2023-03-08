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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.FakeRequest
import utils.BaseSpec
import views.html.RadioListView

class RadioListViewSpec extends BaseSpec with ScalaCheckPropertyChecks with HtmlHelper {

  implicit val enum = Enumerable(
    ("1", 1),
    ("2", 2),
    ("3", 3),
  )

  runningApplication { implicit app =>

    implicit val request = FakeRequest()

    val view = injected[RadioListView]

    "RadioListView" should {

      val requiredKey = "radio.error.required"

      val radioListForm = new RadioListFormProvider().apply[Int](requiredKey)

      "have a title" in {

        forAll(radioListViewModelGen) { viewmodel =>

          title(view(radioListForm, viewmodel)) must startWith(viewmodel.title.toMessage)
        }
      }

      "have a heading" in {

        forAll(radioListViewModelGen) { viewmodel =>

          h1(view(radioListForm, viewmodel)) mustBe viewmodel.heading.toMessage
        }
      }

      "have descriptions" in {

        forAll(radioListViewModelGen) { viewmodel =>

          p(view(radioListForm, viewmodel)) mustBe viewmodel.description.map(_.toMessage)
        }
      }

      "have listed content" in {

        forAll(radioListViewModelGen) { viewmodel =>

          li(view(radioListForm, viewmodel)) mustBe viewmodel.listedContent.map(_.toMessage)
        }
      }

      "have legend when present" in {

        forAll(radioListViewModelGen) { viewmodel =>

          whenever(viewmodel.legend.nonEmpty) {
            legend(view(radioListForm, viewmodel)) mustBe List(viewmodel.legend.value.toMessage)
          }
        }
      }

      "no have a legend when not present" in {

        forAll(radioListViewModelGen) { viewmodel =>

          legend(view(radioListForm, viewmodel.copy(legend = None))) mustBe Nil
        }
      }

      "have radio list values" in {

        forAll(radioListViewModelGen) { viewmodel =>

          radios(view(radioListForm, viewmodel)).map(_.value) mustBe viewmodel.items.map(_.value)
        }
      }

      "have radio button labels" in {

        forAll(radioListViewModelGen) { viewmodel =>

          radios(view(radioListForm, viewmodel)).map(_.label) mustBe viewmodel.items.map(_.content.key)
        }
      }

      "have form" in {

        forAll(radioListViewModelGen) { viewmodel =>

          form(view(radioListForm, viewmodel)).method mustBe viewmodel.onSubmit.method
          form(view(radioListForm, viewmodel)).action mustBe viewmodel.onSubmit.url
        }
      }

      "have error summary" in {

        forAll(radioListViewModelGen) { viewmodel =>

          val invalidForm = radioListForm.bind(Map("value" -> "4"))
          errorSummary(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }

      "have error message" in {

        forAll(radioListViewModelGen) { viewmodel =>

          val invalidForm = radioListForm.bind(Map("value" -> "4"))
          errorMessage(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }
    }
  }
}
