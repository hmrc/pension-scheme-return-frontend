/*
 * Copyright 2023 HM Revenue & Customs
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

import services.{PsrSubmissionService, SchemeDateService}
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import play.api.inject.guice.GuiceableModule
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import pages.nonsipp.WhichTaxYearPage
import controllers.nonsipp.schemedesignatory.FinancialDetailsCheckYourAnswersController._
import models.{DateRange, NormalMode}
import org.mockito.ArgumentMatchers.any

class FinancialDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FinancialDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FinancialDetailsCheckYourAnswersController.onSubmit(srn, NormalMode)

  private val mockSchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

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
          Left(dateRange),
          defaultSchemeDetails
        )
      )
    }.before(mockTaxYear(dateRange)))

    act.like(
      redirectNextPage(onSubmit)
        .before(
          MockPSRSubmissionService.submitPsrDetails()
        )
        .after(
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
        )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

  private def mockTaxYear(taxYear: DateRange) =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))
}
