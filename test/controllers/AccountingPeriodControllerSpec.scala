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

import forms.DateRangeFormProvider
import models.{DateRange, NormalMode}
import pages.AccountingPeriodPage
import uk.gov.hmrc.time.TaxYear
import views.html.DateRangeView

class AccountingPeriodControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  "AccountingPeriodController" should {

    val form = AccountingPeriodController.form(new DateRangeFormProvider(), defaultTaxYear)
    lazy val viewModel = AccountingPeriodController.viewModel(srn, NormalMode)

    val data = dateRangeWithinRangeGen(DateRange(defaultTaxYear.starts, defaultTaxYear.finishes)).sample.value

    lazy val onPageLoad = routes.AccountingPeriodController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.AccountingPeriodController.onSubmit(srn, NormalMode)

    behave like renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[DateRangeView]
      view(form, viewModel)
    }

    behave like renderPrePopView(onPageLoad, AccountingPeriodPage(srn), data) { implicit app => implicit request =>
      val view = injected[DateRangeView]
      view(form.fill(data), viewModel)
    }

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)

    behave like redirectNextPage(onSubmit, form.fill(data).data.toList: _*)
    
    behave like invalidForm(onSubmit)

    behave like journeyRecoveryPage("onSubmit", onSubmit)
  }
}