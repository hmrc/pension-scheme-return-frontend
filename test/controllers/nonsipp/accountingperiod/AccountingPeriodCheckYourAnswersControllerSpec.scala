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

import play.api.mvc.Call
import views.html.CheckYourAnswersView
import cats.implicits.toShow
import eu.timepit.refined._
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import config.RefinedTypes.OneToThree
import controllers.ControllerBaseSpec
import utils.DateTimeUtils.localDateShow
import models.{NormalMode, UserAnswers}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{CheckYourAnswersViewModel, FormPageViewModel, SummaryAction}

class AccountingPeriodCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoad: Call =
    routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, 1, NormalMode)
  lazy val onSubmit: Call = routes.AccountingPeriodCheckYourAnswersController.onSubmit(srn, NormalMode)
  lazy val viewModel: FormPageViewModel[CheckYourAnswersViewModel] =
    AccountingPeriodCheckYourAnswersController.viewModel(srn, refineMV(1), dateRange, NormalMode)

  val userAnswers: UserAnswers =
    defaultUserAnswers.set(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange).success.value

  "AccountingPeriodCheckYourAnswersController" - {
    act.like(
      renderView(onPageLoad, userAnswers)(
        implicit app => implicit request => injected[CheckYourAnswersView].apply(viewModel)
      )
    )

    act.like(
      redirectWhenCacheEmpty(
        onPageLoad,
        routes.AccountingPeriodController.onPageLoad(srn, 1, NormalMode)
      )
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "viewModel" - {

      val viewModel = AccountingPeriodCheckYourAnswersController.viewModel _

      "have the correct message key for title" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).title.key mustBe "checkYourAnswers.title"
        }
      }

      "have the correct message key for heading" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          messageKey(viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).heading) mustBe "checkYourAnswers.heading"
        }
      }

      "have the correct message key for start date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).page.sections
            .flatMap(_.rows.map(_.key.key)) must contain(
            "site.startDate"
          )
        }
      }

      "have the correct message key for end date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).page.sections
            .flatMap(_.rows.map(_.key.key)) must contain(
            "site.endDate"
          )
        }
      }

      "have the correct start date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).page.sections
            .flatMap(_.rows.collect(_.value match {
              case m: Message => m.key
            })) must contain(
            dateRange.from.show
          )
        }
      }

      "have the correct end date" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).page.sections
            .flatMap(_.rows.collect(_.value match {
              case m: Message => m.key
            })) must contain(dateRange.to.show)
        }
      }

      "have the correct actions" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          val content = Message("site.change")
          val href = routes.AccountingPeriodController.onPageLoad(srn, 1, NormalMode).url

          val actions = List(
            SummaryAction(content, href + "#startDate", Message("site.startDate")),
            SummaryAction(content, href + "#endDate", Message("site.endDate"))
          )

          viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).page.sections
            .flatMap(_.rows.flatMap(_.actions)) must contain allElementsOf actions
        }
      }

      "have the correct on submit value" in {

        forAll(srnGen, dateRangeWithinRangeGen(dateRange)) { (srn, dateRange) =>
          viewModel(srn, refineMV[OneToThree](1), dateRange, NormalMode).onSubmit mustBe
            routes.AccountingPeriodCheckYourAnswersController.onSubmit(srn, NormalMode)
        }
      }
    }
  }
}
