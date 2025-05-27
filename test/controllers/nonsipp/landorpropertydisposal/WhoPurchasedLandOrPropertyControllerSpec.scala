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

package controllers.nonsipp.landorpropertydisposal

import models.IdentityType._
import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.NormalMode
import controllers.nonsipp.landorpropertydisposal.WhoPurchasedLandOrPropertyController._
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec

class WhoPurchasedLandOrPropertyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.WhoPurchasedLandOrPropertyController.onPageLoad(srn, index.value, disposalIndex.value, NormalMode)
  private lazy val onSubmit =
    routes.WhoPurchasedLandOrPropertyController.onSubmit(srn, index.value, disposalIndex.value, NormalMode)

  private val userAnswers = userAnswersWithAddress(srn, index)

  "whoPurchasedLandOrPropertyController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, index, disposalIndex, address.addressLine1, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "anIndividual data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> Individual.name))
    }

    "aUKCompany data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> UKCompany.name))
    }

    "aUKPartnership data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> UKPartnership.name))
    }

    "other data is submitted" - {
      act.like(saveAndContinue(onSubmit, userAnswers, "value" -> Other.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
