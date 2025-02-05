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

import pages.nonsipp.bonds.WhyDoesSchemeHoldBondsPage
import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.NormalMode
import controllers.nonsipp.bonds.WhyDoesSchemeHoldBondsController._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec
import models.SchemeHoldBond.{Acquisition, Contribution, Transfer}

class WhyDoesSchemeHoldBondsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)

  private lazy val onPageLoad =
    routes.WhyDoesSchemeHoldBondsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    routes.WhyDoesSchemeHoldBondsController.onSubmit(srn, index, NormalMode)

  "WhyDoesSchemeHoldBondsController" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, index, schemeName, NormalMode)
      )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        WhyDoesSchemeHoldBondsPage(srn, index),
        schemeHoldBonds,
        defaultUserAnswers
          .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), schemeHoldBonds)
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(schemeHoldBonds),
            viewModel(srn, index, schemeName, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "Acquisition data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> Acquisition.name))
    }

    "Contribution data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> Contribution.name))
    }

    "Transfer data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> Transfer.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
