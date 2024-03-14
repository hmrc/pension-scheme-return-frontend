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

package controllers.nonsipp.unregulatedorconnectedbonds

import config.Refined._
import controllers.ControllerBaseSpec
import controllers.nonsipp.unregulatedorconnectedbonds.IncomeFromBondsController._
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models.NormalMode
import pages.nonsipp.bonds.IncomeFromBondsPage
import views.html.MoneyView

class IncomeFromBondsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  "IncomeFromBondsController" - {

    lazy val onPageLoad = routes.IncomeFromBondsController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.IncomeFromBondsController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[MoneyView]
        .apply(
          viewModel(
            srn,
            index,
            form(injected[MoneyFormProvider]),
            NormalMode
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, IncomeFromBondsPage(srn, index), money, defaultUserAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            viewModel(
              srn,
              index,
              form(injected[MoneyFormProvider]).fill(money),
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
