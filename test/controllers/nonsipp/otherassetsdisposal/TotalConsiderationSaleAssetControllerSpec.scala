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

import pages.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.MoneyView
import utils.IntUtils.given
import models.NormalMode
import controllers.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetController._
import forms.MoneyFormProvider

class TotalConsiderationSaleAssetControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val assetIndex = 1
  private val disposalIndex = 1

  private lazy val onPageLoad =
    routes.TotalConsiderationSaleAssetController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.TotalConsiderationSaleAssetController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  "TotalConsiderationSaleAssetController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[MoneyView]
          .apply(
            form(injected[MoneyFormProvider]),
            viewModel(
              srn,
              assetIndex,
              disposalIndex,
              form(injected[MoneyFormProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), money) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            form(injected[MoneyFormProvider]).fill(money),
            viewModel(
              srn,
              assetIndex,
              disposalIndex,
              form(injected[MoneyFormProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
