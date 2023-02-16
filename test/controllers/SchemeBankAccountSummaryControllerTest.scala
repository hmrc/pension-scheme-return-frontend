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

import controllers.SchemeBankAccountSummaryController._
import eu.timepit.refined.api.Refined
import forms.YesNoPageFormProvider
import models.{BankAccount, NormalMode, UserAnswers}
import pages.SchemeBankAccountPage
import play.api.Application
import play.api.data.Form
import views.html.SummaryView

class SchemeBankAccountSummaryControllerTest extends ControllerBaseSpec {

  private val onPageLoad = routes.SchemeBankAccountSummaryController.onPageLoad(srn).url
  private val onSubmit = routes.SchemeBankAccountSummaryController.onSubmit(srn).url

  private val redirectUrlFalse = controllers.routes.UnauthorisedController.onPageLoad.url
  private val redirectUrlNoBankAccounts = schemeBankAccountRedirectUrl(1)
  private val redirectNextPage = controllers.routes.UnauthorisedController.onPageLoad.url

  "SchemeBankAccountSummaryController" should {

    behave like redirectNextPage(onSubmit, redirectUrlFalse, "value" -> "false")

    "with 0 bank accounts" must {
      behave like redirectOnPageLoad(onPageLoad, redirectUrlNoBankAccounts)
    }

    List(
      (1, schemeBankAccountRedirectUrl(2)),
      (9, schemeBankAccountRedirectUrl(10)),
      (10, redirectNextPage)
    ).foreach { case (numBankAccount, redirectOnSubmitUrl) =>
      s"with $numBankAccount bank account" should {
        val bankAccounts = buildBankAccounts(numBankAccount)
        val userAnswers = buildUserAnswers(bankAccounts)

        behave like renderView(onPageLoad, userAnswers) { implicit app =>
          implicit request =>
            view.apply(form, viewModel(srn, NormalMode, bankAccounts))
        }
        behave like redirectNextPage(onSubmit, redirectOnSubmitUrl, userAnswers, "value" -> "true")
        behave like redirectNextPage(onSubmit, redirectNextPage, userAnswers, "value" -> "false")
      }
    }
  }

  private def view(implicit a: Application): SummaryView = injected[SummaryView]

  private def form(implicit a: Application): Form[Boolean] = SchemeBankAccountSummaryController.form(injected[YesNoPageFormProvider])

  private def schemeBankAccountRedirectUrl(bankAccountIndex: Int): String =
    controllers.routes.SchemeBankAccountController.onPageLoad(srn, Refined.unsafeApply(bankAccountIndex), NormalMode).url

  private def buildBankAccounts(num: Int): List[BankAccount] = (1 to num).map(i => BankAccount(i.toString, accountNumber, sortCode)).toList

  private def buildUserAnswers(bankAccounts: List[BankAccount]): UserAnswers =
    defaultUserAnswers.set(SchemeBankAccountPage(srn), bankAccounts).success.value
}
