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
import eu.timepit.refined._
import forms.BankAccountFormProvider
import models.{BankAccount, NormalMode}
import org.mockito.ArgumentMatchers.any
import pages.{SchemeBankAccounts, SchemeBankAccountPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import services.SaveService
import views.html.BankAccountView

class SchemeBankAccountControllerSpec extends ControllerBaseSpec {

  private val onPageLoad = routes.SchemeBankAccountController.onPageLoad(srn, refineMV[OneToTen](1), NormalMode).url
  private val onSubmit = routes.SchemeBankAccountController.onSubmit(srn, refineMV[OneToTen](1), NormalMode).url

  private val bankAccount = BankAccount("testBankName", "10273837", "027162")
  private val otherBankAccount = BankAccount("otherTestBankName", "90273999", "027999")

  private val redirectUrl = controllers.routes.SchemeBankAccountListController.onPageLoad(srn).url

  private val validFormData = List("bankName" -> "testBankName", "accountNumber" -> "10273837", "sortCode" -> "027123")
  private val invalidFormData = List("bankName" -> "testBankName", "accountNumber" -> "10273837", "sortCode" -> "wrong")

  "SchemeBankAccountController" should {

    behave like renderView(onPageLoad) { implicit app => implicit request =>
      injected[BankAccountView].apply(form(injected[BankAccountFormProvider]), viewModel(srn, refineMV[OneToTen](1), NormalMode))
    }
    behave like invalidForm(onSubmit, invalidFormData: _*)
    behave like redirectNextPage(onSubmit, redirectUrl, validFormData: _*)
    
    behave like renderPrePopView(onPageLoad, SchemeBankAccountPage(srn, 0), bankAccount) { implicit app => implicit request =>
      val preparedForm = form(injected[BankAccountFormProvider]).fill(bankAccount)
      injected[BankAccountView].apply(preparedForm, viewModel(srn, refineMV[OneToTen](1), NormalMode))
    }

    "not persist bank account if the account number already exists" in {
      val mockSaveService = mock[SaveService]
      val userAnswers = defaultUserAnswers
        .set(SchemeBankAccountPage(srn, 0), bankAccount).get
        .set(SchemeBankAccountPage(srn, 1), otherBankAccount).get

      val appBuilder = applicationBuilder(Some(userAnswers))
        .bindings(bind[SaveService].toInstance(mockSaveService))

      running(_ => appBuilder) { app =>
        val request = FakeRequest(POST, onSubmit).withFormUrlEncodedBody(
            "bankName" -> "testUniqueBankAccount",
          "accountNumber" -> otherBankAccount.accountNumber,
          "sortCode" -> "123456"
        )
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual redirectUrl

        verify(mockSaveService, never).save(any())(any())
      }
    }
  }
}
