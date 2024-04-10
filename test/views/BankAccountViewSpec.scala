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

import play.api.test.FakeRequest
import play.api.data.Forms.{mapping, text}
import views.html.BankAccountView
import play.api.data
import models.BankAccount
import viewmodels.models.BankAccountViewModel

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

    val viewModelGen = formPageViewModelGen[BankAccountViewModel]

    "BankAccountView" - {

      act.like(renderTitle(viewModelGen)(view(bankAccountForm, _), _.title.key))
      act.like(renderHeading(viewModelGen)(view(bankAccountForm, _), _.heading))

      "render the bank name heading" in {
        forAll(viewModelGen) { viewModel =>
          inputLabel(view(bankAccountForm, viewModel))("bankName").text() must startWith(
            viewModel.page.bankNameHeading.key
          )
        }
      }

      "render bank name required error" in {
        forAll(viewModelGen) { viewModel =>
          val preparedForm =
            bankAccountForm.bind(Map("bankName" -> "", "accountNumber" -> "12345678", "sortCode" -> "12-34-56"))
          errorMessage(view(preparedForm, viewModel)).text() mustBe renderedErrorMessage("bankName.required")
        }
      }

      "render the account number heading" in {
        forAll(viewModelGen) { viewModel =>
          inputLabel(view(bankAccountForm, viewModel))("accountNumber").text() must startWith(
            viewModel.page.accountNumberHeading.key
          )
        }
      }

      "render the account number hint" in {
        forAll(viewModelGen) { viewModel =>
          inputHint(view(bankAccountForm, viewModel))("accountNumber").text() must startWith(
            viewModel.page.accountNumberHint.key
          )
        }
      }

      "render account number required error" in {
        forAll(viewModelGen) { viewModel =>
          val preparedForm =
            bankAccountForm.bind(Map("bankName" -> "abc", "accountNumber" -> "", "sortCode" -> "12-34-56"))
          errorMessage(view(preparedForm, viewModel)).text() mustBe renderedErrorMessage("accountNumber.required")
        }
      }

      "render the sort code heading" in {
        forAll(viewModelGen) { viewModel =>
          inputLabel(view(bankAccountForm, viewModel))("sortCode").text() must startWith(
            viewModel.page.sortCodeHeading.key
          )
        }
      }

      "render the sort code hint" in {
        forAll(viewModelGen) { viewModel =>
          inputHint(view(bankAccountForm, viewModel))("sortCode").text() must startWith(viewModel.page.sortCodeHint.key)
        }
      }

      "render sort code required error" in {
        forAll(viewModelGen) { viewModel =>
          val preparedForm =
            bankAccountForm.bind(Map("bankName" -> "abc", "accountNumber" -> "12345678", "sortCode" -> ""))
          errorMessage(view(preparedForm, viewModel)).text() mustBe renderedErrorMessage("sortCode.required")
        }
      }

      "render the form" in {

        forAll(viewModelGen) { viewModel =>
          form(view(bankAccountForm, viewModel)).method mustBe viewModel.onSubmit.method
          form(view(bankAccountForm, viewModel)).action mustBe viewModel.onSubmit.url
        }
      }

      act.like(renderButtonText(viewModelGen)(view(bankAccountForm, _), _.buttonText))
    }
  }
}
