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

package controllers

import config.Refined.OneToTen
import controllers.SchemeBankAccountListController._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.{BankAccount, NormalMode, UserAnswers}
import pages.SchemeBankAccountPage
import play.api.Application
import play.api.data.Form
import play.api.mvc.Call
import views.html.ListView

class SchemeBankAccountListControllerTest extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.SchemeBankAccountListController.onPageLoad(srn)
  private lazy val onSubmit = routes.SchemeBankAccountListController.onSubmit(srn)

  private lazy val redirectUrlNoBankAccounts = schemeBankAccountRedirectUrl(1)

  "SchemeBankAccountSummaryController" - {

    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    "with 0 bank accounts" - {
      act.like(redirectToPage(onPageLoad, redirectUrlNoBankAccounts))
    }

    List(1, 9, 10).foreach {
      case numBankAccount =>
        s"with $numBankAccount bank account" - {
          val bankAccounts = buildBankAccounts(numBankAccount)
          val userAnswers = buildUserAnswers(bankAccounts)

          act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
            view.apply(form, viewModel(srn, NormalMode, bankAccounts))
          })

          act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
        }
    }
  }

  private def view(implicit a: Application): ListView = injected[ListView]

  private def form(implicit a: Application): Form[Boolean] =
    SchemeBankAccountListController.form(injected[YesNoPageFormProvider])

  private def schemeBankAccountRedirectUrl(bankAccountIndex: Int): Call =
    controllers.routes.SchemeBankAccountController.onPageLoad(srn, Refined.unsafeApply(bankAccountIndex), NormalMode)

  private def buildBankAccounts(num: Int): List[BankAccount] =
    (1 to num).map(i => BankAccount(i.toString, accountNumber, sortCode)).toList

  private def buildUserAnswers(bankAccounts: List[BankAccount]): UserAnswers =
    bankAccounts.zipWithIndex.foldLeft(defaultUserAnswers) {
      case (userAnswers, (bankAccount, index)) =>
        userAnswers
          .set(SchemeBankAccountPage(srn, refineV[OneToTen](index + 1).toOption.value), bankAccount)
          .success
          .value
    }
}
