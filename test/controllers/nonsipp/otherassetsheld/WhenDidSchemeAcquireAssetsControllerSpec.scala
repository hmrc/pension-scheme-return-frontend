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

package controllers.nonsipp.otherassetsheld

import pages.nonsipp.otherassetsheld.WhenDidSchemeAcquireAssetsPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.DatePageView
import utils.IntUtils.given
import forms.DatePageFormProvider
import models.NormalMode
import services.SchemeDateService
import controllers.nonsipp.otherassetsheld.WhenDidSchemeAcquireAssetsController._
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.reset

import java.time.LocalDate

class WhenDidSchemeAcquireAssetsControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit =
    reset(mockSchemeDateService)

  private lazy val onPageLoad =
    routes.WhenDidSchemeAcquireAssetsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    routes.WhenDidSchemeAcquireAssetsController.onSubmit(srn, index, NormalMode)

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
          form(injected[DatePageFormProvider])(date, createMessages(using app)),
          viewModel(srn, index, NormalMode, schemeName)
        )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(
      renderPrePopView(onPageLoad, WhenDidSchemeAcquireAssetsPage(srn, index), date, defaultUserAnswers) {
        implicit app => implicit request =>
          injected[DatePageView]
            .apply(
              form(injected[DatePageFormProvider])(date, createMessages(using app))
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
      invalidForm(onSubmit, defaultUserAnswers, dateTooEarlyForm*)
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )
  }
}
