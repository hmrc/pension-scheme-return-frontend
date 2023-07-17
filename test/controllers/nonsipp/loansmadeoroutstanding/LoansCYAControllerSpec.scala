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
import controllers.nonsipp.loansmadeoroutstanding.LoansCYAController._
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn, NormalMode, ReceivedLoanType, SponsoringOrConnectedParty}
import pages.nonsipp.loansmadeoroutstanding.{
  AmountOfTheLoanPage,
  AreRepaymentsInstalmentsPage,
  CompanyRecipientCrnPage,
  CompanyRecipientNamePage,
  DatePeriodLoanPage,
  InterestOnLoanPage,
  RecipientSponsoringEmployerConnectedPartyPage,
  WhoReceivedLoanPage
}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.CheckYourAnswersView

class LoansCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockSchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override protected def beforeAll(): Unit =
    reset(mockSchemeDateService)

  private val index = refineMV[OneTo9999999](1)
  private val taxYear = Some(Left(dateRange))

  private lazy val onPageLoad = routes.LoansCYAController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LoansCYAController.onSubmit(srn, NormalMode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(WhoReceivedLoanPage(srn, index), ReceivedLoanType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, index), recipientName)
    .unsafeSet(CompanyRecipientCrnPage(srn, index), ConditionalYesNo.yes(crn))
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), SponsoringOrConnectedParty.ConnectedParty)
    .unsafeSet(DatePeriodLoanPage(srn, index), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)
    .unsafeSet(InterestOnLoanPage(srn, index), (money, percentage, money))

  "LoansCYAController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(
          srn,
          index,
          ReceivedLoanType.UKCompany,
          recipientName,
          recipientDetails = Some(crn.value),
          recipientReasonNoDetails = None,
          connectedParty = Right(SponsoringOrConnectedParty.ConnectedParty),
          datePeriodLoan = (localDate, money, loanPeriod),
          loanAmount = (money, money, money),
          returnEndDate = dateRange.to,
          repaymentInstalments = true,
          loanInterest = (money, percentage, money),
          NormalMode
        )
      )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
