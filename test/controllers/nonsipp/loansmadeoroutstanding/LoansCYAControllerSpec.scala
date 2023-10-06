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

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.LoansCYAController._
import eu.timepit.refined.refineMV
import models.ConditionalYesNo._
import models.{
  CheckOrChange,
  ConditionalYesNo,
  Crn,
  IdentitySubject,
  IdentityType,
  Money,
  Security,
  SponsoringOrConnectedParty
}
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage}
import pages.nonsipp.loansmadeoroutstanding._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{PSRSubmissionService, SchemeDateService}
import views.html.CheckYourAnswersView

class LoansCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPSRSubmissionService: PSRSubmissionService = mock[PSRSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PSRSubmissionService].toInstance(mockPSRSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockSchemeDateService, mockPSRSubmissionService)

  private val index = refineMV[OneTo5000](1)
  private val taxYear = Some(Left(dateRange))
  private val subject = IdentitySubject.LoanRecipient

  private def onPageLoad(checkOrChange: CheckOrChange) = routes.LoansCYAController.onPageLoad(srn, index, checkOrChange)

  private def onSubmit(checkOrChange: CheckOrChange) = routes.LoansCYAController.onSubmit(srn, checkOrChange)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, index), recipientName)
    .unsafeSet(CompanyRecipientCrnPage(srn, index, subject), ConditionalYesNo.yes[String, Crn](crn))
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
                schemeName,
                IdentityType.UKCompany,
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
                checkOrChange
              )
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct ${checkOrChange.name} view")
      )

      act.like(
        redirectNextPage(onSubmit(checkOrChange))
          .before(MockPSRSubmissionService.submitPsrDetails())
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
