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

    "go from check return dates page to scheme bank account page" when {

      "yes is selected" in {
        forAll(srnGen) { srn =>
          val ua = userAnswers.set(CheckReturnDatesPage(srn), true).get
          navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe routes.SchemeBankAccountController.onPageLoad(srn, NormalMode)
        }
      }
    }

    "go from check return dates page to unauthorised page" when {

      "no is selected" in {

        forAll(srnGen) { srn =>
          val ua = userAnswers.set(CheckReturnDatesPage(srn), false).get
          navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe routes.AccountingPeriodController.onPageLoad(srn, NormalMode)
        }
      }
    }

    "go from scheme bank account page to check your answers page" in {

      forAll(srnGen) { srn =>
        navigator.nextPage(SchemeBankAccountPage(srn), NormalMode, userAnswers) mustBe routes.SchemeBankAccountCheckYourAnswersController.onPageLoad(srn)
      }
    }
  }
}
