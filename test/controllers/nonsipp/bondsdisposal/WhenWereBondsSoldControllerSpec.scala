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

package controllers.nonsipp.bondsdisposal

import services.SchemeDateService
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.DatePageView
import utils.IntUtils.given
import play.api.inject
import forms.DatePageFormProvider
import models.{DateRange, NormalMode}
import pages.nonsipp.bondsdisposal.WhenWereBondsSoldPage
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}

import java.time.LocalDate

class WhenWereBondsSoldControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val bondIndex = 1
  private val disposalIndex = 1

  private lazy val onPageLoad =
    routes.WhenWereBondsSoldController.onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.WhenWereBondsSoldController.onSubmit(srn, bondIndex, disposalIndex, NormalMode)

  val schemeDatePeriod: DateRange = DateRange(LocalDate.parse("2020-04-06"), LocalDate.parse("2021-04-05"))
  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    setSchemeDate(Some(schemeDatePeriod))
  }

  def setSchemeDate(date: Option[DateRange]): Unit =
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(date)

  "WhenWereBondsSoldController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[DatePageView].apply(
          WhenWereBondsSoldController.form(injected[DatePageFormProvider], dateRange),
          WhenWereBondsSoldController.viewModel(srn, bondIndex, disposalIndex, NormalMode)
        )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, WhenWereBondsSoldPage(srn, bondIndex, disposalIndex), dateRange.to) {
        implicit app => implicit request =>
          injected[DatePageView].apply(
            WhenWereBondsSoldController.form(injected[DatePageFormProvider], dateRange).fill(dateRange.to),
            WhenWereBondsSoldController.viewModel(srn, bondIndex, disposalIndex, NormalMode)
          )
      }
    )

    act.like(
      redirectNextPage(onSubmit, "value.day" -> "10", "value.month" -> "06", "value.year" -> "2020")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value.day" -> "10", "value.month" -> "06", "value.year" -> "2020"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
