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

package controllers.nonsipp.memberpayments

import controllers.ControllerBaseSpec
import forms.mappings.errors.MoneyFormErrorProvider
import models.NormalMode
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage
import views.html.MoneyView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class UnallocatedEmployerAmountControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.UnallocatedEmployerAmountController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.UnallocatedEmployerAmountController.onSubmit(srn, NormalMode)

  "ValueOfSchemeAssetsWhenMoneyBorrowedController" - {

    val myform = UnallocatedEmployerAmountController
    val schemeName = defaultSchemeDetails.schemeName

    def formatDate(
      date: LocalDate
    ): String = {
      val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val outputFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
      val dateParsed = LocalDate.parse(date.toString, inputFormat)
      dateParsed.format(outputFormat)

    }

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MoneyView].apply(
        UnallocatedEmployerAmountController.viewModel(
          srn,
          schemeName,
          UnallocatedEmployerAmountController.form(injected[MoneyFormErrorProvider]),
          NormalMode
        )
      )
    })

    act.like(
      renderPrePopView(onPageLoad, UnallocatedEmployerAmountPage(srn), money) { implicit app => implicit request =>
        injected[MoneyView].apply(
          UnallocatedEmployerAmountController.viewModel(
            srn,
            schemeName,
            UnallocatedEmployerAmountController.form(injected[MoneyFormErrorProvider]).fill(money),
            NormalMode
          )
        )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
