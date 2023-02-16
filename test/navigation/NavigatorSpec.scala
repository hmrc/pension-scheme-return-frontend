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

import config.Refined.OneToTen
import controllers.routes
import models._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages._
import utils.BaseSpec
import eu.timepit.refined._

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

    "go from check return dates page to scheme bank account page" when {

      "yes is selected" in {
        forAll(srnGen) { srn =>
          val ua = userAnswers.set(CheckReturnDatesPage(srn), true).get
          navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe
            routes.SchemeBankAccountController.onPageLoad(srn, refineMV[OneToTen](1), NormalMode)
        }
      }
    }

    "go from check return dates page to unauthorised page" when {

      "no is selected" in {

        forAll(srnGen) { srn =>
          val ua = userAnswers.set(CheckReturnDatesPage(srn), false).get
          navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe routes.UnauthorisedController.onPageLoad
        }
      }
    }

    "go from scheme bank account page to scheme bank account summary page" when {

      "a valid bank account is entered" in {
        forAll(srnGen) { srn =>
          val ua = userAnswers.set(SchemeBankAccountPage(srn), List(BankAccount("test", "12345678", "123456"))).get
          navigator.nextPage(SchemeBankAccountPage(srn), NormalMode, ua) mustBe routes.SchemeBankAccountSummaryController.onPageLoad(srn)
        }
      }
    }

    "go from scheme bank account summary page to scheme bank account page" when {

      "yes is selected" in {
        forAll(srnGen) { srn =>
          val ua = userAnswers.set(SchemeBankAccountPage(srn), List(BankAccount("test", "12345678", "123456"))).get
          navigator.nextPage(SchemeBankAccountSummaryPage(srn, addBankAccount = true), NormalMode, ua) mustBe
            routes.SchemeBankAccountController.onPageLoad(srn, refineMV[OneToTen](2), NormalMode)
        }
      }
    }
  }
}
