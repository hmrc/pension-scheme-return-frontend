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

package controllers.nonsipp.otherassetsdisposal

import services.PsrSubmissionService
import pages.nonsipp.otherassetsdisposal._
import views.html.ListView
import config.Constants.{maxDisposalPerOtherAsset, maxOtherAssetsTransactions}
import eu.timepit.refined.refineMV
import controllers.nonsipp.otherassetsdisposal.ReportedOtherAssetsDisposalListController._
import forms.YesNoPageFormProvider
import models.{NormalMode, ViewOnlyMode, ViewOnlyViewModel}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import models.HowDisposed.{Other, Sold, Transferred}
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject

class ReportedOtherAssetsDisposalListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.ReportedOtherAssetsDisposalListController.onSubmit(srn, page, NormalMode)
  private lazy val otherAssetDisposalPage = routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode)

  private lazy val onSubmitViewOnly =
    routes.ReportedOtherAssetsDisposalListController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPageLoadViewOnly =
    routes.ReportedOtherAssetsDisposalListController.onPageLoadViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    routes.ReportedOtherAssetsDisposalListController.onPreviousViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val page = 1
  private val otherAssetIndexOne = refineMV[Max5000.Refined](1)
  private val otherAssetIndexTwo = refineMV[Max5000.Refined](2)
  private val disposalIndexOne = refineMV[Max50.Refined](1)
  private val disposalIndexTwo = refineMV[Max50.Refined](2)

  private val howOtherAssetsDisposedOne = Sold
  private val howOtherAssetsDisposedTwo = Transferred
  private val howOtherAssetsDisposedThree = Other(otherDetails)

  private val disposalIndexes = List(disposalIndexOne, disposalIndexTwo)

  private val completedUserAnswers = defaultUserAnswers
  // Other Assets #1
    .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexOne), nameOfAsset)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexOne, disposalIndexOne), howOtherAssetsDisposedOne)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexOne, disposalIndexTwo), howOtherAssetsDisposedTwo)
    .unsafeSet(OtherAssetsCompleted(srn, otherAssetIndexOne), SectionCompleted)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexOne, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexOne, disposalIndexTwo), SectionJourneyStatus.Completed)
    // Other Assets #2
    .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexTwo), nameOfAsset)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexTwo, disposalIndexOne), howOtherAssetsDisposedTwo)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, otherAssetIndexTwo, disposalIndexTwo), howOtherAssetsDisposedThree)
    .unsafeSet(OtherAssetsCompleted(srn, otherAssetIndexTwo), SectionCompleted)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexTwo, disposalIndexOne), SectionJourneyStatus.Completed)
    .unsafeSet(OtherAssetsDisposalProgress(srn, otherAssetIndexTwo, disposalIndexTwo), SectionJourneyStatus.Completed)

  private val noDisposalsUserAnswers = defaultUserAnswers
    .unsafeSet(OtherAssetsDisposalPage(srn), false)
    // Other Assets #1
    .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexOne), nameOfAsset)
    .unsafeSet(OtherAssetsCompleted(srn, otherAssetIndexOne), SectionCompleted)
    // Other Assets #2
    .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexTwo), nameOfAsset)
    .unsafeSet(OtherAssetsCompleted(srn, otherAssetIndexTwo), SectionCompleted)
    // fb / submission date
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  private val otherAssetsDisposalsWithIndexes: List[((Max5000, List[Max50]), SectionCompleted)] = List(
    ((otherAssetIndexOne, disposalIndexes), SectionCompleted),
    ((otherAssetIndexTwo, disposalIndexes), SectionCompleted)
  )
  private val numberOfDisposals = otherAssetsDisposalsWithIndexes.map {
    case ((_, disposalIndexes), _) =>
      disposalIndexes.size
  }.sum

  private val numberOfOtherAssetsItems = otherAssetsDisposalsWithIndexes.size

  private val maxPossibleNumberOfDisposals = maxDisposalPerOtherAsset * numberOfOtherAssetsItems

  private val allAssetsFullyDisposed = false

  private val maximumDisposalsReached = numberOfDisposals >= maxOtherAssetsTransactions * maxDisposalPerOtherAsset ||
    numberOfDisposals >= maxPossibleNumberOfDisposals || allAssetsFullyDisposed

  "ReportedOtherAssetsDisposalListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          NormalMode,
          page,
          otherAssetsDisposalsWithIndexes,
          numberOfDisposals,
          maxPossibleNumberOfDisposals,
          completedUserAnswers,
          schemeName,
          viewOnlyViewModel = None,
          showBackLink = true,
          maximumDisposalsReached = maximumDisposalsReached,
          allAssetsFullyDisposed = allAssetsFullyDisposed
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        otherAssetDisposalPage,
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

  "ReportedOtherAssetsDisposalListController in view only mode" - {

    val currentUserAnswers = completedUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
      .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexOne), nameOfAsset)

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
              mode = ViewOnlyMode,
              page,
              otherAssetsDisposalsWithIndexes,
              numberOfDisposals,
              maxPossibleNumberOfDisposals,
              completedUserAnswers,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = false)),
              showBackLink = true,
              maximumDisposalsReached = maximumDisposalsReached,
              allAssetsFullyDisposed = allAssetsFullyDisposed
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated false")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(AssetSaleIndependentValuationPage(srn, otherAssetIndexOne, disposalIndexOne), true)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(new YesNoPageFormProvider()),
            viewModel(
              srn,
              mode = ViewOnlyMode,
              page,
              otherAssetsDisposalsWithIndexes,
              numberOfDisposals,
              maxPossibleNumberOfDisposals,
              completedUserAnswers,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true,
              maximumDisposalsReached = maximumDisposalsReached,
              allAssetsFullyDisposed = allAssetsFullyDisposed
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
            mode = ViewOnlyMode,
            page,
            List.empty,
            numberOfDisposals = 0,
            maxPossibleNumberOfDisposals,
            noDisposalsUserAnswers,
            schemeName,
            viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
            showBackLink = true,
            maximumDisposalsReached = false,
            allAssetsFullyDisposed = false
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
            mode = ViewOnlyMode,
            page,
            otherAssetsDisposalsWithIndexes,
            numberOfDisposals,
            maxPossibleNumberOfDisposals,
            completedUserAnswers,
            schemeName,
            viewOnlyViewModel = Some(
              viewOnlyViewModel.copy(
                currentVersion = submissionNumberOne,
                previousVersion = submissionNumberZero
              )
            ),
            showBackLink = false,
            maximumDisposalsReached = maximumDisposalsReached,
            allAssetsFullyDisposed = allAssetsFullyDisposed
          )
        )
      }.withName(
        "onPreviousViewOnly renders the view with the correct parameters for the previous submission"
      )
    )

  }

}
