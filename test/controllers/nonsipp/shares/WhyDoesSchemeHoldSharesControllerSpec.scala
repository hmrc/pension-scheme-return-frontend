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

package controllers.nonsipp.shares

import pages.nonsipp.shares.{TypeOfSharesHeldPage, WhyDoesSchemeHoldSharesPage}
import config.Refined.Max5000
import controllers.ControllerBaseSpec
import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{NormalMode, TypeOfShares}
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import controllers.nonsipp.shares.WhyDoesSchemeHoldSharesController._

class WhyDoesSchemeHoldSharesControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val typeOfShares = typeOfSharesGen.sample.value
  private val schemeHoldShares = schemeHoldSharesGen.sample.value

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

    act.like(
      renderPrePopView(
        onPageLoad,
        TypeOfSharesHeldPage(srn, index),
        typeOfShares,
        defaultUserAnswers
          .unsafeSet(TypeOfSharesHeldPage(srn, index), typeOfShares)
          .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), schemeHoldShares)
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(schemeHoldShares),
            viewModel(srn, index, schemeName, typeOfShares.name, NormalMode)
          )
      }
    )

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
