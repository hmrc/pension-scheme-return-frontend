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
import models._
import pages.nonsipp.bondsdisposal._
import viewmodels.models.SectionCompleted
import eu.timepit.refined.api.Refined
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

  val bondIndex: Refined[Int, Max5000.Refined] = refineMV[Max5000.Refined](1)
  val disposalIndex: Refined[Int, Max50.Refined] = refineMV[Max50.Refined](1)

  private val completedUserAnswers = defaultUserAnswers
  // Bonds data 1
    .unsafeSet(NameOfBondsPage(srn, bondIndexOne), "name")
    .unsafeSet(BondsCompleted(srn, bondIndexOne), SectionCompleted)
    .unsafeSet(BondsDisposalPage(srn), true)
    .unsafeSet(BondsDisposalCompleted(srn), SectionCompleted)
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

    //    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
    //      injected[ListView].apply(
    //        form(injected[YesNoPageFormProvider]),
    //        viewModel(
    //          srn,
    //          NormalMode,
    //          page,
    //          bondsDisposalsWithIndexes,
    //          numberOfDisposals,
    //          maxPossibleNumberOfDisposals,
    //          completedUserAnswers,
    //          schemeName,
    //          viewOnlyViewModel = None
    //        )
    //      )
    //    }.withName("Completed Journey"))
    //
    //    act.like(redirectNextPage(onSubmit, "value" -> "1"))
    //
    //    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
    //
    //    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
    //  }

    act.like(renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
      injected[ListView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            NormalMode,
            page = 1,
            bondsDisposalsWithIndexes,
            numberOfDisposals,
            maxPossibleNumberOfDisposals = 50,
            completedUserAnswers,
            schemeName,
            viewOnlyViewModel = None
          )
        )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, completedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "ReportBondsDisposalListController in view only mode" - {

      val currentUserAnswers = completedUserAnswers
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

      val previousUserAnswers = completedUserAnswers
        .unsafeSet(FbVersionPage(srn), "001")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = false,
        year = yearString,
        currentVersion = submissionNumberOne,
        previousVersion = submissionNumberZero,
        compilationOrSubmissionDate = Some(submissionDateTwo)
      )

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
            injected[ListView].apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
                srn,
                ViewOnlyMode,
                page = 1,
                bondsDisposalsWithIndexes,
                numberOfDisposals = 1,
                maxPossibleNumberOfDisposals,
                completedUserAnswers,
                schemeName,
                viewOnlyViewModel = Some(viewOnlyViewModel)
              )
            )
        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated false")
      )

      val updatedUserAnswers = currentUserAnswers

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = updatedUserAnswers,
          optPreviousAnswers = Some(
            previousUserAnswers.unsafeSet(HowWereBondsDisposedOfPage(srn, refineMV(1), refineMV(1)), HowDisposed.Sold)
          )
        ) { implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(
              srn,
              ViewOnlyMode,
              page = 2,
              bondsDisposalsWithIndexes,
              numberOfDisposals = 1,
              maxPossibleNumberOfDisposals,
              completedUserAnswers,
              schemeName,
              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true))
            )
          )
        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated true")
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

    //      act.like(
    //        renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
    //          implicit app => implicit request =>
    //            injected[ListView].apply(
    //              form(injected[YesNoPageFormProvider]),
    //              viewModel(
    //                srn,
    //                mode = ViewOnlyMode,
    //                page = 1,
    //                bondsDisposalsWithIndexes,
    //                numberOfDisposals = 1,
    //                maxPossibleNumberOfDisposals,
    //                completedUserAnswers,
    //                schemeName,
    //                viewOnlyViewModel = Some(viewOnlyViewModel)
    //              )
    //            )
    //        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated false")
    //      )
    //
    //      val updatedUserAnswers = currentUserAnswers
    //
    //      act.like(
    //        renderView(
    //          onPageLoadViewOnly,
    //          userAnswers = updatedUserAnswers,
    //          optPreviousAnswers = Some(
    //            previousUserAnswers
    //              .unsafeSet(HowWereBondsDisposedOfPage(srn, refineMV(1), refineMV(1)), HowDisposed.Transferred)
    //          )
    //        ) { implicit app => implicit request =>
    //          injected[ListView].apply(
    //            form(injected[YesNoPageFormProvider]),
    //            viewModel(
    //              srn,
    //              mode = ViewOnlyMode,
    //              page = 1,
    //              bondsDisposalsWithIndexes,
    //              numberOfDisposals = 1,
    //              maxPossibleNumberOfDisposals,
    //              completedUserAnswers,
    //              schemeName,
    //              viewOnlyViewModel = Some(viewOnlyViewModel.copy(viewOnlyUpdated = true))
    //            )
    //          )
    //        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated true")
    //      )
    //
    //      act.like(
    //        redirectToPage(
    //          onSubmitViewOnly,
    //          controllers.nonsipp.routes.ViewOnlyTaskListController
    //            .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
    //        ).withName("Submit redirects to view only tasklist")
    //      )
    //
    //      act.like(
    //        redirectToPage(
    //          onPreviousViewOnly,
    //          controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
    //            .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
    //        ).withName(
    //          "Submit previous view only redirects to the controller with parameters for the previous submission"
    //        )
    //      )
    //    }
    //  }

  }
}
