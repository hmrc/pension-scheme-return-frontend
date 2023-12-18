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

package controllers.nonsipp.accountingperiod

import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.DateRangeFormProvider
import models.NormalMode
import pages.nonsipp.WhichTaxYearPage
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import play.api.libs.json.JsPath
import services.TaxYearService
import views.html.DateRangeView

class AccountingPeriodControllerSpec extends ControllerBaseSpec {

  private val mockTaxYearService = mock[TaxYearService]
  private val userAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)

  override def beforeEach(): Unit = reset(mockTaxYearService)

  "AccountingPeriodController" - {

    val form = AccountingPeriodController.form(new DateRangeFormProvider(), defaultTaxYear, List())
    lazy val viewModel = AccountingPeriodController.viewModel(srn, refineMV(1), NormalMode)

    val rangeGen = dateRangeWithinRangeGen(dateRange)
    val dateRangeData = rangeGen.sample.value
    val otherDateRangeData = rangeGen.sample.value

    val userAnswersWithIntersectedPeriods =
      emptyUserAnswers
        .unsafeSet(WhichTaxYearPage(srn), dateRange)
        .unsafeSet(AccountingPeriodPage(srn, refineMV(1), NormalMode), otherDateRangeData)
        .unsafeSet(AccountingPeriodPage(srn, refineMV(2), NormalMode), dateRangeData)

    lazy val onPageLoad = routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
    lazy val onSubmit = routes.AccountingPeriodController.onSubmit(srn, refineMV(1), NormalMode)

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[DateRangeView]
      view(form, viewModel)
    })

    act.like(
      renderPrePopView(onPageLoad, AccountingPeriodPage(srn, refineMV(1), NormalMode), dateRangeData, userAnswers) {
        implicit app => implicit request =>
          val view = injected[DateRangeView]
          view(form.fill(dateRangeData), viewModel)
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, userAnswers, formData(form, dateRangeData): _*).withName("save and continue"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      saveAndContinue(onSubmit, userAnswers, Some(JsPath \ "accountingPeriods"), formData(form, dateRangeData): _*)
        .withName("allow accounting period to be updated")
    )

    act.like(
      invalidForm(onSubmit, userAnswersWithIntersectedPeriods, formData(form, dateRangeData): _*)
        .withName("return a 400 if range intersects")
    )
  }
}
