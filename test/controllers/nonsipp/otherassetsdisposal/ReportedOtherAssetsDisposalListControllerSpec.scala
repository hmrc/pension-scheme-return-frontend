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
import pages.nonsipp.otherassetsdisposal.{HowWasAssetDisposedOfPage, OtherAssetsDisposalProgress}
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.ListView
import eu.timepit.refined.refineMV
import controllers.nonsipp.otherassetsdisposal.ReportedOtherAssetsDisposalListController._
import forms.YesNoPageFormProvider
import models.{NormalMode, ViewOnlyMode}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import pages.nonsipp.otherassetsheld.{OtherAssetsCompleted, WhatIsOtherAssetPage}
import models.HowDisposed.{Other, Sold, Transferred}
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject

import scala.concurrent.Future

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
  private val otherAssetsDisposalsWithIndexes =
    Map((otherAssetIndexOne, disposalIndexes), (otherAssetIndexTwo, disposalIndexes))

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

  private val mockPsrSubmissionService = mock[PsrSubmissionService]
  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "ReportedOtherAssetsDisposalListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          NormalMode,
          page,
          otherAssetsDisposalsWithIndexes,
          completedUserAnswers,
          viewOnlyUpdated = false
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

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

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

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(new YesNoPageFormProvider()),
            viewModel(
              srn,
              mode = ViewOnlyMode,
              page,
              otherAssetsDisposalsWithIndexes,
              completedUserAnswers,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(WhatIsOtherAssetPage(srn, otherAssetIndexOne), nameOfAsset)

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
              completedUserAnswers,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
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
      redirectToPage(
        onPreviousViewOnly,
        controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
      ).withName(
        "Submit previous view only redirects to the controller with parameters for the previous submission"
      )
    )
  }

}
