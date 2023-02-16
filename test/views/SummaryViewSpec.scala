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

import forms.YesNoPageFormProvider
import play.api.test.FakeRequest
import views.html.SummaryView

class SummaryViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[SummaryView]
    val formProvider = injected[YesNoPageFormProvider]
    val form = formProvider("summaryView.required")

    implicit val request: FakeRequest[_] = FakeRequest()

    "SummaryView" should {
      behave like renderTitle(summaryViewModelGen())(view(form, _), _.title.key)
      behave like renderHeading(summaryViewModelGen())(view(form, _), _.heading.key)
      behave like renderButtonText(summaryViewModelGen())(view(form, _), _.buttonText)

      "render rows" in {
        forAll(summaryViewModelGen()) { viewModel =>
          val renderedRows = summaryListRows(view(form, viewModel))
          renderedRows.length mustEqual viewModel.rows.size
          renderedRows.map(_.selectFirst(".govuk-summary-list__key").text()) mustEqual viewModel.rows.map(row => messageKey(row.text))
        }
      }

      "render radio button and not the inset text when showRadios is true" in {
        forAll(summaryViewModelGen()) { viewModel =>
          val radioElements = radios(view(form, viewModel))
          radioElements.size mustEqual 2
          radioElements.map(_.id()) mustEqual List("value", "value-no")
          Option(inset(view(form, viewModel))) mustBe None
        }
      }

      "renders the inset text and not the radio button whenShowRadios is false" in {
        forAll(summaryViewModelGen(false)) { viewModel =>
          radios(view(form, viewModel)).size mustEqual 0
          inset(view(form, viewModel)).text() mustEqual viewModel.insetText.key
        }
      }
    }
  }
}
