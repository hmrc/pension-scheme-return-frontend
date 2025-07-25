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

import services.SchemeDateService
import play.api.inject.guice.GuiceableModule
import models.ConditionalYesNo._
import play.api.mvc.Call
import models.IdentityType.Individual
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.PrePopCheckYourAnswersView
import config.Constants.incomplete
import models._
import pages.nonsipp.common.IdentityTypePage
import controllers.nonsipp.loansmadeoroutstanding.{routes => loansRoutes}
import utils.nonsipp.summary.LoansCheckAnswersUtils
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject.LoanRecipient

class LoansCheckAndUpdateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  private def onPageLoad: Call = loansRoutes.LoansCheckAndUpdateController.onPageLoad(srn, index1of5000.value)
  private def onSubmit: Call = loansRoutes.LoansCheckAndUpdateController.onSubmit(srn, index1of5000.value)

  private val prePopUserAnswers = defaultUserAnswers
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, index1of5000), recipientName)
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(DatePeriodLoanPage(srn, index1of5000), (localDate, money, loanPeriod))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index1of5000), true)
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Security](security))

  private val completedUserAnswers = prePopUserAnswers
    .unsafeSet(AmountOfTheLoanPage(srn, index1of5000), amountOfTheLoan)
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), interestOnLoan)
    .unsafeSet(ArrearsPrevYears(srn, index1of5000), false)
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Money](money))

  "LoansCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, prePopUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          controllers.nonsipp.loansmadeoroutstanding.LoansCheckAndUpdateController.viewModel(
            srn,
            index1of5000,
            LoansCheckAnswersUtils(mockSchemeDateService)
              .viewModel(
                srn,
                index1of5000,
                schemeName,
                Individual,
                recipientName,
                None,
                None,
                Left(true),
                (localDate, money, loanPeriod),
                Left(incomplete),
                dateRange.to,
                repaymentInstalments = true,
                Left(incomplete),
                Left(incomplete),
                Left(incomplete),
                Some(security),
                NormalMode,
                viewOnlyUpdated = true
              )
              .page
              .sections
          )
        )
      }.before(MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange))))
        .withName("render correct view when prePopulation data missing")
    )

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          controllers.nonsipp.loansmadeoroutstanding.LoansCheckAndUpdateController.viewModel(
            srn,
            index1of5000,
            LoansCheckAnswersUtils(mockSchemeDateService)
              .viewModel(
                srn,
                index1of5000,
                schemeName,
                Individual,
                recipientName,
                None,
                None,
                Left(true),
                (localDate, money, loanPeriod),
                Right(amountOfTheLoan),
                dateRange.to,
                repaymentInstalments = true,
                Right(interestOnLoan),
                Right(false),
                Right(Some(money)),
                Some(security),
                NormalMode,
                viewOnlyUpdated = true
              )
              .page
              .sections
          )
        )
      }.before(MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange))))
        .withName(s"render correct view when data complete")
    )

    act.like(
      redirectToPage(onSubmit, loansRoutes.AmountOfTheLoanController.onPageLoad(srn, index1of5000.value, NormalMode))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
