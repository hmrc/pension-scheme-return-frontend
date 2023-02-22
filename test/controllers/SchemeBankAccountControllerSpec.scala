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
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import pages.SchemeBankAccountPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import services.SaveService
import views.html.BankAccountView

class SchemeBankAccountControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.SchemeBankAccountController.onPageLoad(srn, refineMV[OneToTen](1), NormalMode)
  private lazy val onSubmit = routes.SchemeBankAccountController.onSubmit(srn, refineMV[OneToTen](1), NormalMode)

  private val bankAccount = bankAccountGen.sample.value
  private val otherBankAccount = bankAccountGen.sample.value

  private val validFormData = List("bankName" -> "testBankName", "accountNumber" -> "10273837", "sortCode" -> "027123")
  private val invalidFormData = List("bankName" -> "testBankName", "accountNumber" -> "10273837", "sortCode" -> "wrong")

  "SchemeBankAccountController" should {

    behave like renderView(onPageLoad) { implicit app => implicit request =>
      injected[BankAccountView].apply(form(injected[BankAccountFormProvider]), viewModel(srn, refineMV[OneToTen](1), NormalMode))
    }
    behave like invalidForm(onSubmit, invalidFormData: _*)
    behave like redirectNextPage(onSubmit, validFormData: _*)
    
    behave like renderPrePopView(onPageLoad, SchemeBankAccountPage(srn, refineMV(1)), bankAccount) { implicit app => implicit request =>
      val preparedForm = form(injected[BankAccountFormProvider]).fill(bankAccount)
      injected[BankAccountView].apply(preparedForm, viewModel(srn, refineMV[OneToTen](1), NormalMode))
    }

    "not persist bank account if the account number already exists" in {
      val mockSaveService = mock[SaveService]
      val userAnswers = defaultUserAnswers
        .set(SchemeBankAccountPage(srn, refineMV(1)), bankAccount).get
        .set(SchemeBankAccountPage(srn, refineMV(2)), otherBankAccount).get

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SaveService].toInstance(mockSaveService),
          bind[Navigator].toInstance(new FakeNavigator(Call("GET", "/foo")))
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(onSubmit).withFormUrlEncodedBody(
            "bankName" -> "testUniqueBankAccount",
          "accountNumber" -> otherBankAccount.accountNumber,
          "sortCode" -> "123456"
        )
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "/foo"

        verify(mockSaveService, never).save(any())(any())
      }
    }

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)
    behave like journeyRecoveryPage("onSubmit", onSubmit)
  }
}
