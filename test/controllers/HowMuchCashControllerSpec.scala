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

import forms.MoneyFormProvider
import models.NormalMode
import pages.HowMuchCashPage
import views.html.MoneyView

class HowMuchCashControllerSpec extends ControllerBaseSpec {

  "HowMuchCashController" should {

    val schemeName = defaultSchemeDetails.schemeName

    val form = HowMuchCashController.form(new MoneyFormProvider(), schemeName, defaultTaxYear)
    lazy val viewModel = HowMuchCashController.viewModel(srn, NormalMode, schemeName, defaultTaxYear)

    val moneyData = moneyGen.sample.value

    lazy val onPageLoad = routes.HowMuchCashController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.HowMuchCashController.onSubmit(srn, NormalMode)

    behave.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[MoneyView]
      view(form, viewModel)
    })

    behave.like(renderPrePopView(onPageLoad, HowMuchCashPage(srn), moneyData) { implicit app => implicit request =>
      val view = injected[MoneyView]
      view(form.fill(moneyData), viewModel)
    })

    behave.like(journeyRecoveryPage("onPageLoad", onPageLoad))

    behave.like(saveAndContinue(onSubmit, formData(form, moneyData): _*))

    behave.like(invalidForm(onSubmit))

    "fail to submit when amount entered is greater than maximum allowed amount" when {
      val maxAllowedAmount = 999999999.99
      behave.like(invalidForm(onSubmit, "value" -> (maxAllowedAmount + 0.001).toString))
    }

    behave.like(journeyRecoveryPage("onSubmit", onSubmit))
  }
}
