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

import config.Refined.OneToThree
import controllers.RemoveAccountingPeriodController._
import eu.timepit.refined._
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.AccountingPeriodPage
import views.html.YesNoPageView

class RemoveAccountingPeriodControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveAccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = routes.RemoveAccountingPeriodController.onSubmit(srn, refineMV(1), NormalMode)

  private val period = dateRangeGen.sample.value
  private val otherPeriod = dateRangeGen.sample.value

  private val userAnswers = defaultUserAnswers
    .set(AccountingPeriodPage(srn, refineMV(1)), period).success.value
    .set(AccountingPeriodPage(srn, refineMV(2)), otherPeriod).success.value

  "RemoveSchemeBankAccountController" should {

    behave like renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, refineMV(1), period, NormalMode)
      )
    }

    behave like redirectToPage(onPageLoad, controllers.routes.JourneyRecoveryController.onPageLoad())

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)

    behave like continueNoSave(onSubmit, userAnswers, "value" -> "false")
    behave like saveAndContinue(onSubmit, userAnswers, "value" -> "true")

    behave like journeyRecoveryPage("onSubmit", onSubmit)


  }
}
