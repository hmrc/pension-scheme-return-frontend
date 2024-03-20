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

package controllers.nonsipp.otherassetsdisposal

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetController._
import eu.timepit.refined.refineMV
import forms.mappings.errors.MoneyFormErrorProvider
import models.NormalMode
import pages.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetPage
import views.html.MoneyView

class TotalConsiderationSaleAssetControllerSpec extends ControllerBaseSpec {

  private val assetIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.TotalConsiderationSaleAssetController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.TotalConsiderationSaleAssetController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  "TotalConsiderationSaleAssetController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[MoneyView]
          .apply(
            viewModel(
              srn,
              assetIndex,
              disposalIndex,
              form(injected[MoneyFormErrorProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), money) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            viewModel(
              srn,
              assetIndex,
              disposalIndex,
              form(injected[MoneyFormErrorProvider]).fill(money),
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
