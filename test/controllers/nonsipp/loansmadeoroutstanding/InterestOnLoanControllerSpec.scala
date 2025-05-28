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

package controllers.nonsipp.loansmadeoroutstanding

import models.ConditionalYesNo._
import controllers.ControllerBaseSpec
import views.html.MultipleQuestionView
import config.Constants.maxCurrencyValue
import models._
import controllers.nonsipp.loansmadeoroutstanding.InterestOnLoanController.partialAnswersForm
import pages.nonsipp.loansmadeoroutstanding._

class InterestOnLoanControllerSpec extends ControllerBaseSpec {

  private val partialUserAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), partialInterestOnLoan)

  private val prePopUserAnswersMissing: UserAnswers = defaultUserAnswers
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), conditionalYesNoSecurity)

  private val prePopUserAnswersCompleted: UserAnswers = defaultUserAnswers
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), conditionalYesNoSecurity)
    .unsafeSet(ArrearsPrevYears(srn, index1of5000), true)
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index1of5000), conditionalYesNoMoney)

  "InterestOnLoanController" - {

    val form = InterestOnLoanController.form()
    lazy val viewModel = InterestOnLoanController.viewModel(srn, index1of5000, NormalMode, schemeName, _)

    lazy val onPageLoad = routes.InterestOnLoanController.onPageLoad(srn, index1of5000.value, NormalMode)
    lazy val onSubmit = routes.InterestOnLoanController.onSubmit(srn, index1of5000.value, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[MultipleQuestionView]
      view(InterestOnLoanController.form(), viewModel(InterestOnLoanController.form()))
    })

    act.like(renderPrePopView(onPageLoad, InterestOnLoanPage(srn, index1of5000), interestOnLoan) {
      implicit app => implicit request =>
        val view = injected[MultipleQuestionView]
        view(form.fill((money, percentage, money)), viewModel(form))
    }.withName("return OK and the correct pre-populated view for a GET (full answers)"))

    act.like(renderViewWithPrePopSession(onPageLoad, partialUserAnswers) { implicit app => implicit request =>
      val view = injected[MultipleQuestionView]
      view(partialAnswersForm.fill((money, percentage, None)), viewModel(form))
    }.withName("return OK and the correct pre-populated view for a GET (partial answers only)"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        "value.1" -> money.value.toString,
        "value.2" -> percentage.value.toString,
        "value.3" -> money.value.toString
      )
    )

    act.like(
      redirectToPageWithPrePopSession(
        call = onSubmit,
        page = controllers.nonsipp.loansmadeoroutstanding.routes.OutstandingArrearsOnLoanController
          .onPageLoad(srn, index1of5000.value, NormalMode),
        userAnswers = prePopUserAnswersMissing,
        previousUserAnswers = defaultUserAnswers,
        mockSaveService = None,
        form = "value.1" -> money.value.toString,
        "value.2" -> percentage.value.toString,
        "value.3" -> money.value.toString
      ).withName("Skip next page when SecurityGivenForLoan is present and OutstandingArrearsOnLoan is empty (PrePop)")
    )

    act.like(
      redirectToPageWithPrePopSession(
        call = onSubmit,
        page = controllers.nonsipp.loansmadeoroutstanding.routes.SecurityGivenForLoanController
          .onPageLoad(srn, index1of5000.value, NormalMode),
        userAnswers = prePopUserAnswersCompleted,
        previousUserAnswers = defaultUserAnswers,
        mockSaveService = None,
        form = "value.1" -> money.value.toString,
        "value.2" -> percentage.value.toString,
        "value.3" -> money.value.toString
      ).withName("Don't skip next page when OutstandingArrearsOnLoan is present (PrePop)")
    )

    act.like(
      redirectToPageWithPrePopSession(
        call = onSubmit,
        page = controllers.nonsipp.loansmadeoroutstanding.routes.SecurityGivenForLoanController
          .onPageLoad(srn, index1of5000.value, NormalMode),
        userAnswers = defaultUserAnswers,
        previousUserAnswers = defaultUserAnswers,
        mockSaveService = None,
        form = "value.1" -> money.value.toString,
        "value.2" -> percentage.value.toString,
        "value.3" -> money.value.toString
      ).withName("Don't skip next page when SecurityGivenForLoan is empty (PrePop)")
    )

    act.like(invalidForm(onSubmit))

    act.like(
      invalidForm(onSubmit, "value" -> (maxCurrencyValue + 0.001).toString)
        .withName("fail to submit when amount entered is greater than maximum allowed amount")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
