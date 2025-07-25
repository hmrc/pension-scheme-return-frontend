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

package controllers.nonsipp.bonds

import pages.nonsipp.bonds.CostOfBondsPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.MoneyView
import utils.IntUtils.given
import forms.MoneyFormProvider
import controllers.nonsipp.bonds.CostOfBondsController._
import models.NormalMode

class CostOfBondsControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  "CostOfBondsController" - {

    lazy val onPageLoad = routes.CostOfBondsController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.CostOfBondsController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[MoneyView]
        .apply(
          form(injected[MoneyFormProvider]),
          viewModel(
            srn,
            index,
            form(injected[MoneyFormProvider]),
            NormalMode
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, CostOfBondsPage(srn, index), money, defaultUserAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            form(injected[MoneyFormProvider]).fill(money),
            viewModel(
              srn,
              index,
              form(injected[MoneyFormProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, defaultUserAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> "1"))

    act.like(invalidForm(onSubmit, defaultUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
