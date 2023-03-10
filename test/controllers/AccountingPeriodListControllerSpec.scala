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

import config.Constants.maxAccountingPeriods
import config.Refined.OneToThree
import eu.timepit.refined.{refineMV, refineV}
import forms.YesNoPageFormProvider
import models.NormalMode
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import pages.AccountingPeriodPage
import views.html.ListView

class AccountingPeriodListControllerSpec extends ControllerBaseSpec {

  "AccountingPeriodListController" should {

    val dateRanges = Gen.listOfN(3, dateRangeGen).sample.value

    val userAnswers =
      dateRanges
        .zipWithIndex
        .foldLeft(defaultUserAnswers) {
          case (userAnswers, (range, index)) =>
            val refinedIndex = refineV[OneToThree](index + 1).toOption.value
            userAnswers.set(AccountingPeriodPage(srn, refinedIndex), range).get
        }

    val form = AccountingPeriodListController.form(new YesNoPageFormProvider())
    lazy val viewModel = AccountingPeriodListController.viewModel(srn, NormalMode, dateRanges)

    lazy val onPageLoad = routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.AccountingPeriodListController.onSubmit(srn, NormalMode)

    lazy val accountingPeriodPage = routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)

    behave like renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[ListView]
      view(form, viewModel)
    }

    behave like redirectToPage(onPageLoad, accountingPeriodPage)

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)

    behave like redirectNextPage(onSubmit, "value" -> "true")
    
    behave like invalidForm(onSubmit)

    behave like journeyRecoveryPage("onSubmit", onSubmit)
  }

  "AccountingPeriodListController.viewModel" should {

    "contain the correct number of rows" in {

      val rowsGen = Gen.choose(0, maxAccountingPeriods).flatMap(Gen.listOfN(_, dateRangeGen))

      forAll(srnGen, rowsGen, modeGen) { (srn, rows, mode) =>

        val viewModel = AccountingPeriodListController.viewModel(srn, mode, rows)
        viewModel.rows.length mustBe rows.length
      }
    }

    "drop any rows over 3" in {
      val rowsGen = Gen.listOf(dateRangeGen)

      forAll(srnGen, rowsGen, modeGen) { (srn, rows, mode) =>

        val viewModel = AccountingPeriodListController.viewModel(srn, mode, rows)
        viewModel.rows.length must be <= 3
      }
    }
  }
}