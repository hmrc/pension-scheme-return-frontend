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
import controllers.nonsipp.loansmadeoroutstanding.CompanyRecipientCrnController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Crn, NormalMode}
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientCrnPage, CompanyRecipientNamePage}
import views.html.ConditionalYesNoPageView

class CompanyRecipientCrnControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  private lazy val onPageLoad = routes.CompanyRecipientCrnController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.CompanyRecipientCrnController.onSubmit(srn, index, NormalMode)

  val userAnswersWithCompanyName = defaultUserAnswers.unsafeSet(CompanyRecipientNamePage(srn, index), companyName)

  val conditionalNo: ConditionalYesNo[Crn] = ConditionalYesNo[Crn](Left("reason"))
  val conditionalYes: ConditionalYesNo[Crn] = ConditionalYesNo[Crn](Right(crn))

  "CompanyRecipientCrnController" - {

    act.like(renderView(onPageLoad, userAnswersWithCompanyName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, companyName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        CompanyRecipientCrnPage(srn, index),
        conditionalNo,
        userAnswersWithCompanyName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, index, companyName, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> crn.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> crn.value))

    act.like(invalidForm(onSubmit, userAnswersWithCompanyName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
