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

package controllers

import eu.timepit.refined.refineMV
import forms.DateRangeFormProvider
import models.{DateRange, NormalMode}
import pages.AccountingPeriodPage
import views.html.DateRangeView

class AccountingPeriodControllerSpec extends ControllerBaseSpec {

  "AccountingPeriodController" should {

    val form = AccountingPeriodController.form(new DateRangeFormProvider(), defaultTaxYear, List())
    lazy val viewModel = AccountingPeriodController.viewModel(srn, refineMV(1), NormalMode)

    val rangeGen = dateRangeWithinRangeGen(DateRange(defaultTaxYear.starts, defaultTaxYear.finishes))
    val dateRangeData = rangeGen.sample.value
    val otherDateRangeData = rangeGen.sample.value

    lazy val onPageLoad = routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
    lazy val onSubmit = routes.AccountingPeriodController.onSubmit(srn, refineMV(1), NormalMode)

    behave like renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[DateRangeView]
      view(form, viewModel)
    }

    behave like renderPrePopView(onPageLoad, AccountingPeriodPage(srn, refineMV(1)), dateRangeData) { implicit app => implicit request =>
      val view = injected[DateRangeView]
      view(form.fill(dateRangeData), viewModel)
    }

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)

    behave like saveAndContinue(onSubmit, formData(form, dateRangeData): _*)
    
    behave like invalidForm(onSubmit)

    behave like journeyRecoveryPage("onSubmit", onSubmit)

    "allow accounting period to be updated" when {
      val userAnswers = emptyUserAnswers.set(AccountingPeriodPage(srn, refineMV(1)), dateRangeData).get
      behave like saveAndContinue(onSubmit, userAnswers, formData(form, dateRangeData): _*)
    }

    "return a 400 if range intersects" when {
      val userAnswers =
        emptyUserAnswers
          .set(AccountingPeriodPage(srn, refineMV(1)), otherDateRangeData).get
          .set(AccountingPeriodPage(srn, refineMV(2)), dateRangeData).get

      behave like invalidForm(onSubmit, userAnswers, formData(form, dateRangeData): _*)
    }
  }
}