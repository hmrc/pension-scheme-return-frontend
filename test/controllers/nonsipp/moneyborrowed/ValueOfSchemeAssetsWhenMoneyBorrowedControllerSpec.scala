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

package controllers.nonsipp.moneyborrowed

import controllers.nonsipp.moneyborrowed.ValueOfSchemeAssetsWhenMoneyBorrowedController._
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import views.html.MoneyView
import eu.timepit.refined.refineMV
import models.NormalMode
import pages.nonsipp.moneyborrowed.{ValueOfSchemeAssetsWhenMoneyBorrowedPage, WhenBorrowedPage}
import forms.mappings.errors.MoneyFormErrorProvider

import java.time.LocalDate
import java.util.Locale
import java.time.format.DateTimeFormatter

class ValueOfSchemeAssetsWhenMoneyBorrowedControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private lazy val onPageLoad = routes.ValueOfSchemeAssetsWhenMoneyBorrowedController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.ValueOfSchemeAssetsWhenMoneyBorrowedController.onSubmit(srn, index, NormalMode)

  "ValueOfSchemeAssetsWhenMoneyBorrowedController" - {

    val schemeName = defaultSchemeDetails.schemeName

    def formatDate(
      date: LocalDate
    ): String = {
      val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val outputFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
      val dateParsed = LocalDate.parse(date.toString, inputFormat)
      dateParsed.format(outputFormat)

    }

    val updatedUserAnswers = defaultUserAnswers.unsafeSet(WhenBorrowedPage(srn, index), localDate)

    act.like(renderView(onPageLoad, updatedUserAnswers) { implicit app => implicit request =>
      injected[MoneyView].apply(
        viewModel(srn, index, schemeName, formatDate(localDate), form(injected[MoneyFormErrorProvider]), NormalMode)
      )
    })

    act.like(
      renderPrePopView(onPageLoad, ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), money, updatedUserAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            viewModel(
              srn,
              index,
              schemeName,
              formatDate(localDate),
              form(injected[MoneyFormErrorProvider]).fill(money),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, updatedUserAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, updatedUserAnswers, "value" -> "1"))

    act.like(invalidForm(onSubmit, updatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
