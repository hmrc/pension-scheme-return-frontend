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
import models.Money
import play.api.data
import play.api.test.FakeRequest
import views.html.MoneyView

class MoneyViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[MoneyView]

    implicit val request = FakeRequest()

    val moneyForm: data.Form[Money] =
      data.Form("value" -> money("money.error.required"))

    "MoneyView" should {

      "render the title" in {

        forAll(moneyViewModelGen) { viewModel =>
          title(view(moneyForm, viewModel)) must startWith(viewModel.title.key)
        }
      }

      "render the heading" in {

        forAll(moneyViewModelGen) { viewModel =>
          h1(view(moneyForm, viewModel)) mustBe viewModel.heading.key
        }
      }

      "have form" in {

        forAll(moneyViewModelGen) { viewModel =>
          form(view(moneyForm, viewModel)).method mustBe viewModel.onSubmit.method
          form(view(moneyForm, viewModel)).action mustBe viewModel.onSubmit.url
        }
      }

      "render the required error summary" in {

        forAll(moneyViewModelGen) { viewModel =>
          val invalidForm = moneyForm.bind(Map("value" -> ""))
          errorSummary(view(invalidForm, viewModel)).text() must include("money.error.required")
        }
      }

      "render the start date required error message" in {

        forAll(moneyViewModelGen) { viewModel =>
          val invalidForm = moneyForm.bind(Map("value" -> ""))
          errorMessage(view(invalidForm, viewModel)).text() must include("money.error.required")
        }
      }
    }
  }
}
