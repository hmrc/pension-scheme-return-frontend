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

package controllers.nonsipp.sharesdisposal

import services.PsrSubmissionService
import pages.nonsipp.shares._
import config.Constants.{maxDisposalsPerShare, maxSharesTransactions}
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal.{HowWereSharesDisposedPage, IndependentValuationPage, SharesDisposalProgress}
import forms.YesNoPageFormProvider
import models.{NormalMode, ViewOnlyMode, ViewOnlyViewModel}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import controllers.nonsipp.sharesdisposal.ReportedSharesDisposalListController._
import play.api.inject.guice.GuiceableModule
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.ListView
import models.TypeOfShares._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject
import models.HowSharesDisposed._

class ReportedSharesDisposalListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReportedSharesDisposalListController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.ReportedSharesDisposalListController.onSubmit(srn, page, NormalMode)
  private lazy val sharesDisposalPage = routes.SharesDisposalController.onPageLoad(srn, NormalMode)

  private lazy val onSubmitViewOnly =
    controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPageLoadViewOnly =
    controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController.onPageLoadViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController.onPreviousViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val page = 1
  private val shareIndexOne = refineMV[Max5000.Refined](1)
  private val shareIndexTwo = refineMV[Max5000.Refined](2)
  private val disposalIndexOne = refineMV[Max50.Refined](1)
  private val disposalIndexTwo = refineMV[Max50.Refined](2)

  private val sharesTypeOne = SponsoringEmployer
  private val sharesTypeTwo = Unquoted

  private val howSharesDisposedOne = Sold
  private val howSharesDisposedTwo = Redeemed
  private val howSharesDisposedThree = Transferred
  private val howSharesDisposedFour = Other(otherDetails)

  private val disposalIndexes = List(disposalIndexOne, disposalIndexTwo)

  private val completedUserAnswers = defaultUserAnswers
  // Shares #1
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexOne), sharesTypeOne)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndexOne), companyName)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, disposalIndexOne), howSharesDisposedOne)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, disposalIndexTwo), howSharesDisposedTwo)
    .unsafeSet(SharesCompleted(srn, shareIndexOne), SectionCompleted)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, disposalIndexTwo), SectionJourneyStatus.Completed)
    // Shares #2
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexTwo), sharesTypeTwo)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndexTwo), companyName)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexTwo, disposalIndexOne), howSharesDisposedThree)
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexTwo, disposalIndexTwo), howSharesDisposedFour)
    .unsafeSet(SharesCompleted(srn, shareIndexTwo), SectionCompleted)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexTwo, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(SharesDisposalProgress(srn, shareIndexTwo, disposalIndexTwo), SectionJourneyStatus.Completed)

  private val noDisposalsUserAnswers = defaultUserAnswers
    .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
    // Shares Disposal #1
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexOne), sharesTypeOne)
    .unsafeSet(SharesCompleted(srn, shareIndexOne), SectionCompleted)
    // Shares Disposal #2
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndexOne), sharesTypeOne)
    .unsafeSet(SharesCompleted(srn, shareIndexOne), SectionCompleted)
    // fb / submission date
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private val sharesDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)] = List(
    ((shareIndexOne, disposalIndexes), SectionCompleted),
    ((shareIndexTwo, disposalIndexes), SectionCompleted)
  )
  private val numberOfSharesItems = sharesDisposalsWithIndexes.size

  private val numberOfDisposals = sharesDisposalsWithIndexes.map {
    case ((_, disposalIndexes), _) =>
      disposalIndexes.size
  }.sum

  private val maxPossibleNumberOfDisposals = maxDisposalsPerShare * numberOfSharesItems

  private val allSharesFullyDisposed = false

  private val maximumDisposalsReached =
    numberOfDisposals >= maxSharesTransactions * maxDisposalsPerShare ||
      numberOfDisposals >= maxPossibleNumberOfDisposals ||
      allSharesFullyDisposed

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "ReportedSharesDisposalListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          page,
          mode = NormalMode,
          sharesDisposalsWithIndexes,
          numberOfDisposals,
          maxPossibleNumberOfDisposals,
          completedUserAnswers,
          schemeName,
          viewOnlyViewModel = None,
          showBackLink = true,
          maximumDisposalsReached = maximumDisposalsReached,
          allSharesFullyDisposed = allSharesFullyDisposed
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        sharesDisposalPage,
        defaultUserAnswers
      ).withName("Not Started Journey")
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

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "ReportedSharesDisposalListController in view only mode" - {

    val currentUserAnswers = completedUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = completedUserAnswers
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
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                page,
                mode = ViewOnlyMode,
                sharesDisposalsWithIndexes,
                numberOfDisposals,
                maxPossibleNumberOfDisposals,
                completedUserAnswers,
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel),
                showBackLink = true,
                maximumDisposalsReached = maximumDisposalsReached,
                allSharesFullyDisposed = allSharesFullyDisposed
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(IndependentValuationPage(srn, shareIndexOne, disposalIndexOne), true)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                page,
                mode = ViewOnlyMode,
                sharesDisposalsWithIndexes,
                numberOfDisposals,
                maxPossibleNumberOfDisposals,
                completedUserAnswers,
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
                showBackLink = true,
                maximumDisposalsReached = maximumDisposalsReached,
                allSharesFullyDisposed = allSharesFullyDisposed
              )
            )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = noDisposalsUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView].apply(
          form(new YesNoPageFormProvider()),
          viewModel(
            srn,
            page,
            mode = ViewOnlyMode,
            List.empty,
            numberOfDisposals = 0,
            maxPossibleNumberOfDisposals,
            noDisposalsUserAnswers,
            schemeName,
            viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
            showBackLink = true,
            maximumDisposalsReached = maximumDisposalsReached,
            allSharesFullyDisposed = allSharesFullyDisposed
          )
        )
      }.withName("OnPageLoadViewOnly renders ok with no disposals")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only taskList")
    )

    act.like(
      renderView(
        onPreviousViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView].apply(
          form(new YesNoPageFormProvider()),
          viewModel(
            srn,
            page,
            mode = ViewOnlyMode,
            sharesDisposalsWithIndexes,
            numberOfDisposals,
            maxPossibleNumberOfDisposals,
            completedUserAnswers,
            schemeName,
            viewOnlyViewModel = Some(
              viewOnlyViewModel.copy(
                viewOnlyUpdated = false,
                currentVersion = (submissionNumberTwo - 1).max(0),
                previousVersion = (submissionNumberOne - 1).max(0)
              )
            ),
            showBackLink = false,
            maximumDisposalsReached = maximumDisposalsReached,
            allSharesFullyDisposed = allSharesFullyDisposed
          )
        )
      }.withName("OnPreviousViewOnly renders the view correctly")
    )

  }
}
