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

import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, HowManySharesPage}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.IntView
import utils.IntUtils.given
import controllers.nonsipp.shares.HowManySharesController._
import forms.IntFormProvider
import models.NormalMode

class HowManySharesControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val populatedUserAnswers =
    defaultUserAnswers.unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)

  "HowManySharesController" - {

    lazy val onPageLoad = routes.HowManySharesController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.HowManySharesController.onSubmit(srn, index, NormalMode)

    act.like(
      renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
        injected[IntView].apply(
          form(injected[IntFormProvider]),
          viewModel(
            srn,
            index,
            companyName,
            NormalMode,
            form(injected[IntFormProvider])
          )
        )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, HowManySharesPage(srn, index), totalShares, populatedUserAnswers) {
        implicit app => implicit request =>
          injected[IntView].apply(
            form(injected[IntFormProvider]).fill(totalShares),
            viewModel(
              srn,
              index,
              companyName,
              NormalMode,
              form(injected[IntFormProvider])
            )
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, populatedUserAnswers, "value" -> totalShares.toString))
    act.like(invalidForm(onSubmit, populatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
