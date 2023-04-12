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

package controllers.nonsipp

import controllers.ControllerBaseSpec
import forms.RadioListFormProvider
import models.{DateRange, Enumerable, NormalMode}
import org.scalacheck.Gen
import pages.nonsipp.WhichTaxYearPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.TaxYearService
import views.html.RadioListView

class WhichTaxYearControllerSpec extends ControllerBaseSpec {

  private val mockTaxYearService = mock[TaxYearService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[TaxYearService].toInstance(mockTaxYearService)
  )

  override def beforeEach(): Unit = {
    reset(mockTaxYearService)
    when(mockTaxYearService.current).thenReturn(defaultTaxYear)
  }

  "WhichTaxYearController" - {

    lazy val onPageLoad = routes.WhichTaxYearController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.WhichTaxYearController.onSubmit(srn, NormalMode)

    implicit val allDates: Enumerable[DateRange] = WhichTaxYearController.options(defaultTaxYear)
    val radioListForm = new RadioListFormProvider().apply[DateRange]("whichTaxYear.error.required")
    lazy val viewModel = WhichTaxYearController.viewModel(srn, NormalMode, defaultTaxYear)

    val testDate = Gen.oneOf(allDates.toList).sample.value._2

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[RadioListView]
      view(radioListForm, viewModel)
    })

    act.like(renderPrePopView(onPageLoad, WhichTaxYearPage(srn), testDate) { implicit app => implicit request =>
      val view = injected[RadioListView]
      view(radioListForm.fill(testDate), viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(invalidForm(onSubmit, "value" -> "invalid data"))

    act.like(saveAndContinue(onSubmit, "value" -> testDate.toString))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "WhichTaxYearController.options" - {

    "return the current tax year" in {

      val expectedDateRange = DateRange(defaultTaxYear.starts, defaultTaxYear.finishes)
      WhichTaxYearController.options(defaultTaxYear).get(expectedDateRange.toString) mustBe Some(expectedDateRange)
    }

    "return 7 options" in {

      WhichTaxYearController.options(defaultTaxYear).toList.length mustBe 7
    }

    "start date should always be a day after previous end date" in {

      val ranges = WhichTaxYearController.options(defaultTaxYear).toList.map(_._2)

      ranges.foldLeft(DateRange.from(defaultTaxYear.next)) { (previous, current) =>
        previous.from mustBe current.to.plusDays(1)
        current
      }
    }
  }
}
