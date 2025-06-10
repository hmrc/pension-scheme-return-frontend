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

package controllers.nonsipp.schemedesignatory

import pages.nonsipp.schemedesignatory.FeesCommissionsWagesSalariesPage
import views.html.MoneyViewWithDescription
import forms.MoneyFormProvider
import models.{Money, NormalMode}
import play.api.data.Form
import controllers.nonsipp.schemedesignatory.FeesCommissionsWagesSalariesController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.Application
import play.api.libs.json.JsPath

class FeesCommissionsWagesSalariesControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.FeesCommissionsWagesSalariesController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FeesCommissionsWagesSalariesController.onSubmit(srn, NormalMode)

  private val maxAllowedAmount = 999999999.99
  private val validMoney = moneyGen.sample.value
  private def moneyForm(implicit app: Application): Form[Money] = form(injected[MoneyFormProvider])

  "FeesCommissionsWagesSalariesController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MoneyViewWithDescription].apply(moneyForm, viewModel(srn, moneyForm, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, FeesCommissionsWagesSalariesPage(srn, NormalMode), validMoney) {
      implicit app => implicit request =>
        injected[MoneyViewWithDescription].apply(moneyForm.fill(validMoney), viewModel(srn, moneyForm, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      saveAndContinue(onSubmit, Some(JsPath \ "schemeDesignatory" \ "totalPayments"), "value" -> s"${validMoney.value}")
    )

    act.like(invalidForm(onSubmit))

    act.like(
      invalidForm(onSubmit, "value" -> (maxAllowedAmount + 0.001).toString)
        .withName("fail to submit when amount entered is greater than maximum allowed amount")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
