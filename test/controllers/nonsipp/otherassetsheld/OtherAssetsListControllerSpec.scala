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

package controllers.nonsipp.otherassetsheld

import services.PsrSubmissionService
import org.mockito.Mockito._
import pages.nonsipp.otherassetsheld._
import models.IdentityType.Individual
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ListView
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import controllers.nonsipp.otherassetsheld.OtherAssetsListController._
import pages.nonsipp.common.IdentityTypePage
import models.IdentitySubject.OtherAssetSeller
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import models._
import models.SchemeHoldAsset._

class OtherAssetsListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(OtherAssetsHeldPage(srn), true)
    // First Other Assets record:
    .unsafeSet(WhatIsOtherAssetPage(srn, index1of5000), "nameOfOtherAsset1")
    .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index1of5000), true)
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index1of5000), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, OtherAssetSeller), Individual)
    .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index1of5000), name)
    .unsafeSet(OtherAssetIndividualSellerNINumberPage(srn, index1of5000), conditionalYesNoNino)
    .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(CostOfOtherAssetPage(srn, index1of5000), money)
    .unsafeSet(IndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(IncomeFromAssetPage(srn, index1of5000), money)
    .unsafeSet(OtherAssetsCompleted(srn, index1of5000), SectionCompleted)
    .unsafeSet(OtherAssetsProgress(srn, index1of5000), SectionJourneyStatus.Completed)
    // Second Other Assets record:
    .unsafeSet(WhatIsOtherAssetPage(srn, index2of5000), "nameOfOtherAsset2")
    .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index2of5000), false)
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index2of5000), Contribution)
    .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index2of5000), localDate)
    .unsafeSet(CostOfOtherAssetPage(srn, index2of5000), money)
    .unsafeSet(IndependentValuationPage(srn, index2of5000), false)
    .unsafeSet(IncomeFromAssetPage(srn, index2of5000), money)
    .unsafeSet(OtherAssetsCompleted(srn, index2of5000), SectionCompleted)
    .unsafeSet(OtherAssetsProgress(srn, index2of5000), SectionJourneyStatus.Completed)

  private val userAnswersToCheck = completedUserAnswers
    .unsafeSet(WhatIsOtherAssetPage(srn, index3of5000), "nameOfOtherAsset3")
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index3of5000), Transfer)
    .unsafeSet(CostOfOtherAssetPage(srn, index3of5000), money)
    .unsafeSet(OtherAssetsCompleted(srn, index3of5000), SectionCompleted)
    .unsafeSet(OtherAssetsProgress(srn, index3of5000), SectionJourneyStatus.Completed)
    .unsafeSet(OtherAssetsPrePopulated(srn, index3of5000), false)

  private val userAnswersChecked = completedUserAnswers
    .unsafeSet(WhatIsOtherAssetPage(srn, index3of5000), "otherAssetsChecked")
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index3of5000), Transfer)
    .unsafeSet(CostOfOtherAssetPage(srn, index3of5000), money)
    .unsafeSet(OtherAssetsCompleted(srn, index3of5000), SectionCompleted)
    .unsafeSet(OtherAssetsProgress(srn, index3of5000), SectionJourneyStatus.Completed)
    .unsafeSet(OtherAssetsPrePopulated(srn, index3of5000), true)

  private val userAnswersWithIncompleteRecord = completedUserAnswers
    .unsafeSet(WhatIsOtherAssetPage(srn, index4of5000), "nameOfOtherAsset3")
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index4of5000), Transfer)
    .unsafeSet(
      OtherAssetsProgress(srn, index4of5000),
      SectionJourneyStatus.InProgress(CostOfOtherAssetPage(srn, index4of5000))
    )

  private val userAnswersToCheckWithIncompleteRecord = userAnswersToCheck
    .unsafeSet(WhatIsOtherAssetPage(srn, index4of5000), "nameOfOtherAsset3")
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index4of5000), Transfer)
    .unsafeSet(
      OtherAssetsProgress(srn, index4of5000),
      SectionJourneyStatus.InProgress(CostOfOtherAssetPage(srn, index4of5000))
    )
    .unsafeSet(OtherAssetsPrePopulated(srn, index4of5000), false)

  private val page = 1

  private lazy val onPageLoad = routes.OtherAssetsListController.onPageLoad(srn, page, NormalMode)

  private lazy val onSubmit = routes.OtherAssetsListController.onSubmit(srn, page, NormalMode)

  private lazy val onPageLoadTaskListController = controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

  private val otherAssetsData: List[OtherAssetsData] = List(
    OtherAssetsData(
      index1of5000,
      "nameOfOtherAsset1",
      true
    ),
    OtherAssetsData(
      index2of5000,
      "nameOfOtherAsset2",
      true
    )
  )

  private val otherAssetsDataToCheck: List[OtherAssetsData] = List(
    OtherAssetsData(
      index3of5000,
      "nameOfOtherAsset3",
      false
    )
  )

  private val otherAssetsDataChanged: List[OtherAssetsData] = List(
    OtherAssetsData(
      index1of5000,
      "changedNameOfOtherAsset",
      true
    ),
    OtherAssetsData(
      index2of5000,
      "nameOfOtherAsset2",
      true
    )
  )

  private val otherAssetsDataChecked: List[OtherAssetsData] = List(
    OtherAssetsData(
      index3of5000,
      "otherAssetsChecked",
      false
    )
  )

  private val viewOnlyViewModel = ViewOnlyViewModel(
    viewOnlyUpdated = false,
    year = yearString,
    currentVersion = submissionNumberTwo,
    previousVersion = submissionNumberOne,
    compilationOrSubmissionDate = Some(submissionDateTwo)
  )

  private lazy val onSubmitViewOnly = routes.OtherAssetsListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.OtherAssetsListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.OtherAssetsListController.onPreviousViewOnly(
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
    .unsafeSet(WhatIsOtherAssetPage(srn, index1of5000), "changedNameOfOtherAsset")

  private val noOtherAssetsUserAnswers = defaultUserAnswers
    .unsafeSet(OtherAssetsHeldPage(srn), false)
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  "OtherAssetsListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn = srn,
            page = page,
            mode = NormalMode,
            otherAssets = otherAssetsData,
            otherAssetsToCheck = Nil,
            schemeName = schemeName,
            viewOnlyViewModel = None,
            showBackLink = true,
            isPrePop = false
          )
        )
    }.withName("Completed Journey"))

    act.like(renderView(onPageLoad, userAnswersWithIncompleteRecord) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn = srn,
            page = page,
            mode = NormalMode,
            otherAssets = otherAssetsData,
            otherAssetsToCheck = Nil,
            schemeName = schemeName,
            viewOnlyViewModel = None,
            showBackLink = true,
            isPrePop = false
          )
        )
    }.withName("Completed Journey with incomplete record"))

    act.like(renderViewWithPrePopSession(onPageLoad, userAnswersToCheck) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn = srn,
          page = page,
          mode = NormalMode,
          otherAssets = otherAssetsData,
          otherAssetsToCheck = otherAssetsDataToCheck,
          schemeName = schemeName,
          viewOnlyViewModel = None,
          showBackLink = true,
          isPrePop = true
        )
      )
    }.withName("PrePop Journey Not Checked"))

    act.like(renderViewWithPrePopSession(onPageLoad, userAnswersChecked) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn = srn,
          page = page,
          mode = NormalMode,
          otherAssets = otherAssetsData ++ otherAssetsDataChecked,
          otherAssetsToCheck = Nil,
          schemeName = schemeName,
          viewOnlyViewModel = None,
          showBackLink = true,
          isPrePop = true
        )
      )
    }.withName("PrePop Journey Checked"))

    act.like(renderViewWithPrePopSession(onPageLoad, userAnswersToCheckWithIncompleteRecord) {
      implicit app => implicit request =>
        injected[ListView].apply(
          form(new YesNoPageFormProvider()),
          viewModel(
            srn = srn,
            page = page,
            mode = NormalMode,
            otherAssets = otherAssetsData,
            otherAssetsToCheck = otherAssetsDataToCheck,
            schemeName = schemeName,
            viewOnlyViewModel = None,
            showBackLink = true,
            isPrePop = true
          )
        )
    }.withName("PrePop Journey with 2 added records, 1 record to check and 1 incomplete data"))

    act.like(
      renderPrePopView(onPageLoad, OtherAssetsListPage(srn), true, completedUserAnswers) {
        implicit app => implicit request =>
          injected[ListView]
            .apply(
              form(injected[YesNoPageFormProvider]).fill(true),
              viewModel(
                srn = srn,
                page = page,
                mode = NormalMode,
                otherAssets = otherAssetsData,
                otherAssetsToCheck = Nil,
                schemeName = schemeName,
                viewOnlyViewModel = None,
                showBackLink = true,
                isPrePop = false
              )
            )
      }
    )

    act.like(
      redirectToPage(
        onPageLoad,
        onPageLoadTaskListController,
        defaultUserAnswers
      ).withName("Redirect to Task List when 0 Other Assets completed and not in ViewOnly mode")
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

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, defaultUserAnswers))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "OtherAssetsListController in view only mode" - {

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn = srn,
              page = page,
              mode = ViewOnlyMode,
              otherAssets = otherAssetsData,
              otherAssetsToCheck = Nil,
              schemeName = schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel),
              showBackLink = true,
              isPrePop = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(defaultUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn = srn,
              page = page,
              mode = ViewOnlyMode,
              otherAssets = otherAssetsDataChanged,
              otherAssetsToCheck = Nil,
              schemeName = schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true,
              isPrePop = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = noOtherAssetsUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView].apply(
          form(new YesNoPageFormProvider()),
          viewModel(
            srn = srn,
            page = page,
            mode = ViewOnlyMode,
            otherAssets = List(),
            otherAssetsToCheck = Nil,
            schemeName = schemeName,
            viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
            showBackLink = true,
            isPrePop = false
          )
        )
      }.withName("OnPageLoadViewOnly renders ok with no loans")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        onPageLoadViewOnlyTaskListController
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to view only tasklist")
    )

    act.like(
      renderView(
        onPreviousViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[ListView].apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn = srn,
            page = page,
            mode = ViewOnlyMode,
            otherAssets = otherAssetsData,
            otherAssetsToCheck = Nil,
            schemeName = schemeName,
            viewOnlyViewModel = Some(
              viewOnlyViewModel.copy(
                currentVersion = (submissionNumberTwo - 1).max(0),
                previousVersion = (submissionNumberOne - 1).max(0)
              )
            ),
            showBackLink = false,
            isPrePop = false
          )
        )
      }.withName("OnPreviousViewOnly renders the correct view with decreased submission numbers")
    )
  }
}
