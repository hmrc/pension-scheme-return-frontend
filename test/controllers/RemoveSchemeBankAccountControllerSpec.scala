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
import controllers.RemoveSchemeBankAccountController._
import eu.timepit.refined._
import forms.YesNoPageFormProvider
import models.{BankAccount, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import pages.SchemeBankAccountPage
import play.api.inject.bind
import play.api.test.FakeRequest
import services.SaveService
import views.html.YesNoPageView

import scala.concurrent.Future

class RemoveSchemeBankAccountControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveSchemeBankAccountController.onPageLoad(srn, refineMV[OneToTen](1), NormalMode)
  private lazy val onSubmit = routes.RemoveSchemeBankAccountController.onSubmit(srn, refineMV[OneToTen](1), NormalMode)

  private lazy val redirectUrl = routes.SchemeBankAccountListController.onPageLoad(srn)

  private val bankAccount = BankAccount("testBankName", "12345678", "123456")
  private val otherBankAccount = BankAccount("otherTestBankName", "11112222", "111222")

  private val userAnswersWithBankAccounts = defaultUserAnswers
    .set(SchemeBankAccountPage(srn, refineMV[OneToTen](1)), bankAccount).success.value
    .set(SchemeBankAccountPage(srn, refineMV[OneToTen](2)), otherBankAccount).success.value

  "RemoveSchemeBankAccountController" should {

    behave like renderView(onPageLoad, userAnswersWithBankAccounts) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, refineMV[OneToTen](1), bankAccount, NormalMode))
    }

    behave like redirectToPage(onSubmit, redirectUrl, userAnswersWithBankAccounts, "value" -> "false")
    behave like redirectToPage(onPageLoad, controllers.routes.JourneyRecoveryController.onPageLoad())

    "remove the correct bank account on submit when yes is selected" in {
      val mockSaveService = mock[SaveService]

      val appBuilder = applicationBuilder(Some(userAnswersWithBankAccounts))
        .bindings(bind[SaveService].toInstance(mockSaveService))

      when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

      running(_ => appBuilder) { app =>
        val request = FakeRequest(POST, onSubmit.url).withFormUrlEncodedBody("value" -> "true")
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual redirectUrl.url
        verify(mockSaveService).save(captor.capture())(any(), any())

        captor.getValue mustBe defaultUserAnswers.set(SchemeBankAccountPage(srn, refineMV[OneToTen](1)), otherBankAccount).success.value
      }
    }

    "do nothing and redirect to bank account list page when no is selected" in {
      val mockSaveService = mock[SaveService]

      val appBuilder = applicationBuilder(Some(userAnswersWithBankAccounts))
        .bindings(bind[SaveService].toInstance(mockSaveService))

      when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))

      running(_ => appBuilder) { app =>
        val request = FakeRequest(POST, onSubmit.url).withFormUrlEncodedBody("value" -> "false")
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual redirectUrl.url
        verify(mockSaveService, never).save(any())(any(), any())
      }
    }
  }
}
