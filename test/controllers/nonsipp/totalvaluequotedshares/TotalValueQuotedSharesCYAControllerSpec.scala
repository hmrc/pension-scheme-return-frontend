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
import config.Refined.Max3
import controllers.ControllerBaseSpec
import play.api.inject.bind
import controllers.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesCYAController._
import pages.nonsipp.WhichTaxYearPage
import org.mockito.stubbing.OngoingStubbing
import models.DateRange
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import org.mockito.Mockito.{times, verify, when}
import cats.data.NonEmptyList
import views.html.CYAWithRemove

class TotalValueQuotedSharesCYAControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.TotalValueQuotedSharesCYAController.onPageLoad(srn)
  private lazy val onSubmit = routes.TotalValueQuotedSharesCYAController.onSubmit(srn)

  private val mockSchemeDateService = mock[SchemeDateService]
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

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
          totalCost = money,
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
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

  private def mockTaxYear(
    taxYear: DateRange
  ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(any())).thenReturn(Some(Left(taxYear)))
}
