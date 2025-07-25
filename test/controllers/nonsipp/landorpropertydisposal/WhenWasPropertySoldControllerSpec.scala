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

package controllers.nonsipp.landorpropertydisposal

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.DatePageView
import utils.IntUtils.given
import pages.nonsipp.landorpropertydisposal.WhenWasPropertySoldPage
import forms.DatePageFormProvider
import models.NormalMode
import services.SchemeDateService
import controllers.nonsipp.landorpropertydisposal.WhenWasPropertySoldController._
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.reset

import java.time.LocalDate

class WhenWasPropertySoldControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val disposalIndex = 1

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit =
    reset(mockSchemeDateService)

  private lazy val onPageLoad = routes.WhenWasPropertySoldController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = routes.WhenWasPropertySoldController.onSubmit(srn, index, disposalIndex, NormalMode)

  private val userAnswers = userAnswersWithAddress(srn, index)

  "WhenWasPropertySoldController" - {

    val taxYear = Some(Left(dateRange))
    val date = LocalDate.of(2020, 12, 10)
    val beforeDate = LocalDate.of(2021, 9, 18)

    act.like(
      journeyRecoveryPage(onPageLoad, Some(emptyUserAnswers))
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(None))
        .withName("onPageLoad redirect to journey recovery page when taxYearOrAccountingPeriods is not found")
    )

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[DatePageView]
        .apply(
          form(injected[DatePageFormProvider])(date, beforeDate, createMessages(using app)),
          viewModel(srn, index, disposalIndex, address.addressLine1, NormalMode)
        )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(
      renderPrePopView(onPageLoad, WhenWasPropertySoldPage(srn, index, disposalIndex), date, userAnswers) {
        implicit app => implicit request =>
          injected[DatePageView]
            .apply(
              form(injected[DatePageFormProvider])(date, beforeDate, createMessages(using app))
                .fill(date),
              viewModel(srn, index, disposalIndex, address.addressLine1, NormalMode)
            )
      }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      journeyRecoveryPage(onSubmit, Some(emptyUserAnswers))
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(None))
        .withName("onSubmit redirect to journey recovery page when taxYearOrAccountingPeriods is not found")
    )

    act.like(
      invalidForm(onSubmit, userAnswers).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      saveAndContinue(
        onSubmit,
        userAnswers,
        "value.day" -> "10",
        "value.month" -> "12",
        "value.year" -> "2020"
      ).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )
  }
}
