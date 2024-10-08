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

import services.{PsrSubmissionService, SchemeDateService}
import models.ConditionalYesNo._
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage}
import pages.nonsipp.loansmadeoroutstanding._
import controllers.nonsipp.loansmadeoroutstanding.LoansCYAController._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

class LoansCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit = {
    reset(mockSchemeDateService)
    reset(mockPsrSubmissionService)
  }

  private val index = refineMV[OneTo5000](1)
  private val page = 1
  private val taxYear = Some(Left(dateRange))
  private val subject = IdentitySubject.LoanRecipient

  private def onPageLoad(mode: Mode) = routes.LoansCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) = routes.LoansCYAController.onSubmit(srn, index, mode)

  private lazy val onPageLoadViewOnly = routes.LoansCYAController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onSubmitViewOnly = routes.LoansCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

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
    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
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
              mode,
              viewOnlyUpdated = true
            )
          )
        }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
          .withName(s"render correct $mode view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPsrSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in ${mode.toString} mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${mode.toString} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${mode.toString} mode")
      )
    }
  }

  "LoansCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
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

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
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

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
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
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to loans list page")
    )
  }
}
