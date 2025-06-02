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

import pages.nonsipp.bonds.BondsFromConnectedPartyPage
import views.html.YesNoPageView
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec
import models.NormalMode
import controllers.nonsipp.bonds.BondsFromConnectedPartyController._

class BondsFromConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private lazy val onPageLoad = routes.BondsFromConnectedPartyController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.BondsFromConnectedPartyController.onSubmit(srn, index, NormalMode)
  private val incomeTaxAct = "https://www.legislation.gov.uk/ukpga/2007/3/section/993"

  "BondsFromConnectedPartyControllerSpec" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, incomeTaxAct, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        BondsFromConnectedPartyPage(srn, index),
        true,
        defaultUserAnswers
      ) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, incomeTaxAct, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, defaultUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
