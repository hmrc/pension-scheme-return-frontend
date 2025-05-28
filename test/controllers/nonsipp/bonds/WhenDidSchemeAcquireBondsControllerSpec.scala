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

package controllers.nonsipp.bonds

import services.SchemeDateService
import play.api.inject.bind
import views.html.DatePageView
import eu.timepit.refined.refineMV
import forms.DatePageFormProvider
import models.NormalMode
import controllers.nonsipp.bonds.WhenDidSchemeAcquireBondsController._
import org.mockito.Mockito.reset
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds.WhenDidSchemeAcquireBondsPage
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec

import java.time.LocalDate

class WhenDidSchemeAcquireBondsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit =
    reset(mockSchemeDateService)

  private lazy val onPageLoad =
    routes.WhenDidSchemeAcquireBondsController.onPageLoad(srn, index.value, NormalMode)
  private lazy val onSubmit =
    routes.WhenDidSchemeAcquireBondsController.onSubmit(srn, index.value, NormalMode)

  private val dateTooEarlyForm = List(
    "value.day" -> "31",
    "value.month" -> "12",
    "value.year" -> "1899"
  )

  "WhenDidSchemeAcquireController" - {

    val taxYear = Some(Left(dateRange))
    val date = LocalDate.of(2020, 12, 10)

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[DatePageView]
        .apply(
          form(injected[DatePageFormProvider])(date, createMessages(app)),
          viewModel(srn, index, NormalMode, schemeName)
        )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(
      renderPrePopView(onPageLoad, WhenDidSchemeAcquireBondsPage(srn, index), date, defaultUserAnswers) {
        implicit app => implicit request =>
          injected[DatePageView]
            .apply(
              form(injected[DatePageFormProvider])(date, createMessages(app))
                .fill(date),
              viewModel(srn, index, NormalMode, schemeName)
            )
      }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      invalidForm(onSubmit, defaultUserAnswers).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      saveAndContinue(
        onSubmit,
        defaultUserAnswers,
        "value.day" -> "10",
        "value.month" -> "12",
        "value.year" -> "2020"
      ).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      invalidForm(onSubmit, defaultUserAnswers, dateTooEarlyForm: _*)
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )
  }
}
