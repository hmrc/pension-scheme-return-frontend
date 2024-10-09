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

package controllers.nonsipp.totalvaluequotedshares

import services.{PsrSubmissionService, SchemeDateService}
import play.api.inject.bind
import controllers.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesCYAController._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage, WhichTaxYearPage}
import org.mockito.stubbing.OngoingStubbing
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.totalvaluequotedshares.{QuotedSharesManagedFundsHeldPage, TotalValueQuotedSharesPage}
import org.mockito.Mockito._
import config.RefinedTypes.Max3
import controllers.ControllerBaseSpec
import cats.data.NonEmptyList
import views.html.CYAWithRemove

class TotalValueQuotedSharesCYAControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.TotalValueQuotedSharesCYAController.onPageLoad(srn)
  private lazy val onSubmit = routes.TotalValueQuotedSharesCYAController.onSubmit(srn)
  private lazy val onPageLoadViewOnly =
    routes.TotalValueQuotedSharesCYAController.onPageLoadViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onSubmitViewOnly =
    routes.TotalValueQuotedSharesCYAController.onSubmitViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
  private lazy val onPreviousViewOnly =
    routes.TotalValueQuotedSharesCYAController.onPreviousViewOnly(
      srn,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )

  private val mockSchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "TotalValueQuotedSharesCYAController" - {

    val userAnswersWithTaxYear = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(TotalValueQuotedSharesPage(srn), money)

    act.like(renderView(onPageLoad, userAnswersWithTaxYear) { implicit app => implicit request =>
      injected[CYAWithRemove].apply(
        viewModel(
          srn,
          totalCost = Some(money),
          Left(dateRange),
          defaultSchemeDetails,
          NormalMode,
          None,
          showBackLink = true
        )
      )
    }.before(mockTaxYear(dateRange)))

    act.like(
      redirectNextPage(onSubmit)
        .before(
          MockPsrSubmissionService.submitPsrDetails()
        )
        .after(
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "in view only mode" - {

      val currentUserAnswers = defaultUserAnswers
        .unsafeSet(TotalValueQuotedSharesPage(srn), money)
        .unsafeSet(QuotedSharesManagedFundsHeldPage(srn), true)
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
        .unsafeSet(FbVersionPage(srn), "002")

      val previousUserAnswers = currentUserAnswers
        .unsafeSet(TotalValueQuotedSharesPage(srn), money)
        .unsafeSet(QuotedSharesManagedFundsHeldPage(srn), true)
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
            injected[CYAWithRemove].apply(
              viewModel(
                srn,
                Some(money),
                Left(dateRange),
                defaultSchemeDetails,
                ViewOnlyMode,
                Some(viewOnlyViewModel),
                showBackLink = true
              )
            )
        }.before(mockTaxYear(dateRange))
          .withName("OnPageLoadViewOnly renders ok with no changed flag")
      )

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = currentUserAnswers.unsafeSet(TotalValueQuotedSharesPage(srn), Money.zero),
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[CYAWithRemove].apply(
            viewModel(
              srn,
              Some(Money.zero),
              Left(dateRange),
              defaultSchemeDetails,
              ViewOnlyMode,
              Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true
            )
          )
        }.before(mockTaxYear(dateRange))
          .withName("OnPageLoadViewOnly renders ok when money is zero")
      )

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = currentUserAnswers.remove(TotalValueQuotedSharesPage(srn)).get,
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[CYAWithRemove].apply(
            viewModel(
              srn,
              totalCost = None,
              Left(dateRange),
              defaultSchemeDetails,
              ViewOnlyMode,
              Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
              showBackLink = true
            )
          )
        }.before(mockTaxYear(dateRange))
          .withName("OnPageLoadViewOnly renders ok with no quoted shares")
      )

      val updatedUserAnswers = currentUserAnswers
        .unsafeSet(TotalValueQuotedSharesPage(srn), otherMoney)

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
            injected[CYAWithRemove].apply(
              viewModel(
                srn,
                Some(otherMoney),
                Left(dateRange),
                defaultSchemeDetails,
                ViewOnlyMode,
                Some(viewOnlyViewModel.copy(viewOnlyUpdated = true)),
                showBackLink = true
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
        renderView(
          onPreviousViewOnly,
          userAnswers = currentUserAnswers,
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[CYAWithRemove].apply(
            viewModel(
              srn,
              totalCost = Some(money),
              Left(dateRange),
              defaultSchemeDetails,
              ViewOnlyMode,
              Some(
                viewOnlyViewModel.copy(
                  viewOnlyUpdated = false,
                  currentVersion = (submissionNumberTwo - 1).max(0),
                  previousVersion = (submissionNumberOne - 1).max(0)
                )
              ),
              showBackLink = false
            )
          )
        }.before(mockTaxYear(dateRange))
          .withName("OnPreviousViewOnly renders the view correctly")
      )

    }

  }

  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))
}
