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

package controllers.nonsipp.shares

import services.SchemeDateService
import pages.nonsipp.shares.{TypeOfSharesHeldPage, WhenDidSchemeAcquireSharesPage}
import config.Refined.Max5000
import controllers.ControllerBaseSpec
import play.api.inject.bind
import eu.timepit.refined.refineMV
import forms.DatePageFormProvider
import models.{NormalMode, TypeOfShares}
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.reset
import views.html.DatePageView
import models.TypeOfShares.ConnectedParty
import controllers.nonsipp.shares.WhenDidSchemeAcquireSharesController._

import java.time.LocalDate

class WhenDidSchemeAcquireSharesControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit =
    reset(mockSchemeDateService)

  private lazy val onPageLoad =
    routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    routes.WhenDidSchemeAcquireSharesController.onSubmit(srn, index, NormalMode)

  private val populatedUserAnswers =
    defaultUserAnswers.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)

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
        .withName("onPageLoad redirect to journey recovery page when address data not found")
    )

    act.like(renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
      injected[DatePageView]
        .apply(
          form(injected[DatePageFormProvider])(date, createMessages(app)),
          viewModel(srn, index, NormalMode, schemeName, ConnectedParty.name)
        )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(
      renderPrePopView(onPageLoad, WhenDidSchemeAcquireSharesPage(srn, index), date, populatedUserAnswers) {
        implicit app => implicit request =>
          injected[DatePageView]
            .apply(
              form(injected[DatePageFormProvider])(date, createMessages(app))
                .fill(date),
              viewModel(srn, index, NormalMode, schemeName, ConnectedParty.name)
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
        .withName("onSubmit redirect to journey recovery page when type of shares not found")
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
