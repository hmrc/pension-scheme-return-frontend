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
import utils.UserAnswersUtils._

class NavigatorSpec extends BaseSpec with ScalaCheckPropertyChecks {

  val navigator = new Navigator
  val userAnswers = UserAnswers("id")

  "Navigator" - {

    "go from a page that doesn't exist in the route map to Index" - {
      "in Normal mode" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, userAnswers) mustBe routes.IndexController.onPageLoad()
      }
    }

    "go from start page to which tax year page" in {

      forAll(srnGen) { srn =>
        navigator.nextPage(StartPage(srn), NormalMode, userAnswers) mustBe routes.WhichTaxYearController
          .onPageLoad(srn, NormalMode)
      }
    }

    "go from which tax year page to check return dates page" in {

      forAll(srnGen) { srn =>
        navigator.nextPage(WhichTaxYearPage(srn), NormalMode, userAnswers) mustBe routes.CheckReturnDatesController
          .onPageLoad(srn, NormalMode)
      }
    }

    "check returns page" - {

      "navigate to how active bank account" - {
        "yes is selected" in {
          forAll(srnGen) { srn =>
            val ua = userAnswers.set(CheckReturnDatesPage(srn), true).get
            navigator.nextPage(CheckReturnDatesPage(srn), NormalMode, ua) mustBe
              routes.ActiveBankAccountController.onPageLoad(srn, NormalMode)
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

    "go from accounting period page to check your answers page" in {

      forAll(srnGen) { srn =>
        val page = AccountingPeriodPage(srn, refineMV(1))
        navigator.nextPage(page, NormalMode, userAnswers) mustBe
          routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, refineMV(1))
      }
    }

    "go from accounting period check your answers page to list page" in {

      forAll(srnGen) { srn =>
        val page = AccountingPeriodCheckYourAnswersPage(srn)
        navigator.nextPage(page, NormalMode, userAnswers) mustBe
          routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)
      }
    }

    "go from accounting periods list page to the next accounting periods page" - {

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
            .unsafeSet(AccountingPeriodPage(srn, refineMV(1)), dateRangeGen.sample.value)
            .unsafeSet(AccountingPeriodPage(srn, refineMV(2)), dateRangeGen.sample.value)

          val page = AccountingPeriodListPage(srn, addPeriod = true)
          navigator.nextPage(page, NormalMode, answers) mustBe
            routes.AccountingPeriodController.onPageLoad(srn, refineMV(3), NormalMode)
        }
      }
    }

    "go from accounting periods list page to how many members page" - {

      "no is selected" in {

        forAll(srnGen) { srn =>
          val page = AccountingPeriodListPage(srn, addPeriod = false)
          navigator.nextPage(page, NormalMode, userAnswers) mustBe
            routes.HowManyMembersController.onPageLoad(srn, NormalMode)
        }
      }

      "3 periods already exist" in {

        forAll(srnGen) { srn =>
          val answers = userAnswers
            .unsafeSet(AccountingPeriodPage(srn, refineMV(1)), dateRangeGen.sample.value)
            .unsafeSet(AccountingPeriodPage(srn, refineMV(2)), dateRangeGen.sample.value)
            .unsafeSet(AccountingPeriodPage(srn, refineMV(3)), dateRangeGen.sample.value)

          val page = AccountingPeriodListPage(srn, addPeriod = true)
          navigator.nextPage(page, NormalMode, answers) mustBe
            routes.HowManyMembersController.onPageLoad(srn, NormalMode)
        }
      }
    }

    "go from remove accounting period page to accounting list page" in {

      forAll(srnGen) { srn =>
        val page = RemoveAccountingPeriodPage(srn)
        navigator.nextPage(page, NormalMode, userAnswers) mustBe
          routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)
      }
    }

    "go from remove member details page to scheme members list" in {

      forAll(srnGen) { srn =>
        val page = RemoveMemberDetailsPage(srn)
        navigator.nextPage(page, NormalMode, userAnswers) mustBe
          routes.SchemeMembersListController.onPageLoad(srn, page = 1)
      }
    }

    "go from pension scheme members page to Enter member details page on Manual" in {

      forAll(srnGen) { srn =>
        val page = PensionSchemeMembersPage(srn)
        val answers = userAnswers.unsafeSet(page, ManualOrUpload.Manual)
        navigator.nextPage(page, NormalMode, answers) mustBe routes.MemberDetailsController.onPageLoad(srn, refineMV(1))
      }
    }

    "go from pension scheme members page to unauthorised on Upload" in {

      forAll(srnGen) { srn =>
        val page = PensionSchemeMembersPage(srn)
        val answers = userAnswers.unsafeSet(page, ManualOrUpload.Upload)
        navigator.nextPage(page, NormalMode, answers) mustBe routes.UnauthorisedController.onPageLoad()
      }
    }

    "go from how much cash page to value of assets page" in {

      forAll(srnGen) { srn =>
        val page = HowMuchCashPage(srn)

        navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.ValueOfAssetsController
          .onPageLoad(srn, NormalMode)
      }
    }

    "go from value of assets page to scheme members manual or upload page" in {

      forAll(srnGen) { srn =>
        val page = ValueOfAssetsPage(srn)
        navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.PensionSchemeMembersController.onPageLoad(srn)
      }
    }

    "go from psa declaration page to unauthorised" in {

      forAll(srnGen) { srn =>
        val page = PsaDeclarationPage(srn)
        navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.UnauthorisedController.onPageLoad()
      }
    }

    "go from psp declaration page to unauthorised" in {

      forAll(srnGen) { srn =>
        val page = PspDeclarationPage(srn)
        navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.UnauthorisedController.onPageLoad()

      }
    }

    "go from member details page to does member have nino page" in {

      forAll(srnGen) { srn =>
        val page = MemberDetailsPage(srn, refineMV(1))
        navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.DoesSchemeMemberHaveNINOController.onPageLoad(
          srn,
          refineMV(1),
          NormalMode
        )
      }
    }

    "go from member details nino page to member details check your answers page" in {

      forAll(srnGen) { srn =>
        val page = MemberDetailsNinoPage(srn, refineMV(1))
        navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.SchemeMemberDetailsAnswersController.onPageLoad(
          srn,
          refineMV(1),
          CheckOrChange.Check
        )
      }
    }

    "go from how many members page to how much cash page" - {

      "total number of members is less than 100" in {

        forAll(srnGen, pensionSchemeIdGen) { (srn, id) =>
          val page = HowManyMembersPage(srn, id)
          val ua = userAnswers.unsafeSet(page, SchemeMemberNumbers(50, 30, 19))

          navigator.nextPage(page, NormalMode, ua) mustBe
            routes.HowMuchCashController.onPageLoad(srn, NormalMode)
        }
      }
    }

    "go from how many members page to declaration" - {

      "psa is signed in" in {

        forAll(srnGen, psaIdGen) { (srn, id) =>
          val page = HowManyMembersPage(srn, id)
          val ua = userAnswers.unsafeSet(page, SchemeMemberNumbers(50, 30, 20))

          navigator.nextPage(page, NormalMode, ua) mustBe
            routes.PsaDeclarationController.onPageLoad(srn)
        }
      }

      "psp is signed in" in {

        forAll(srnGen, pspIdGen) { (srn, id) =>
          val page = HowManyMembersPage(srn, id)
          val ua = userAnswers.unsafeSet(page, SchemeMemberNumbers(50, 30, 20))

          navigator.nextPage(page, NormalMode, ua) mustBe
            routes.PspDeclarationController.onPageLoad(srn)
        }
      }
    }
  }
}
