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

import org.jsoup.Jsoup
import views.html.YesNoPageView
import forms.YesNoPageFormProvider
import viewmodels.models.YesNoPageViewModel

class YesNoPageViewSpec extends ViewSpec with ViewBehaviours {

  runningApplication { implicit app =>
    val view = injected[YesNoPageView]

    val viewModelGen = formPageViewModelGen[YesNoPageViewModel]

    "YesNoPageView" - {

      val requiredKey = "required"
      val invalidKey = "invalid"

      val yesNoForm = new YesNoPageFormProvider()(requiredKey, invalidKey)
      val invalidForm = yesNoForm.bind(Map("value" -> ""))

      act.like(renderTitle(viewModelGen)(view(yesNoForm, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(yesNoForm, _), _.heading))
      act.like(renderDescription(viewModelGen)(view(yesNoForm, _), _.description))
      act.like(renderButtonText(viewModelGen)(view(yesNoForm, _), _.buttonText))
      act.like(renderForm(viewModelGen)(view(yesNoForm, _), _.onSubmit))
      act.like(renderErrors(viewModelGen)(view(invalidForm, _), _ => requiredKey))
      act.like(renderButtonWithPreventDoubleClick(viewModelGen)(view(yesNoForm, _)))

      "have a legend" in {

        forAll(viewModelGen) { viewmodel =>
          whenever(viewmodel.page.legend.nonEmpty) {

            legend(view(yesNoForm, viewmodel)) must contain(viewmodel.page.legend.map(_.toMessage).value)
          }
        }
      }

      "have radio button values" in {

        forAll(viewModelGen) { viewmodel =>
          radios(view(yesNoForm, viewmodel)).map(_.value) mustBe List("true", "false")
        }
      }

      "have radio button labels" - {

        "yes" in {
          forAll(viewModelGen) { viewmodel =>
            whenever(viewmodel.page.yes.nonEmpty) {
              radios(view(yesNoForm, viewmodel))
                .map(_.label) must contain(viewmodel.page.yes.value.toMessage)
            }
          }
        }

        "no" in {
          forAll(viewModelGen) { viewmodel =>
            whenever(viewmodel.page.no.nonEmpty) {
              radios(view(yesNoForm, viewmodel))
                .map(_.label) must contain(viewmodel.page.no.value.toMessage)
            }
          }
        }
      }

      "have error summary" in {

        forAll(viewModelGen) { viewmodel =>
          val invalidForm = yesNoForm.bind(Map("value" -> ""))
          errorSummary(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }

      "have error message" in {

        forAll(viewModelGen) { viewmodel =>
          val invalidForm = yesNoForm.bind(Map("value" -> ""))
          errorMessage(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }

      "have a hint text when provided" in {

        forAll(viewModelGen) { viewModel =>
          whenever(viewModel.page.hint.nonEmpty) {
            val html = view(yesNoForm, viewModel).toString
            val document = Jsoup.parse(html)

            val hint = document.select(".govuk-hint").text

            hint mustBe renderMessage(viewModel.page.hint.value).body
          }
        }
      }

      "not have a hint text when not provided" in {

        forAll(viewModelGen) { viewModel =>
          val noHintViewModel = viewModel.copy(page = viewModel.page.copy(hint = None))

          val html = view(yesNoForm, noHintViewModel).toString
          val document = Jsoup.parse(html)

          document.select(".govuk-hint") mustBe empty
        }
      }
    }
  }
}
