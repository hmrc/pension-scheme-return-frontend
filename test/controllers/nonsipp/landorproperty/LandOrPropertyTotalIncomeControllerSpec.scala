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

package controllers.nonsipp.landorproperty

import config.Refined.Max5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorproperty.LandOrPropertyTotalIncomeController._
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models.NormalMode
import pages.nonsipp.landorproperty.LandOrPropertyTotalIncomePage
import views.html.MoneyView

class LandOrPropertyTotalIncomeControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private lazy val onPageLoad = routes.LandOrPropertyTotalIncomeController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertyTotalIncomeController.onSubmit(srn, index, NormalMode)

  private val userAnswers = userAnswersWithAddress(srn, index)

  "LandOrPropertyTotalIncomeController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[MoneyView].apply(
        viewModel(srn, index, form(injected[MoneyFormProvider]), address.addressLine1, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, LandOrPropertyTotalIncomePage(srn, index), money, userAnswers) {
      implicit app => implicit request =>
        injected[MoneyView].apply(
          viewModel(srn, index, form(injected[MoneyFormProvider]).fill(money), address.addressLine1, NormalMode)
        )
    })

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "1"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
