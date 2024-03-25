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

package controllers.nonsipp.bondsdisposal

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.DatePageFormProvider
import models.NormalMode
import pages.nonsipp.bondsdisposal.WhenWereBondsSoldPage
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.DatePageView

class WhenWereBondsSoldControllerSpec extends ControllerBaseSpec {

  private val bondIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.WhenWereBondsSoldController.onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.WhenWereBondsSoldController.onSubmit(srn, bondIndex, disposalIndex, NormalMode)

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))
  }

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