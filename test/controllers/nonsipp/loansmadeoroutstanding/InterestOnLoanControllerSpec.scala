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
import models.NormalMode
import pages.nonsipp.loansmadeoroutstanding.InterestOnLoanPage
import views.html.MultipleQuestionView

class InterestOnLoanControllerSpec extends ControllerBaseSpec {

  val maxAllowedAmount = 999999999.99

  "InterestOnLoanController" - {

    val schemeName = defaultSchemeDetails.schemeName

    lazy val viewModel = InterestOnLoanController.viewModel(srn, NormalMode, schemeName, _)

    lazy val onPageLoad = routes.InterestOnLoanController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.InterestOnLoanController.onSubmit(srn, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[MultipleQuestionView]
      view(viewModel(InterestOnLoanController.form))
    })

    act.like(renderPrePopView(onPageLoad, InterestOnLoanPage(srn), (money, percentage, money)) {
      implicit app => implicit request =>
        val view = injected[MultipleQuestionView]
        view(viewModel(InterestOnLoanController.form.fill((money, percentage, money))))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        "value.1" -> money.value.toString,
        "value.2" -> percentage.value.toString,
        "value.3" -> money.value.toString
      )
    )

    act.like(invalidForm(onSubmit))

    act.like(
      invalidForm(onSubmit, "value" -> (maxAllowedAmount + 0.001).toString)
        .withName("fail to submit when amount entered is greater than maximum allowed amount")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
