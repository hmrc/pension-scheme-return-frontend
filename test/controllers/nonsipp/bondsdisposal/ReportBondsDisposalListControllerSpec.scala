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

package controllers.nonsipp.bondsdisposal

import services.PsrSubmissionService
import views.html.ListView
import utils.IntUtils.given
import models._
import pages.nonsipp.bondsdisposal._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds.{BondsCompleted, NameOfBondsPage}
import config.RefinedTypes.{Max50, Max5000}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject
import controllers.nonsipp.bondsdisposal.ReportBondsDisposalListController._
import forms.YesNoPageFormProvider

class ReportBondsDisposalListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.ReportBondsDisposalListController.onPageLoad(srn, page)
  private lazy val onSubmit = routes.ReportBondsDisposalListController.onSubmit(srn, page, NormalMode)
  private lazy val bondsDisposalPage = routes.BondsDisposalController.onPageLoad(srn, NormalMode)

  private lazy val onSubmitViewOnly =
    routes.ReportBondsDisposalListController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPageLoadViewOnly =
    routes.ReportBondsDisposalListController.onPageLoadViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    routes.ReportBondsDisposalListController.onPreviousViewOnly(
      srn,
      1,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val page = 1
  private val bondIndexOne: Max5000 = 1
  private val disposalIndexOne: Max50 = 1
  private val disposalIndexTwo: Max50 = 2

  private val numberOfDisposals = 2
  private val maxPossibleNumberOfDisposals = 100

  private val disposalIndexes = List(disposalIndexTwo, disposalIndexOne)
  private val bondsDisposalsWithIndexes =
    List(((bondIndexOne, disposalIndexes), SectionCompleted))

  private val completedUserAnswers = defaultUserAnswers
    // Bonds data 1
    .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")
    .unsafeSet(BondsCompleted(srn, bondIndexOne), SectionCompleted)
    .unsafeSet(BondsDisposalPage(srn), true)
    // Bond 1 - disposal data 1
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), HowDisposed.Sold)
    .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne), SectionJourneyStatus.Completed)
    // Bond 1 - disposal data 2
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexTwo), HowDisposed.Sold)
    .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexTwo), SectionJourneyStatus.Completed)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "ReportBondsDisposalListController" - {

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          NormalMode,
          page,
          bondsDisposalsWithIndexes,
          numberOfDisposals,
          maxPossibleNumberOfDisposals,
          completedUserAnswers,
          schemeName,
          viewOnlyViewModel = None,
          showBackLink = true,
          maximumDisposalsReached = false,
          allBondsFullyDisposed = false
        )
      )
    }.withName("Completed Journey"))

    act.like(
      redirectToPage(
        onPageLoad,
        bondsDisposalPage,
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

  "ReportBondsDisposalListController in view only mode" - {

    val currentUserAnswers = completedUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = completedUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
      .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")

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
              bondsDisposalsWithIndexes,
              numberOfDisposals,
              maxPossibleNumberOfDisposals,
              completedUserAnswers,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel),
              showBackLink = true,
              maximumDisposalsReached = true,
              allBondsFullyDisposed = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated false")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(IsBuyerConnectedPartyPage(srn, bondIndexOne, disposalIndexOne), true)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(new YesNoPageFormProvider()),
            viewModel(
              srn,
              mode = ViewOnlyMode,
              page,
              bondsDisposalsWithIndexes,
              numberOfDisposals,
              maxPossibleNumberOfDisposals,
              updatedUserAnswers,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true,
              maximumDisposalsReached = true,
              allBondsFullyDisposed = false
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
        verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to view only taskList")
    )

    act.like(
      renderView(onPreviousViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[ListView].apply(
            form(new YesNoPageFormProvider()),
            viewModel(
              srn,
              mode = ViewOnlyMode,
              page,
              bondsDisposalsWithIndexes,
              numberOfDisposals,
              maxPossibleNumberOfDisposals,
              updatedUserAnswers,
              schemeName,
              viewOnlyViewModel = Some(
                viewOnlyViewModel.copy(
                  currentVersion = submissionNumberOne,
                  previousVersion = submissionNumberZero
                )
              ),
              showBackLink = false,
              maximumDisposalsReached = true,
              allBondsFullyDisposed = false
            )
          )
      }.withName("OnPreviousViewOnly renders view with parameters for the previous submission")
    )

  }
}
