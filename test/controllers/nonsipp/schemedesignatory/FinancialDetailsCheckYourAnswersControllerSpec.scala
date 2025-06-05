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

package controllers.nonsipp.schemedesignatory

import play.api.test.FakeRequest
import services.{PsrSubmissionService, SchemeDateService}
import pages.nonsipp.schemedesignatory.{HowManyMembersPage, ValueOfAssetsPage}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage, WhichTaxYearPage}
import controllers.nonsipp.schemedesignatory.FinancialDetailsCheckYourAnswersController._
import org.mockito.stubbing.OngoingStubbing
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

class FinancialDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FinancialDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FinancialDetailsCheckYourAnswersController.onSubmit(srn, NormalMode)
  private lazy val onPageLoadViewOnly = routes.FinancialDetailsCheckYourAnswersController.onPageLoadViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onSubmitViewOnly = routes.FinancialDetailsCheckYourAnswersController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.FinancialDetailsCheckYourAnswersController.onPreviousViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val mockSchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    reset(mockPsrSubmissionService)
  }

  "FinancialDetailsCheckYourAnswersController" - {

    val userAnswersWithTaxYear = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

    act.like(renderView(onPageLoad, userAnswersWithTaxYear) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(
          srn,
          NormalMode,
          howMuchCashPage = None,
          valueOfAssetsPage = None,
          feesCommissionsWagesSalariesPage = None,
          dateRange,
          defaultSchemeDetails,
          viewOnlyUpdated = false
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

  }

  "FinancialDetailsCheckYourAnswersController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
      .unsafeSet(FbVersionPage(srn), "002")
    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    "must return OK and render the correct view without back link" in {

      mockTaxYear(dateRange)

      val application =
        applicationBuilder(userAnswers = Some(currentUserAnswers), previousUserAnswers = Some(previousUserAnswers))
          .build()

      running(application) {

        val request = FakeRequest(GET, onPreviousViewOnly.url)

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) must include("Financial details")
        contentAsString(result) must include("Submitted on")

        (contentAsString(result) must not).include("govuk-back-link")
      }
    }

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              ViewOnlyMode,
              howMuchCashPage = None,
              valueOfAssetsPage = None,
              feesCommissionsWagesSalariesPage = None,
              dateRange,
              defaultSchemeDetails,
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
      .unsafeSet(ValueOfAssetsPage(srn, NormalMode), MoneyInPeriod(money, money))

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              srn,
              ViewOnlyMode,
              howMuchCashPage = None,
              valueOfAssetsPage = Some(MoneyInPeriod(money, money)),
              feesCommissionsWagesSalariesPage = None,
              dateRange,
              defaultSchemeDetails,
              viewOnlyUpdated = true,
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
      ).withName("Submit redirects to view only tasklist")
    )

  }
  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[DateRange]] =
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(taxYear))
}
