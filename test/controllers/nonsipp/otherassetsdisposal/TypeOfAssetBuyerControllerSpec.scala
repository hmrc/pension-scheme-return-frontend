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

package controllers.nonsipp.otherassetsdisposal

import models.IdentityType._
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.NormalMode
import controllers.nonsipp.otherassetsdisposal.TypeOfAssetBuyerController._

class TypeOfAssetBuyerControllerSpec extends ControllerBaseSpec {

  private val assetIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.TypeOfAssetBuyerController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.TypeOfAssetBuyerController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  "TypeOfAssetBuyerController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, assetIndex, disposalIndex, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "anIndividual data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> Individual.name))
    }
    "aUKCompany data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> UKCompany.name))
    }
    "aUKPartnership data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> UKPartnership.name))
    }
    "other data is submitted" - {
      act.like(saveAndContinue(onSubmit, "value" -> Other.name))
    }
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
