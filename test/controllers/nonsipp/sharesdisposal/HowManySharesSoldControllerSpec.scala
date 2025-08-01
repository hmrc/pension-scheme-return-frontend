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

package controllers.nonsipp.sharesdisposal

import controllers.nonsipp.sharesdisposal.HowManySharesSoldController._
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.IntView
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.HowManySharesSoldPage
import forms.IntFormProvider
import models.NormalMode

class HowManySharesSoldControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val shareIndex = 1
  private val disposalIndex = 1

  private lazy val onPageLoad =
    routes.HowManySharesSoldController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.HowManySharesSoldController.onSubmit(srn, shareIndex, disposalIndex, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), companyName)

  "HowManySharesSoldController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[IntView].apply(
          form(injected[IntFormProvider]),
          viewModel(
            srn,
            shareIndex,
            disposalIndex,
            companyName,
            NormalMode,
            form(injected[IntFormProvider])
          )
        )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, HowManySharesSoldPage(srn, shareIndex, disposalIndex), totalShares, userAnswers) {
        implicit app => implicit request =>
          injected[IntView]
            .apply(
              form(injected[IntFormProvider]).fill(totalShares),
              viewModel(
                srn,
                shareIndex,
                disposalIndex,
                companyName,
                NormalMode,
                form(injected[IntFormProvider])
              )
            )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> totalShares.toString))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> totalShares.toString))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
