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

import services.PsrSubmissionService
import org.mockito.Mockito._
import models.ConditionalYesNo._
import play.api.mvc.{AnyContent, Session}
import controllers.ControllerBaseSpec
import views.html.ListView
import config.Constants.PREPOPULATION_FLAG
import controllers.nonsipp.loansmadeoroutstanding.LoansListController._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models.{Security, _}
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage, PartnershipRecipientUtrPage}
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import models.SponsoringOrConnectedParty._
import models.requests.DataRequest

class LoansListControllerSpec extends ControllerBaseSpec {

  private val loanRecipient = IdentitySubject.LoanRecipient

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    // First loan:
    .unsafeSet(IdentityTypePage(srn, index1of5000, loanRecipient), IdentityType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, index1of5000), "recipientName1")
    .unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, loanRecipient), conditionalYesNoCrn)
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index1of5000), ConnectedParty)
    .unsafeSet(DatePeriodLoanPage(srn, index1of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index1of5000), amountOfTheLoan)
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index1of5000), false)
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), interestOnLoan)
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), conditionalYesNoSecurity)
    .unsafeSet(ArrearsPrevYears(srn, index1of5000), true)
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index1of5000), conditionalYesNoMoney)
    .unsafeSet(LoanCompleted(srn, index1of5000), SectionCompleted)
    .unsafeSet(LoansProgress(srn, index1of5000), SectionJourneyStatus.Completed)
    // Second loan:
    .unsafeSet(IdentityTypePage(srn, index2of5000, loanRecipient), IdentityType.UKPartnership)
    .unsafeSet(PartnershipRecipientNamePage(srn, index2of5000), "recipientName2")
    .unsafeSet(PartnershipRecipientUtrPage(srn, index2of5000, loanRecipient), conditionalYesNoUtr)
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index2of5000), Neither)
    .unsafeSet(DatePeriodLoanPage(srn, index2of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index2of5000), amountOfTheLoan)
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index2of5000), false)
    .unsafeSet(InterestOnLoanPage(srn, index2of5000), interestOnLoan)
    .unsafeSet(SecurityGivenForLoanPage(srn, index2of5000), ConditionalYesNo.no[Unit, Security](()))
    .unsafeSet(ArrearsPrevYears(srn, index2of5000), false)
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index2of5000), ConditionalYesNo.no[Unit, Money](()))
    .unsafeSet(LoanCompleted(srn, index2of5000), SectionCompleted)
    .unsafeSet(LoansProgress(srn, index1of5000), SectionJourneyStatus.Completed)

  private val userAnswersToCheck = completedUserAnswers
    .unsafeSet(IdentityTypePage(srn, index3of5000, loanRecipient), IdentityType.Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, index3of5000), "recipientName3")
    .unsafeSet(IndividualRecipientNinoPage(srn, index3of5000), conditionalYesNoNino)
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, index3of5000), false)
    .unsafeSet(DatePeriodLoanPage(srn, index3of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index3of5000), partialAmountOfTheLoan)
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index3of5000), false)
    .unsafeSet(InterestOnLoanPage(srn, index3of5000), partialInterestOnLoan)
    .unsafeSet(SecurityGivenForLoanPage(srn, index3of5000), conditionalYesNoSecurity)
    .unsafeSet(LoanCompleted(srn, index3of5000), SectionCompleted)
    .unsafeSet(LoansProgress(srn, index1of5000), SectionJourneyStatus.Completed)

  private val page = 1

  private lazy val onPageLoad = routes.LoansListController.onPageLoad(srn, page, NormalMode)

  private lazy val onSubmit = routes.LoansListController.onSubmit(srn, page, NormalMode)

  private lazy val onPageLoadTaskListController = controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

  private val loansData: List[LoansData] = List(
    LoansData(
      index1of5000,
      money,
      "recipientName1",
      SectionJourneyStatus.Completed
    ),
    LoansData(
      index2of5000,
      money,
      "recipientName2",
      SectionJourneyStatus.Completed
    )
  )

  private val loansDataToCheck: List[LoansData] = List(
    LoansData(
      index3of5000,
      money,
      "recipientName3",
      SectionJourneyStatus.Completed
    )
  )

  private val loansDataChanged: List[LoansData] = List(
    LoansData(
      index1of5000,
      money,
      "changedRecipientName",
      SectionJourneyStatus.Completed
    ),
    LoansData(
      index2of5000,
      money,
      "recipientName2",
      SectionJourneyStatus.Completed
    )
  )

  private val viewOnlyViewModel = ViewOnlyViewModel(
    viewOnlyUpdated = false,
    year = yearString,
    currentVersion = submissionNumberTwo,
    previousVersion = submissionNumberOne,
    compilationOrSubmissionDate = Some(submissionDateTwo)
  )

  private lazy val onSubmitViewOnly = routes.LoansListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.LoansListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPreviousViewOnly = routes.LoansListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnlyTaskListController = controllers.nonsipp.routes.ViewOnlyTaskListController
    .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)

  private val currentUserAnswers = completedUserAnswers
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private val previousUserAnswers = currentUserAnswers
    .unsafeSet(FbVersionPage(srn), "001")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

  private val updatedUserAnswers = currentUserAnswers
    .unsafeSet(CompanyRecipientNamePage(srn, index1of5000), "changedRecipientName")

  private val noLoansUserAnswers = defaultUserAnswers
    .unsafeSet(LoansMadeOrOutstandingPage(srn), false)
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val mockReq = mock[DataRequest[AnyContent]]

  when(mockReq.session).thenReturn(Session(Map(PREPOPULATION_FLAG -> "true")))

  "LoansListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn = srn,
          page = page,
          mode = NormalMode,
          loansNotToCheck = loansData,
          loansToCheck = Nil,
          schemeName = schemeName,
          viewOnlyViewModel = None,
          showBackLink = true,
          isPrePop = false
        )(mockReq)
      )
    }.withName("Completed Journey"))

    act.like(renderViewWithPrePopSession(onPageLoad, userAnswersToCheck) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn = srn,
          page = page,
          mode = NormalMode,
          loansNotToCheck = loansData,
          loansToCheck = loansDataToCheck,
          schemeName = schemeName,
          viewOnlyViewModel = None,
          showBackLink = true,
          isPrePop = true
        )(mockReq)
      )
    }.withName("PrePop Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        onPageLoadTaskListController,
        defaultUserAnswers
      ).withName("Redirect to Task List when 0 Loans completed and not in ViewOnly mode")
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
    )

    act.like(invalidForm(onSubmit, defaultUserAnswers))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "LoansListController in view only mode" - {

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn = srn,
              page = page,
              mode = ViewOnlyMode,
              loansNotToCheck = loansData,
              loansToCheck = Nil,
              schemeName = schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel),
              showBackLink = true,
              isPrePop = false
            )(mockReq)
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn = srn,
              page = page,
              mode = ViewOnlyMode,
              loansNotToCheck = loansDataChanged,
              loansToCheck = Nil,
              schemeName = schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true,
              isPrePop = false
            )(mockReq)
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noLoansUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(new YesNoPageFormProvider()),
            viewModel(
              srn = srn,
              page = page,
              mode = ViewOnlyMode,
              loansNotToCheck = List(),
              loansToCheck = Nil,
              schemeName = schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true,
              isPrePop = false
            )(mockReq)
          )
      }.withName("OnPageLoadViewOnly renders ok with no loans")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        onPageLoadViewOnlyTaskListController
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only tasklist")
    )

    act.like(
      renderView(
        onPreviousViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView]
          .apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn = srn,
              page = page,
              mode = ViewOnlyMode,
              loansNotToCheck = loansData,
              loansToCheck = Nil,
              schemeName = schemeName,
              viewOnlyViewModel = Some(
                viewOnlyViewModel.copy(
                  currentVersion = (submissionNumberTwo - 1).max(0),
                  previousVersion = (submissionNumberOne - 1).max(0)
                )
              ),
              showBackLink = false,
              isPrePop = false
            )(mockReq)
          )
      }.withName("OnPreviousViewOnly renders the view correctly")
    )
  }
}
