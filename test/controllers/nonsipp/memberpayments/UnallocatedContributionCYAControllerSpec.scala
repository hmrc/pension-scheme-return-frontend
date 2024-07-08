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

package controllers.nonsipp.memberpayments

import services.{PsrSubmissionService, SchemeDateService}
import play.api.mvc.Call
import controllers.nonsipp.memberpayments.UnallocatedContributionCYAController._
import config.Refined.Max3
import controllers.ControllerBaseSpec
import play.api.inject.bind
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import org.mockito.stubbing.OngoingStubbing
import models._
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import cats.data.NonEmptyList
import views.html.CYAWithRemove

class UnallocatedContributionCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private def onPageLoad(mode: Mode): Call =
    controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController.onPageLoad(srn, mode)

  private def onSubmit(mode: Mode): Call =
    controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController.onSubmit(srn, mode)

  private lazy val onPageLoadViewOnly =
    controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController.onPageLoadViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onSubmitViewOnly =
    controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController.onPreviousViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val updatedValues = SchemeMemberNumbers(4, 5, 6)

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

  "UnallocatedContributionCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CYAWithRemove].apply(
            UnallocatedContributionCYAController.viewModel(
              srn,
              schemeName,
              money,
              mode,
              viewOnlyUpdated = false
            )
          )
        }.withName(s"render correct ${mode.toString} view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .withName(s"redirect to next page when in ${mode.toString} mode")
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
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

  "UnallocatedContributionCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
      .unsafeSet(FbVersionPage(srn), "002")

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
      .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CYAWithRemove].apply(
            viewModel(
              srn,
              schemeName,
              money,
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.before(mockTaxYear(dateRange))
        .withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CYAWithRemove].apply(
            viewModel(
              srn,
              schemeName,
              money,
              ViewOnlyMode,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.before(mockTaxYear(dateRange))
        .withName("OnPageLoadViewOnly renders ok with changed flag")
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
        controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
          .onPageLoadViewOnly(srn, yearString, submissionNumberOne, submissionNumberZero)
      ).withName(
        "Submit previous view only redirects to UnallocatedContributionCYAController for the previous submission"
      )
    )
  }

  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))

}
