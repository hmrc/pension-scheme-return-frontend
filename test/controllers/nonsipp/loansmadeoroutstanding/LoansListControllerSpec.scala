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
import config.Refined.{Max5000, OneTo5000}
import controllers.ControllerBaseSpec
import views.html.ListView
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage, PartnershipRecipientUtrPage}
import pages.nonsipp.loansmadeoroutstanding._
import eu.timepit.refined.api.Refined
import org.mockito.ArgumentMatchers.any
import controllers.nonsipp.loansmadeoroutstanding.LoansListController._
import eu.timepit.refined.refineMV
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import uk.gov.hmrc.domain.Nino

class LoansListControllerSpec extends ControllerBaseSpec {
  private val subject = IdentitySubject.LoanRecipient

  val indexOne: Refined[Int, OneTo5000] = refineMV[OneTo5000](1)
  val indexTwo: Refined[Int, OneTo5000] = refineMV[OneTo5000](2)
  val indexThree: Refined[Int, OneTo5000] = refineMV[OneTo5000](3)

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(IdentityTypePage(srn, indexOne, subject), IdentityType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, indexOne), "recipientName1")
    .unsafeSet(
      CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LoanRecipient),
      ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
    )
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, indexOne), SponsoringOrConnectedParty.ConnectedParty)
    .unsafeSet(DatePeriodLoanPage(srn, indexOne), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, indexOne), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, indexOne), false)
    .unsafeSet(InterestOnLoanPage(srn, indexOne), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, indexOne), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, indexOne), ConditionalYesNo.yes[Unit, Money](money))
    .unsafeSet(IdentityTypePage(srn, indexTwo, subject), IdentityType.UKPartnership)
    .unsafeSet(PartnershipRecipientNamePage(srn, indexTwo), "recipientName2")
    .unsafeSet(
      PartnershipRecipientUtrPage(srn, indexTwo, IdentitySubject.LoanRecipient),
      ConditionalYesNo.yes[String, Utr](utrGen.sample.value)
    )
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, indexTwo), SponsoringOrConnectedParty.Neither)
    .unsafeSet(DatePeriodLoanPage(srn, indexTwo), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, indexTwo), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, indexTwo), false)
    .unsafeSet(InterestOnLoanPage(srn, indexTwo), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, indexTwo), ConditionalYesNo.no[Unit, Security](()))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, indexTwo), ConditionalYesNo.no[Unit, Money](()))
    .unsafeSet(IdentityTypePage(srn, indexThree, subject), IdentityType.Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, indexThree), "recipientName3")
    .unsafeSet(IndividualRecipientNinoPage(srn, indexThree), ConditionalYesNo.yes[String, Nino](ninoGen.sample.value))
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, indexThree), false)
    .unsafeSet(DatePeriodLoanPage(srn, indexThree), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, indexThree), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, indexThree), false)
    .unsafeSet(InterestOnLoanPage(srn, indexThree), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, indexThree), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, indexThree), ConditionalYesNo.yes[Unit, Money](money))

  private val inProgressUserAnswers = defaultUserAnswers
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(IdentityTypePage(srn, indexOne, subject), IdentityType.Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, indexOne), "recipientName3")
    .unsafeSet(IndividualRecipientNinoPage(srn, indexOne), ConditionalYesNo.yes[String, Nino](ninoGen.sample.value))
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, indexOne), false)
    .unsafeSet(DatePeriodLoanPage(srn, indexOne), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, indexOne), (money, money, money))

  private lazy val onPageLoad = routes.LoansListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.LoansListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onLoansMadePageLoad = routes.LoansMadeOrOutstandingController.onPageLoad(srn, NormalMode)

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
    submissionNumberOne,
    showBackLink = true
  )
  private lazy val onPreviousViewOnly = routes.LoansListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val page = 1

  private val recipients: List[(Max5000, String, Money)] = List(
    (indexOne, "recipientName1", money),
    (indexTwo, "recipientName2", money),
    (indexThree, "recipientName3", money)
  )

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private val noLoansUserAnswers = defaultUserAnswers
    .unsafeSet(LoansMadeOrOutstandingPage(srn), false)
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  "LoansListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          1,
          NormalMode,
          recipients,
          schemeName,
          showBackLink = true
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.nonsipp.common.routes.IdentityTypeController
          .onPageLoad(srn, indexOne, NormalMode, IdentitySubject.LoanRecipient),
        inProgressUserAnswers
      ).withName("Incomplete Journey")
    )

    act.like(
      redirectToPage(
        onPageLoad,
        onLoansMadePageLoad,
        defaultUserAnswers
      ).withName("Not Started Journey")
    )

    act.like(
      redirectToPage(
        onPageLoad,
        onLoansMadePageLoad,
        noLoansUserAnswers
      ).withName("No loans added redirects to yes-no page")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "LoansListController in view only mode" - {
    val currentUserAnswers = completedUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    val viewOnlyViewModel = ViewOnlyViewModel(
      viewOnlyUpdated = false,
      year = yearString,
      currentVersion = submissionNumberTwo,
      previousVersion = submissionNumberOne,
      compilationOrSubmissionDate = Some(submissionDateTwo)
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              recipients,
              schemeName,
              Some(viewOnlyViewModel),
              showBackLink = true
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(AreRepaymentsInstalmentsPage(srn, indexOne), true)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              recipients,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noLoansUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(new YesNoPageFormProvider()),
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              List(),
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no loans")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only tasklist")
    )

    act.like(
      redirectToPage(
        onPreviousViewOnly,
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero, showBackLink = false)
      ).withName(
        "Submit previous view only redirects to the controller with parameters for the previous submission"
      )
    )
  }
}
