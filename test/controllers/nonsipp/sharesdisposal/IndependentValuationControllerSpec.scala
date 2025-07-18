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

import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.YesNoPageView
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.IndependentValuationPage
import forms.YesNoPageFormProvider
import models.NormalMode
import controllers.nonsipp.sharesdisposal.IndependentValuationController._

class IndependentValuationControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val disposalIndex = 1
  private lazy val onPageLoad = routes.IndependentValuationController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = routes.IndependentValuationController.onSubmit(srn, index, disposalIndex, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)

  "IndependentValuationController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, companyName, index, disposalIndex, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, IndependentValuationPage(srn, index, disposalIndex), true, userAnswers) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, companyName, index, disposalIndex, NormalMode)
          )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
