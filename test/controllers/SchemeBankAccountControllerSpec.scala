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
import controllers.SchemeBankAccountController._
import eu.timepit.refined.refineMV
import forms.BankAccountFormProvider
import models.NormalMode
import pages.SchemeBankAccountPage
import views.html.BankAccountView

class SchemeBankAccountControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.SchemeBankAccountController.onPageLoad(srn, refineMV[OneToTen](1), NormalMode)
  private lazy val onSubmit = routes.SchemeBankAccountController.onSubmit(srn, refineMV[OneToTen](1), NormalMode)

  private val bankAccount = bankAccountGen.sample.value
  private val otherBankAccount = bankAccountGen.sample.value

  private val validFormData = List(
    "bankName" -> bankAccount.bankName,
    "accountNumber" -> bankAccount.accountNumber,
    "sortCode" -> bankAccount.sortCode
  )
  private val invalidFormData = List("bankName" -> "testBankName", "accountNumber" -> "10273837", "sortCode" -> "wrong")

  "SchemeBankAccountController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[BankAccountView]
        .apply(form(injected[BankAccountFormProvider], List()), viewModel(srn, refineMV[OneToTen](1), NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, SchemeBankAccountPage(srn, refineMV(1)), bankAccount) {
      implicit app => implicit request =>
        val preparedForm = form(injected[BankAccountFormProvider], List()).fill(bankAccount)
        injected[BankAccountView].apply(preparedForm, viewModel(srn, refineMV[OneToTen](1), NormalMode))
    })

    act.like(invalidForm(onSubmit, invalidFormData: _*))
    act.like(saveAndContinue(onSubmit, validFormData: _*))

    "persist data when updating" - {
      val ua = defaultUserAnswers.set(SchemeBankAccountPage(srn, refineMV(1)), bankAccount).get
      act.like(saveAndContinue(onSubmit, ua, validFormData: _*))
    }

    "return a 400 when data exists in user answers" - {
      val userAnswers = defaultUserAnswers
        .unsafeSet(SchemeBankAccountPage(srn, refineMV(1)), otherBankAccount)
        .unsafeSet(SchemeBankAccountPage(srn, refineMV(2)), bankAccount)

      act.like(invalidForm(onSubmit, userAnswers, validFormData: _*))
    }

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
