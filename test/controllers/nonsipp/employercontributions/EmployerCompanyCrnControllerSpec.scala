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

package controllers.nonsipp.employercontributions

import pages.nonsipp.employercontributions.{EmployerCompanyCrnPage, EmployerNamePage}
import config.Refined.{Max300, Max50}
import controllers.ControllerBaseSpec
import views.html.ConditionalYesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._

class EmployerCompanyCrnControllerSpec extends ControllerBaseSpec {

  private val memberIndex = refineMV[Max300.Refined](1) //memberIndex: Max300
  private val index = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.EmployerCompanyCrnController
      .onPageLoad(srn, memberIndex, index, NormalMode)
  private lazy val onSubmit =
    routes.EmployerCompanyCrnController
      .onSubmit(srn, memberIndex, index, NormalMode)

  val userAnswersCompanyName: UserAnswers =
    defaultUserAnswers.unsafeSet(EmployerNamePage(srn, memberIndex, index), companyName)

  val conditionalNo: ConditionalYesNo[String, Crn] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Crn] = ConditionalYesNo.yes(crn)

  "CompanyBuyerCrnController" - {

    act.like(renderView(onPageLoad, userAnswersCompanyName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(
          EmployerCompanyCrnController.form(injected[YesNoPageFormProvider]),
          EmployerCompanyCrnController.viewModel(srn, memberIndex, index, NormalMode, companyName)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        EmployerCompanyCrnPage(srn, memberIndex, index),
        conditionalNo,
        userAnswersCompanyName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            EmployerCompanyCrnController.form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            EmployerCompanyCrnController.viewModel(srn, memberIndex, index, NormalMode, companyName)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> crn.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> crn.value))

    act.like(invalidForm(onSubmit, userAnswersCompanyName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
