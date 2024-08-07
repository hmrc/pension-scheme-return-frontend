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
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.ListView
import eu.timepit.refined.refineMV
import models.{HowDisposed, NormalMode, ViewOnlyMode}
import pages.nonsipp.bondsdisposal.{BondsDisposalProgress, HowWereBondsDisposedOfPage}
import viewmodels.models.SectionCompleted
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds.{BondsCompleted, NameOfBondsPage}
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import play.api.inject
import controllers.nonsipp.bondsdisposal.ReportBondsDisposalListController._
import forms.YesNoPageFormProvider

import scala.concurrent.Future

class ReportBondsDisposalListControllerSpec extends ControllerBaseSpec {

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
  private val bondIndexOne = refineMV[Max5000.Refined](1)
  private val disposalIndexOne = refineMV[Max50.Refined](1)
  private val disposalIndexTwo = refineMV[Max50.Refined](2)

  private val numberOfDisposals = 2
  private val maxPossibleNumberOfDisposals = 100

  private val disposalIndexes = List(disposalIndexOne, disposalIndexTwo)
  private val bondsDisposalsWithIndexes =
    List(((bondIndexOne, disposalIndexes), SectionCompleted))

  private val completedUserAnswers = defaultUserAnswers
  // Bonds data 1
    .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")
    .unsafeSet(BondsCompleted(srn, bondIndexOne), SectionCompleted)
    //Bond 1 - disposal data 1
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), HowDisposed.Sold)
    .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne), SectionCompleted)
    //Bond 1 - disposal data 2
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexTwo), HowDisposed.Sold)
    .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexTwo), SectionCompleted)

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

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
          viewOnlyUpdated = false
        )
      )
    }.withName("Completed Journey"))

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "ReportBondsDisposalListController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
      // Bonds data 1
      .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")
      .unsafeSet(BondsCompleted(srn, bondIndexOne), SectionCompleted)
      //Bond 1 - disposal data 1
      .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), HowDisposed.Sold)
      .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne), SectionCompleted)
      //Bond 1 - disposal data 2
      .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexTwo), HowDisposed.Sold)
      .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexTwo), SectionCompleted)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
      .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
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
      .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")

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
        controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
      ).withName(
        "Submit previous view only redirects to the controller with parameters for the previous submission"
      )
    )
  }

}
