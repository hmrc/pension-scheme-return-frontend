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
import play.api.mvc.Call
import models.IdentityType.Individual
import controllers.ControllerBaseSpec
import views.html.ContentTablePageView
import models.{ConditionalYesNo, NormalMode, Security}
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject.LoanRecipient

class LoansCheckAndUpdateControllerSpec extends ControllerBaseSpec {

  private val conditionalYesSecurity: ConditionalYes[Security] = ConditionalYesNo.yes(security)

  private def onPageLoad: Call = routes.LoansCheckAndUpdateController.onPageLoad(srn, index1of5000)
  private def onSubmit: Call = routes.LoansCheckAndUpdateController.onSubmit(srn, index1of5000)

  private val prePopUserAnswers = defaultUserAnswers
    .unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, index1of5000), recipientName)
    .unsafeSet(IndividualRecipientNinoPage(srn, index1of5000), conditionalYesNoNino)
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(DatePeriodLoanPage(srn, index1of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index1of5000), partialAmountOfTheLoan)
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index1of5000), true)
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), partialInterestOnLoan)
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), conditionalYesSecurity)

  "LoansCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, prePopUserAnswers) { implicit app => implicit request =>
        injected[ContentTablePageView].apply(
          LoansCheckAndUpdateController.viewModel(
            srn = srn,
            index = index1of5000,
            recipientName = recipientName,
            dateOfTheLoan = localDate,
            amountOfTheLoan = money
          )
        )
      }.withName(s"render correct view")
    )

    act.like(
      redirectToPage(onSubmit, routes.AmountOfTheLoanController.onPageLoad(srn, index1of5000, NormalMode))
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .updateName("onPageLoad" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .updateName("onSubmit" + _)
    )
  }
}
