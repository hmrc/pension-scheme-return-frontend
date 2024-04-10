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

package controllers.nonsipp.otherassetsdisposal

import services.SchemeDateService
import pages.nonsipp.otherassetsdisposal.WhenWasAssetSoldPage
import play.api.inject.guice.GuiceableModule
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.DatePageView
import eu.timepit.refined.refineMV
import play.api.inject
import forms.DatePageFormProvider
import models.NormalMode

class WhenWasAssetSoldControllerSpec extends ControllerBaseSpec {

  private val assetIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.WhenWasAssetSoldController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.WhenWasAssetSoldController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))
  }

  "WhenWasAssetSoldController" - {

    act.like(
      renderView(onPageLoad) { implicit app => implicit request =>
        injected[DatePageView].apply(
          WhenWasAssetSoldController.form(injected[DatePageFormProvider], dateRange),
          WhenWasAssetSoldController.viewModel(srn, assetIndex, disposalIndex, NormalMode)
        )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, WhenWasAssetSoldPage(srn, assetIndex, disposalIndex), dateRange.to) {
        implicit app => implicit request =>
          injected[DatePageView].apply(
            WhenWasAssetSoldController.form(injected[DatePageFormProvider], dateRange).fill(dateRange.to),
            WhenWasAssetSoldController.viewModel(srn, assetIndex, disposalIndex, NormalMode)
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
