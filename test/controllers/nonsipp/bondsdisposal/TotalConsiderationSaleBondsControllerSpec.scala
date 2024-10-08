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

package controllers.nonsipp.bondsdisposal

import views.html.MoneyView
import eu.timepit.refined.refineMV
import controllers.nonsipp.bondsdisposal.TotalConsiderationSaleBondsController._
import forms.MoneyFormProvider
import models.NormalMode
import pages.nonsipp.bondsdisposal.TotalConsiderationSaleBondsPage
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec

class TotalConsiderationSaleBondsControllerSpec extends ControllerBaseSpec {

  private val bondIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.TotalConsiderationSaleBondsController.onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.TotalConsiderationSaleBondsController.onSubmit(srn, bondIndex, disposalIndex, NormalMode)

  "TotalConsiderationSaleBondsController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[MoneyView]
          .apply(
            form(injected[MoneyFormProvider]),
            viewModel(
              srn,
              bondIndex,
              disposalIndex,
              form(injected[MoneyFormProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, TotalConsiderationSaleBondsPage(srn, bondIndex, disposalIndex), money) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            form(injected[MoneyFormProvider]).fill(money),
            viewModel(
              srn,
              bondIndex,
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
