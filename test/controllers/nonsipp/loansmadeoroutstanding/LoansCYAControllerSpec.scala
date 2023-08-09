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
import controllers.nonsipp.loansmadeoroutstanding.LoansCYAController._
import eu.timepit.refined.refineMV
import models.ConditionalYesNo._
import models.{
  CheckOrChange,
  ConditionalYesNo,
  Crn,
  Money,
  NormalMode,
  ReceivedLoanType,
  Security,
  SponsoringOrConnectedParty
}
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding._
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

  private def onPageLoad(checkOrChange: CheckOrChange) = routes.LoansCYAController.onPageLoad(srn, index, checkOrChange)

  private def onSubmit(checkOrChange: CheckOrChange) = routes.LoansCYAController.onSubmit(srn, checkOrChange)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(IdentityTypePage(srn, index), ReceivedLoanType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, index), recipientName)
    .unsafeSet(CompanyRecipientCrnPage(srn, index), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), SponsoringOrConnectedParty.ConnectedParty)
    .unsafeSet(DatePeriodLoanPage(srn, index), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)
    .unsafeSet(InterestOnLoanPage(srn, index), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, index), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index), ConditionalYesNo.yes[Unit, Money](money))

  "LoansCYAController" - {
    List(CheckOrChange.Check, CheckOrChange.Change).foreach { checkOrChange =>
      act.like(
        renderView(onPageLoad(checkOrChange), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
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
                outstandingArrearsOnLoan = Some(money),
                securityOnLoan = Some(security),
                checkOrChange,
                NormalMode
              )
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct ${checkOrChange.name} view")
      )

      act.like(
        redirectNextPage(onSubmit(checkOrChange))
          .withName(s"redirect to next page when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(checkOrChange))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(checkOrChange))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${checkOrChange.name} mode")
      )
    }
  }
}
