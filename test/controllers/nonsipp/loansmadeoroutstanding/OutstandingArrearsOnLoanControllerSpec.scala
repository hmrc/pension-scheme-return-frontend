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

package controllers.nonsipp.loansmadeoroutstanding

import models.ConditionalYesNo._
import controllers.nonsipp.loansmadeoroutstanding.OutstandingArrearsOnLoanController._
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import views.html.ConditionalYesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Money, NormalMode}
import pages.nonsipp.loansmadeoroutstanding.OutstandingArrearsOnLoanPage

class OutstandingArrearsOnLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.OutstandingArrearsOnLoanController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.OutstandingArrearsOnLoanController.onSubmit(srn, index, NormalMode)

  private val conditionalYes: ConditionalYes[Money] = ConditionalYesNo.yes(money)

  "OutstandingArrearsOnLoanController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, OutstandingArrearsOnLoanPage(srn, index), conditionalYes) {
      implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalYes.value),
            viewModel(srn, index, NormalMode)
          )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> "1"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> "1"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
