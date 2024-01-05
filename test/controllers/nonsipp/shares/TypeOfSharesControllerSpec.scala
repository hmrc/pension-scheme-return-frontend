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
import controllers.nonsipp.shares.TypeOfSharesHeldController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.NormalMode
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import views.html.RadioListView

class TypeOfSharesControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)

  private lazy val onPageLoad =
    routes.TypeOfSharesHeldController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    routes.TypeOfSharesHeldController.onSubmit(srn, index, NormalMode)

  "typeOfSharesController" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, index, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "SponsoringEmployer data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> SponsoringEmployer.name))
    }

    "Unquoted data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> Unquoted.name))
    }

    "ConnectedParty data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> ConnectedParty.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
