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

package controllers.nonsipp.bondsdisposal

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.bondsdisposal.BondsStillHeldController._
import eu.timepit.refined.refineMV
import forms.IntFormProvider
import models.NormalMode
import pages.nonsipp.bondsdisposal.BondsStillHeldPage
import views.html.IntView

class BondsStillHeldControllerSpec extends ControllerBaseSpec {

  private val bondIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.BondsStillHeldController.onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.BondsStillHeldController.onSubmit(srn, bondIndex, disposalIndex, NormalMode)

  private val userAnswers = defaultUserAnswers

  "BondsStillHeldController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[IntView].apply(
          viewModel(
            srn,
            bondIndex,
            disposalIndex,
            schemeName,
            NormalMode,
            form(injected[IntFormProvider])
          )
        )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, BondsStillHeldPage(srn, bondIndex, disposalIndex), bondsStillHeld, userAnswers) {
        implicit app => implicit request =>
          injected[IntView]
            .apply(
              viewModel(
                srn,
                bondIndex,
                disposalIndex,
                schemeName,
                NormalMode,
                form(injected[IntFormProvider]).fill(bondsStillHeld)
              )
            )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> bondsStillHeld.toString))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> bondsStillHeld.toString))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
