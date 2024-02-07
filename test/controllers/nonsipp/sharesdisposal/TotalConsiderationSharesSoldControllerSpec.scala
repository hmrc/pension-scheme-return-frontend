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

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import controllers.nonsipp.sharesdisposal.TotalConsiderationSharesSoldController._
import forms.mappings.errors.MoneyFormErrorProvider
import models.NormalMode
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import pages.nonsipp.sharesdisposal.{HowManySharesSoldPage, TotalConsiderationSharesSoldPage}
import views.html.MoneyView

class TotalConsiderationSharesSoldControllerSpec extends ControllerBaseSpec {

  private val shareIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.TotalConsiderationSharesSoldController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.TotalConsiderationSharesSoldController.onSubmit(srn, shareIndex, disposalIndex, NormalMode)

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), companyName)
      .unsafeSet(HowManySharesSoldPage(srn, shareIndex, disposalIndex), totalShares)

  "TotalConsiderationSharesSoldController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[MoneyView]
          .apply(
            viewModel(
              srn,
              shareIndex,
              disposalIndex,
              totalShares,
              companyName,
              form(injected[MoneyFormErrorProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex), money, userAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            viewModel(
              srn,
              shareIndex,
              disposalIndex,
              totalShares,
              companyName,
              form(injected[MoneyFormErrorProvider]).fill(money),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "1"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
