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

package controllers.nonsipp.shares

import config.Refined.Max5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.shares.WhyDoesSchemeHoldSharesController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import models.{NormalMode, TypeOfShares}
import pages.nonsipp.shares.TypeOfSharesHeldPage
import views.html.RadioListView

class WhyDoesSchemeHoldSharesControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)

  private lazy val onPageLoad =
    routes.WhyDoesSchemeHoldSharesController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    routes.WhyDoesSchemeHoldSharesController.onSubmit(srn, index, NormalMode)

  private val userAnswers =
    defaultUserAnswers.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)

  "WhyDoesSchemeHoldSharesController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, index, schemeName, TypeOfShares.ConnectedParty.name, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "Acquisition data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> Acquisition.name))
    }

    "Contribution data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> Contribution.name))
    }

    "Transfer data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> Transfer.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
