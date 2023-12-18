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

package controllers.nonsipp.schemedesignatory

import controllers.ControllerBaseSpec
import controllers.nonsipp.schemedesignatory.FeesCommissionsWagesSalariesController._
import forms.MoneyFormProvider
import models.NormalMode
import pages.nonsipp.schemedesignatory.FeesCommissionsWagesSalariesPage
import play.api.Application
import play.api.libs.json.JsPath
import views.html.MoneyView

class FeesCommissionsWagesSalariesControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FeesCommissionsWagesSalariesController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FeesCommissionsWagesSalariesController.onSubmit(srn, NormalMode)

  private val maxAllowedAmount = 999999999.99
  private val validMoney = moneyGen.sample.value
  private def moneyForm(implicit app: Application) = form(injected[MoneyFormProvider])

  "FeesCommissionsWagesSalariesController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MoneyView].apply(viewModel(srn, schemeName, moneyForm, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, FeesCommissionsWagesSalariesPage(srn, NormalMode), validMoney) {
      implicit app => implicit request =>
        injected[MoneyView].apply(viewModel(srn, schemeName, moneyForm.fill(validMoney), NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      saveAndContinue(onSubmit, Some(JsPath \ "schemeDesignatory" \ "totalPayments"), "value" -> s"${validMoney.value}")
        .withName("save and continue")
    )

    act.like(invalidForm(onSubmit))

    act.like(
      invalidForm(onSubmit, "value" -> (maxAllowedAmount + 0.001).toString)
        .withName("fail to submit when amount entered is greater than maximum allowed amount")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
