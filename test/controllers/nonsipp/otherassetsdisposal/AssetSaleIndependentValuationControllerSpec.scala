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

import pages.nonsipp.otherassetsdisposal.AssetSaleIndependentValuationPage
import controllers.nonsipp.otherassetsdisposal.AssetSaleIndependentValuationController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.YesNoPageView
import utils.IntUtils.given
import forms.YesNoPageFormProvider
import models.NormalMode

class AssetSaleIndependentValuationControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val assetIndex = 1
  private val disposalIndex = 1
  private lazy val onPageLoad =
    routes.AssetSaleIndependentValuationController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.AssetSaleIndependentValuationController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  "AssetSaleIndependentValuationController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, assetIndex, disposalIndex, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, assetIndex, disposalIndex, NormalMode)
          )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
