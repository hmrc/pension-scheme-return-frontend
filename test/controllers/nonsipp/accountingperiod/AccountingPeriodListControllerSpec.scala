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

package controllers.nonsipp.accountingperiod

import views.html.ListView
import config.Constants.maxAccountingPeriods
import eu.timepit.refined.refineV
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import org.scalacheck.Gen
import forms.YesNoPageFormProvider
import models.NormalMode
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import config.RefinedTypes.OneToThree
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class AccountingPeriodListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  "AccountingPeriodListController" - {

    val dateRanges = Gen.listOfN(3, dateRangeGen).sample.value

    val userAnswers =
      dateRanges.zipWithIndex
        .foldLeft(defaultUserAnswers) { case (userAnswers, (range, index)) =>
          val refinedIndex = refineV[OneToThree](index + 1).toOption.value
          userAnswers.set(AccountingPeriodPage(srn, refinedIndex, NormalMode), range).get
        }

    val form = AccountingPeriodListController.form(new YesNoPageFormProvider())
    lazy val viewModel = AccountingPeriodListController.viewModel(srn, NormalMode, dateRanges)

    lazy val onPageLoad = routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.AccountingPeriodListController.onSubmit(srn, NormalMode)

    lazy val accountingPeriodPage = controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[ListView]
      view(form, viewModel)
    })

    act.like(redirectToPage(onPageLoad, accountingPeriodPage))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "AccountingPeriodListController.viewModel" - {

    "contain the correct number of rows" in {

      val rowsGen = Gen.choose(0, maxAccountingPeriods).flatMap(Gen.listOfN(_, dateRangeGen))

      forAll(srnGen, rowsGen, modeGen) { (srn, rows, mode) =>
        val viewModel = AccountingPeriodListController.viewModel(srn, mode, rows)
        viewModel.page.sections.map(_.rows.size).sum mustBe rows.length
      }
    }

    "drop any rows over 3" in {
      val rowsGen = Gen.listOf(dateRangeGen)

      forAll(srnGen, rowsGen, modeGen) { (srn, rows, mode) =>
        val viewModel = AccountingPeriodListController.viewModel(srn, mode, rows)
        viewModel.page.sections.length must be <= 3
      }
    }
  }
}
