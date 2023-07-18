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

import config.Refined.OneTo9999999
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.ConditionalYesNo._
import models.{ConditionalYesNo, NormalMode, Security}
import pages.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanPage
import views.html.ConditionalYesNoPageView

class SecurityGivenForLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  private lazy val onPageLoad = routes.SecurityGivenForLoanController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.SecurityGivenForLoanController.onSubmit(srn, index, NormalMode)

  private val conditionalNo: ConditionalYes[Security] = ConditionalYesNo.no(())
  private val conditionalYes: ConditionalYes[Security] = ConditionalYesNo.yes(security)

  "SecurityGivenForLoanController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, SecurityGivenForLoanPage(srn, index), conditionalYes) {
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
