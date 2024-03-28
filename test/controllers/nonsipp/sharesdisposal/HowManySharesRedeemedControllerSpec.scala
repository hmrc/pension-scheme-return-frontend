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

package controllers.nonsipp.sharesdisposal

import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.IntView
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal.HowManySharesRedeemedPage
import forms.IntFormProvider
import models.NormalMode
import controllers.nonsipp.sharesdisposal.HowManySharesRedeemedController._

class HowManySharesRedeemedControllerSpec extends ControllerBaseSpec {

  private val shareIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.HowManySharesRedeemedController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.HowManySharesRedeemedController.onSubmit(srn, shareIndex, disposalIndex, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), companyName)

  "HowManySharesRedeemedController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[IntView].apply(
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
      renderPrePopView(onPageLoad, HowManySharesRedeemedPage(srn, shareIndex, disposalIndex), totalShares, userAnswers) {
        implicit app => implicit request =>
          injected[IntView]
            .apply(
              viewModel(
                srn,
                shareIndex,
                disposalIndex,
                companyName,
                NormalMode,
                form(injected[IntFormProvider]).fill(totalShares)
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
