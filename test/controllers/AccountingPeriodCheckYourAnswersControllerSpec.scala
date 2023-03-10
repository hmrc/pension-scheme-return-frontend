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

import cats.implicits.toShow
import config.Refined.OneToThree
import eu.timepit.refined._
import models.{DateRange, NormalMode}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import pages.AccountingPeriodPage
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models.SummaryAction
import views.html.CheckYourAnswersView

import java.time.LocalDate

class AccountingPeriodCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  private val from = LocalDate.of(2020, 1, 1)
  private val to = LocalDate.of(2020, 4, 1)

  private val dateRange = DateRange(from, to)

  lazy val onPageLoad = controllers.routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, refineMV[OneToThree](1))
  lazy val onSubmit = controllers.routes.AccountingPeriodCheckYourAnswersController.onSubmit(srn)
  lazy val viewModel = AccountingPeriodCheckYourAnswersController.viewModel(srn, refineMV(1), DateRange(from, to))

  val userAnswers = defaultUserAnswers.set(AccountingPeriodPage(srn, refineMV[OneToThree](1)), dateRange).success.value

  "AccountingPeriodCheckYourAnswersController" should {
    behave like renderView(onPageLoad, userAnswers)(implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(viewModel)
    )

    behave like redirectWhenCacheEmpty(onPageLoad, controllers.routes.AccountingPeriodController.onPageLoad(srn, refineMV[OneToThree](1), NormalMode))

    behave like journeyRecoveryPage("onPageLoad", onPageLoad)
    behave like journeyRecoveryPage("onSubmit", onSubmit)

    "viewModel" should {

      val viewModel = AccountingPeriodCheckYourAnswersController.viewModel _

      "have the correct message key for title" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          viewModel(srn, refineMV[OneToThree](1), dateRange).title.key mustBe "checkYourAnswers.title"
        }
      }

      "have the correct message key for heading" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          viewModel(srn, refineMV[OneToThree](1), dateRange).heading.key mustBe "checkYourAnswers.heading"
        }
      }

      "have the correct message key for start date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          viewModel(srn, refineMV[OneToThree](1), dateRange).rows.map(_.key.key) must contain("site.startDate")
        }
      }

      "have the correct message key for end date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          viewModel(srn, refineMV[OneToThree](1), dateRange).rows.map(_.key.key) must contain("site.endDate")
        }
      }

      "have the correct start date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          viewModel(srn, refineMV[OneToThree](1), dateRange).rows.map(_.value.key) must contain(dateRange.from.show)
        }
      }

      "have the correct end date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          viewModel(srn, refineMV[OneToThree](1), dateRange).rows.map(_.value.key) must contain(dateRange.to.show)
        }
      }

      "have the correct actions" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          val content = SimpleMessage("site.change")
          val href = routes.AccountingPeriodController.onPageLoad(srn, refineMV[OneToThree](1), NormalMode).url

          val actions = List(
            SummaryAction(content, href, SimpleMessage("site.startDate")),
            SummaryAction(content, href, SimpleMessage("site.endDate"))
          )

          viewModel(srn, refineMV[OneToThree](1), dateRange).rows.flatMap(_.actions) must contain allElementsOf actions
        }
      }

      "have the correct on submit value" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>

          viewModel(srn, refineMV[OneToThree](1), dateRange).onSubmit mustBe
            routes.AccountingPeriodCheckYourAnswersController.onSubmit(srn)
        }
      }
    }
  }
}
