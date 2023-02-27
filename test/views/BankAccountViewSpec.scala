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

import models.BankAccount
import play.api.data
import play.api.data.Forms.{mapping, text}
import play.api.test.FakeRequest
import views.html.BankAccountView

class BankAccountViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[BankAccountView]

    val bankAccountForm = data.Form(
      mapping(
        "bankName" -> text.verifying("bankName.required", _.nonEmpty),
        "accountNumber" -> text.verifying("accountNumber.required", _.nonEmpty),
        "sortCode" -> text.verifying("sortCode.required", _.nonEmpty)
      )(BankAccount.apply)(BankAccount.unapply)
    )

    implicit val request = FakeRequest()

    "BankAccountView" should {

      behave like renderTitle(bankAccountViewModelGen)(view(bankAccountForm, _), _.title.key)
      behave like renderHeading(bankAccountViewModelGen)(view(bankAccountForm, _), _.heading.key)

      "render the bank name heading" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          inputLabel(view(bankAccountForm, viewModel))("bankName").text() must startWith(viewModel.bankNameHeading.key)
        }
      }

      "render bank name required error" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          val preparedForm = bankAccountForm.bind(Map("bankName" -> "", "accountNumber" -> "12345678", "sortCode" -> "12-34-56"))
          errorMessage(view(preparedForm, viewModel)).text() mustBe renderedErrorMessage("bankName.required")
        }
      }

      "render the account number heading" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          inputLabel(view(bankAccountForm, viewModel))("accountNumber").text() must startWith(viewModel.accountNumberHeading.key)
        }
      }

      "render the account number hint" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          inputHint(view(bankAccountForm, viewModel))("accountNumber").text() must startWith(viewModel.accountNumberHint.key)
        }
      }

      "render account number required error" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          val preparedForm = bankAccountForm.bind(Map("bankName" -> "abc", "accountNumber" -> "", "sortCode" -> "12-34-56"))
          errorMessage(view(preparedForm, viewModel)).text() mustBe renderedErrorMessage("accountNumber.required")
        }
      }

      "render the sort code heading" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          inputLabel(view(bankAccountForm, viewModel))("sortCode").text() must startWith(viewModel.sortCodeHeading.key)
        }
      }

      "render the sort code hint" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          inputHint(view(bankAccountForm, viewModel))("sortCode").text() must startWith(viewModel.sortCodeHint.key)
        }
      }

      "render sort code required error" in {
        forAll(bankAccountViewModelGen) { viewModel =>
          val preparedForm = bankAccountForm.bind(Map("bankName" -> "abc", "accountNumber" -> "12345678", "sortCode" -> ""))
          errorMessage(view(preparedForm, viewModel)).text() mustBe renderedErrorMessage("sortCode.required")
        }
      }

      "render the form" in {

        forAll(bankAccountViewModelGen) { viewModel =>

          form(view(bankAccountForm, viewModel)).method mustBe viewModel.onSubmit.method
          form(view(bankAccountForm, viewModel)).action mustBe viewModel.onSubmit.url
        }
      }

      behave like renderButtonText(bankAccountViewModelGen)(view(bankAccountForm, _), _.buttonText)
    }
  }
}
