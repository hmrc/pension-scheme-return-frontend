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

package navigation

import controllers.routes
import eu.timepit.refined._
import models._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages._
import utils.BaseSpec

class NavigatorSpec extends BaseSpec with ScalaCheckPropertyChecks {

  val navigator = new Navigator
  val userAnswers = UserAnswers("id")

  "Navigator" should {

    "go from a page that doesn't exist in the route map to Index" when {
      "in Normal mode" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe routes.IndexController.onPageLoad
      }
    }

    "go from start page to details page" in {

      forAll(srnGen) { srn =>

        navigator.nextPage(StartPage(srn), NormalMode, userAnswers) mustBe routes.SchemeDetailsController.onPageLoad(srn)
      }
    }

    "go from scheme details page to check return dates page" in {

      forAll(srnGen) { srn =>

        navigator.nextPage(SchemeDetailsPage(srn), NormalMode, userAnswers) mustBe routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
      }
    }

    "check returns page" should {

      "navigate to scheme bank account page" when {
        "yes is selected" in {
          forAll(srnGen) { srn =>
            val ua = userAnswers.set(CheckReturnDatesPage(srn), true).get
            navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe
              routes.SchemeBankAccountController.onPageLoad(srn, refineMV(1), NormalMode)
          }
        }
      }

      "navigate to scheme bank account list page" when {
        "yes is selected and a bank account has previously been entered" in {
          forAll(srnGen) { srn =>
            val ua = userAnswers
              .set(CheckReturnDatesPage(srn), true).get
              .set(SchemeBankAccountPage(srn, refineMV(1)), BankAccount("test", "11111111", "111111")).get
            navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe
              routes.SchemeBankAccountListController.onPageLoad(srn)
          }
        }
      }

      "no is selected" in {

        forAll(srnGen) { srn =>
          val ua = userAnswers.set(CheckReturnDatesPage(srn), false).get
          navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe
            routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
        }
      }
    }

    "go from scheme bank account page to check your answers page" in {

      forAll(srnGen) { srn =>
        val page = SchemeBankAccountPage(srn, refineMV(1))
        navigator.nextPage(page, NormalMode, userAnswers) mustBe
          routes.SchemeBankAccountCheckYourAnswersController.onPageLoad(srn, refineMV(1))
      }
    }

    "go from bank account check your answers page to list page" in {

      forAll(srnGen) { srn =>
        val page = SchemeBankAccountCheckYourAnswersPage(srn)
        navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.SchemeBankAccountListController.onPageLoad(srn)
      }
    }

    "go from scheme bank account list page to scheme bank account page" when {

      "yes is selected" in {
        forAll(srnGen) { srn =>
          val ua = userAnswers.set(SchemeBankAccountPage(srn, refineMV(1)), BankAccount("test", "11111111", "111111")).get
          val page = SchemeBankAccountListPage(srn, addBankAccount = true)
          navigator.nextPage(page, NormalMode, ua) mustBe
            routes.SchemeBankAccountController.onPageLoad(srn, refineMV(2), NormalMode)
        }
      }
    }

    "go from accounting periods list page to the next accounting periods page" when {

      "yes is selected and 1 period exists" in {

        forAll(srnGen) { srn =>

          val answers = userAnswers.set(AccountingPeriodPage(srn, refineMV(1)), dateRangeGen.sample.value).get
          val page = AccountingPeriodListPage(srn, addPeriod = true)
          navigator.nextPage(page, NormalMode, answers) mustBe
            routes.AccountingPeriodController.onPageLoad(srn, refineMV(2), NormalMode)
        }
      }

      "yes is selected and 2 periods exists" in {

        forAll(srnGen) { srn =>

          val answers = userAnswers
            .set(AccountingPeriodPage(srn, refineMV(1)), dateRangeGen.sample.value).get
            .set(AccountingPeriodPage(srn, refineMV(2)), dateRangeGen.sample.value).get

          val page = AccountingPeriodListPage(srn, addPeriod = true)
          navigator.nextPage(page, NormalMode, answers) mustBe
            routes.AccountingPeriodController.onPageLoad(srn, refineMV(3), NormalMode)
        }
      }
    }

    "got from accounting periods list page to unauthorised page" when {

      "no is selected" in {

        forAll(srnGen) { srn =>

          val page = AccountingPeriodListPage(srn, addPeriod = false)
          navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.UnauthorisedController.onPageLoad
        }
      }

      "3 periods already exist" in {

        forAll(srnGen) { srn =>

          val answers = userAnswers
            .set(AccountingPeriodPage(srn, refineMV(1)), dateRangeGen.sample.value).get
            .set(AccountingPeriodPage(srn, refineMV(2)), dateRangeGen.sample.value).get
            .set(AccountingPeriodPage(srn, refineMV(3)), dateRangeGen.sample.value).get

          val page = AccountingPeriodListPage(srn, addPeriod = true)
          navigator.nextPage(page, NormalMode, answers) mustBe routes.UnauthorisedController.onPageLoad
        }
      }
    }
  }
}
