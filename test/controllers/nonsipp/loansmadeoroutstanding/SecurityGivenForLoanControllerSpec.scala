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

import controllers.ControllerBaseSpec
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode, Security}
import pages.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanPage
import views.html.ConditionalYesNoPageView
import SecurityGivenForLoanController._

class SecurityGivenForLoanControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.SecurityGivenForLoanController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.SecurityGivenForLoanController.onSubmit(srn, NormalMode)

//  val userAnswersWithCompanyName = defaultUserAnswers.unsafeSet(CompanyRecipientNamePage(srn), companyName)

  val conditionalNo: ConditionalYesNo[Security] = ConditionalYesNo[Security](Left("reason"))
  val conditionalYes: ConditionalYesNo[Security] = ConditionalYesNo[Security](Right(security))

  "SecurityGivenForLoanController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        SecurityGivenForLoanPage(srn),
        conditionalNo
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> security.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> security.value))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
