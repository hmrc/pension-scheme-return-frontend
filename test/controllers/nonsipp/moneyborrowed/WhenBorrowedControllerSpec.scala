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

package controllers.nonsipp.moneyborrowed

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.DatePageFormProvider
import models.{Money, NormalMode, Percentage, UserAnswers}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.DatePageView

import java.time.LocalDate
import pages.nonsipp.moneyborrowed.{BorrowedAmountAndRatePage, LenderNamePage, WhenBorrowedPage}
import controllers.nonsipp.moneyborrowed.WhenBorrowedController._

class WhenBorrowedControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit =
    reset(mockSchemeDateService)

  val populatedUserAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(LenderNamePage(srn, index), lenderName)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index), amountBorrowed) //.unsafeSet(LandOrPropertyAddressLookupPage(srn, refineMV(1)), address1)

  val populatedUserAnswers1: UserAnswers =
    defaultUserAnswers.set(BorrowedAmountAndRatePage(srn, index), amountBorrowed).get

  private lazy val onPageLoad = routes.WhenBorrowedController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.WhenBorrowedController.onSubmit(srn, index, NormalMode)

  private val dateTooEarlyForm = List(
    "value.day" -> "31",
    "value.month" -> "12",
    "value.year" -> "1899"
  )

  "WhenDidSchemeAcquireController" - {

    val taxYear = Some(Left(dateRange))
    val date = LocalDate.of(2020, 12, 10)

    act.like(
      journeyRecoveryPage(onPageLoad, Some(emptyUserAnswers))
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(None))
        .withName("onPageLoad redirect to journey recovery page when taxYearOrAccountingPeriods not found")
    )

    act.like(
      journeyRecoveryPage(onPageLoad, Some(emptyUserAnswers))
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
        .withName("onPageLoad redirect to journey recovery page when lender name data not found")
    )

    act.like(renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
      injected[DatePageView]
        .apply(
          WhenBorrowedController.form(injected[DatePageFormProvider])(date, createMessages(app)),
          viewModel(srn, index, NormalMode, schemeName, amountBorrowed._1.displayAs, lenderName)
        )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(
      renderPrePopView(onPageLoad, WhenBorrowedPage(srn, index), date, populatedUserAnswers) {
        implicit app => implicit request =>
          injected[DatePageView]
            .apply(
              WhenBorrowedController
                .form(injected[DatePageFormProvider])(date, createMessages(app))
                .fill(date),
              viewModel(srn, index, NormalMode, schemeName, amountBorrowed._1.displayAs, lenderName)
            )
      }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      journeyRecoveryPage(onSubmit, Some(emptyUserAnswers))
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(None))
        .withName("onSubmit redirect to journey recovery page when taxYearOrAccountingPeriods not found")
    )

    act.like(
      journeyRecoveryPage(onSubmit, Some(emptyUserAnswers))
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
        .withName("onSubmit redirect to journey recovery page when lender name data not found")
    )

    act.like(
      invalidForm(onSubmit, populatedUserAnswers).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      saveAndContinue(
        onSubmit,
        populatedUserAnswers,
        "value.day" -> "10",
        "value.month" -> "12",
        "value.year" -> "2020"
      ).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      invalidForm(onSubmit, populatedUserAnswers, dateTooEarlyForm: _*)
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )
  }
}
