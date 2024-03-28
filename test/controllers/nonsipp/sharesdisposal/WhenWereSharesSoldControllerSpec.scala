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

package controllers.nonsipp.sharesdisposal

import services.SchemeDateService
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.DatePageView
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal.WhenWereSharesSoldPage
import play.api.inject
import forms.DatePageFormProvider
import models.NormalMode

class WhenWereSharesSoldControllerSpec extends ControllerBaseSpec {

  private val shareIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.WhenWereSharesSoldController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.WhenWereSharesSoldController.onSubmit(srn, shareIndex, disposalIndex, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), companyName)

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))
  }

  "WhenWereSharesSoldController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[DatePageView].apply(
          WhenWereSharesSoldController.form(injected[DatePageFormProvider], dateRange),
          WhenWereSharesSoldController.viewModel(srn, shareIndex, disposalIndex, companyName, NormalMode)
        )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, WhenWereSharesSoldPage(srn, shareIndex, disposalIndex), dateRange.to, userAnswers) {
        implicit app => implicit request =>
          injected[DatePageView].apply(
            WhenWereSharesSoldController.form(injected[DatePageFormProvider], dateRange).fill(dateRange.to),
            WhenWereSharesSoldController.viewModel(srn, shareIndex, disposalIndex, companyName, NormalMode)
          )
      }
    )

    act.like(
      redirectNextPage(onSubmit, userAnswers, "value.day" -> "10", "value.month" -> "06", "value.year" -> "2020")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value.day" -> "10", "value.month" -> "06", "value.year" -> "2020"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
