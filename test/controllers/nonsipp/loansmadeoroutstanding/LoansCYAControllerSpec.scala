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
import utils.nonsipp.summary.LoansCheckAnswersUtils
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.CheckYourAnswersView
import utils.IntUtils.given
import pages.nonsipp.FbVersionPage
import models._
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

class LoansCYAControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

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

  private val index = 1
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

  private val filledUserAnswersCompanyConnectedParty = defaultUserAnswers
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, index), recipientName)
    .unsafeSet(CompanyRecipientCrnPage(srn, index, subject), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), SponsoringOrConnectedParty.ConnectedParty)
    .unsafeSet(DatePeriodLoanPage(srn, index), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index), amountOfTheLoan)
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)
    .unsafeSet(InterestOnLoanPage(srn, index), interestOnLoan)
    .unsafeSet(SecurityGivenForLoanPage(srn, index), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(ArrearsPrevYears(srn, index), true)
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index), ConditionalYesNo.yes[Unit, Money](money))
    .unsafeSet(LoanCompleted(srn, index), SectionCompleted)
    .unsafeSet(LoansProgress(srn, index), SectionJourneyStatus.Completed)

  private val filledUserAnswersIndividualSponsoring = filledUserAnswersCompanyConnectedParty
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, index), recipientName)
    .unsafeSet(IndividualRecipientNinoPage(srn, index), conditionalYesNoNino)
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, index), false)

  private val filledUserAnswersPartnershipNeither = filledUserAnswersCompanyConnectedParty
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKPartnership)
    .unsafeSet(PartnershipRecipientNamePage(srn, index), recipientName)
    .unsafeSet(PartnershipRecipientUtrPage(srn, index, subject), conditionalYesNoUtr)
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), SponsoringOrConnectedParty.Neither)

  private val filledUserAnswersOtherSponsoring = filledUserAnswersCompanyConnectedParty
    .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Other)
    .unsafeSet(
      OtherRecipientDetailsPage(srn, index, subject),
      RecipientDetails(recipientName, otherRecipientDescription)
    )
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), SponsoringOrConnectedParty.Sponsoring)

  "LoansCYAController" - {
    List(NormalMode, CheckMode).foreach { mode =>

      List(
        (
          filledUserAnswersCompanyConnectedParty,
          Right(SponsoringOrConnectedParty.ConnectedParty),
          IdentityType.UKCompany,
          crn.crn
        ),
        (filledUserAnswersIndividualSponsoring, Left(false), IdentityType.Individual, nino.value),
        (
          filledUserAnswersPartnershipNeither,
          Right(SponsoringOrConnectedParty.Neither),
          IdentityType.UKPartnership,
          utr.value
        ),
        (
          filledUserAnswersOtherSponsoring,
          Right(SponsoringOrConnectedParty.Sponsoring),
          IdentityType.Other,
          otherRecipientDescription
        )
      ).foreach { (filledAnswers, sponsoringOrConnected, identityType, description) =>
        act.like(
          renderView(onPageLoad(mode), filledAnswers) { implicit app => implicit request =>
            injected[CheckYourAnswersView].apply(
              LoansCheckAnswersUtils(mockSchemeDateService).viewModel(
                srn,
                index,
                schemeName,
                identityType,
                recipientName,
                recipientDetails = Some(description),
                recipientReasonNoDetails = None,
                connectedParty = sponsoringOrConnected,
                datePeriodLoan = (localDate, money, loanPeriod),
                amountOfTheLoan = Right(amountOfTheLoan),
                returnEndDate = dateRange.to,
                repaymentInstalments = true,
                interestOnLoan = Right(interestOnLoan),
                arrearsPrevYears = Right(true),
                outstandingArrearsOnLoan = Right(Some(money)),
                securityOnLoan = Some(security),
                mode,
                viewOnlyUpdated = true
              )
            )
          }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
            .withName(s"render correct $mode view for $identityType")
        )
      }

      act.like(
        redirectToPage(
          call = onPageLoad(mode),
          page = routes.LoansListController.onPageLoad(srn, 1, mode),
          userAnswers = filledUserAnswersCompanyConnectedParty
            .unsafeSet(LoansProgress(srn, index), SectionJourneyStatus.InProgress(anyUrl)),
          previousUserAnswers = emptyUserAnswers
        ).withName(s"Redirect to loans list when incomplete when in $mode mode")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .after {
            verify(mockPsrSubmissionService, times(1))
              .submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
            reset(mockPsrSubmissionService)
          }
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
      .unsafeSet(AmountOfTheLoanPage(srn, index), amountOfTheLoan)
      .unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)
      .unsafeSet(InterestOnLoanPage(srn, index), interestOnLoan)
      .unsafeSet(SecurityGivenForLoanPage(srn, index), ConditionalYesNo.yes[Unit, Security](security))
      .unsafeSet(ArrearsPrevYears(srn, index), false)
      .unsafeSet(OutstandingArrearsOnLoanPage(srn, index), ConditionalYesNo.no[Unit, Money](()))
      .unsafeSet(LoanCompleted(srn, index), SectionCompleted)
      .unsafeSet(LoansProgress(srn, index), SectionJourneyStatus.Completed)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)
      .unsafeSet(CompanyRecipientNamePage(srn, index), recipientName)
      .unsafeSet(CompanyRecipientCrnPage(srn, index, subject), ConditionalYesNo.yes[String, Crn](crn))
      .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), SponsoringOrConnectedParty.ConnectedParty)
      .unsafeSet(DatePeriodLoanPage(srn, index), (localDate, money, loanPeriod))
      .unsafeSet(AmountOfTheLoanPage(srn, index), amountOfTheLoan)
      .unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)
      .unsafeSet(InterestOnLoanPage(srn, index), interestOnLoan)
      .unsafeSet(SecurityGivenForLoanPage(srn, index), ConditionalYesNo.yes[Unit, Security](security))
      .unsafeSet(ArrearsPrevYears(srn, index), true)
      .unsafeSet(OutstandingArrearsOnLoanPage(srn, index), ConditionalYesNo.yes[Unit, Money](money))
      .unsafeSet(LoanCompleted(srn, index), SectionCompleted)
      .unsafeSet(LoansProgress(srn, index), SectionJourneyStatus.Completed)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            LoansCheckAnswersUtils(mockSchemeDateService).viewModel(
              srn,
              index,
              schemeName,
              IdentityType.UKCompany,
              recipientName,
              recipientDetails = Some(crn.value),
              recipientReasonNoDetails = None,
              connectedParty = Right(SponsoringOrConnectedParty.ConnectedParty),
              datePeriodLoan = (localDate, money, loanPeriod),
              amountOfTheLoan = Right(amountOfTheLoan),
              returnEndDate = dateRange.to,
              repaymentInstalments = true,
              interestOnLoan = Right(interestOnLoan),
              arrearsPrevYears = Right(false),
              outstandingArrearsOnLoan = Right(None),
              securityOnLoan = Some(security),
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne)
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
        verify(mockPsrSubmissionService, never()).submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to loans list page")
    )
  }
}
