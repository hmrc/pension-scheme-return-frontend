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

package controllers.nonsipp.landorproperty

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.MoneyView
import forms.MoneyFormProvider
import utils.IntUtils.given
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandOrPropertyTotalCostPage}
import models.NormalMode
import controllers.nonsipp.landorproperty.LandOrPropertyTotalCostController._

class LandOrPropertyTotalCostControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private lazy val onPageLoad = routes.LandOrPropertyTotalCostController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertyTotalCostController.onSubmit(srn, index, NormalMode)

  "LandOrPropertyTotalCostController" - {

    val updatedUserAnswers = defaultUserAnswers.unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)

    act.like(renderView(onPageLoad, updatedUserAnswers) { implicit app => implicit request =>
      injected[MoneyView].apply(
        form(injected[MoneyFormProvider]),
        viewModel(srn, index, address.addressLine1, form(injected[MoneyFormProvider]), NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, LandOrPropertyTotalCostPage(srn, index), money, updatedUserAnswers) {
      implicit app => implicit request =>
        injected[MoneyView].apply(
          form(injected[MoneyFormProvider]).fill(money),
          viewModel(srn, index, address.addressLine1, form(injected[MoneyFormProvider]), NormalMode)
        )
    })

    act.like(redirectNextPage(onSubmit, updatedUserAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, updatedUserAnswers, "value" -> "1"))

    act.like(invalidForm(onSubmit, updatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
