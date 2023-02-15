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

import controllers.SchemeBankAccountController._
import forms.BankAccountFormProvider
import models.{BankAccount, NormalMode}
import pages.SchemeBankAccountPage
import views.html.BankAccountView

class SchemeBankAccountControllerSpec extends ControllerBaseSpec {

  private val onPageLoad = routes.SchemeBankAccountController.onPageLoad(srn, NormalMode).url
  private val onSubmit = routes.SchemeBankAccountController.onSubmit(srn, NormalMode).url

  private val bankAccount = BankAccount("testBankName", "10273837", "027162")

  private val redirectUrl = controllers.routes.UnauthorisedController.onPageLoad.url

  private val validFormData = List("bankName" -> "testBankName", "accountNumber" -> "10273837", "sortCode" -> "027123")
  private val invalidFormData = List("bankName" -> "testBankName", "accountNumber" -> "10273837", "sortCode" -> "wrong")

  "SchemeBankAccountController" should {

    behave like renderView(onPageLoad) { implicit app => implicit request =>
      injected[BankAccountView].apply(form(injected[BankAccountFormProvider]), viewModel(srn, NormalMode))
    }
    behave like invalidForm(onSubmit, invalidFormData: _*)
    behave like redirectNextPage(onSubmit, redirectUrl, validFormData: _*)
    
    behave like renderPrePopView(onPageLoad, SchemeBankAccountPage(srn), bankAccount) { implicit app => implicit request =>
      val preparedForm = form(injected[BankAccountFormProvider]).fill(bankAccount)
      injected[BankAccountView].apply(preparedForm, viewModel(srn, NormalMode))
    }
  }
}
