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

package navigation.nonsipp

import utils.BaseSpec
import config.RefinedTypes.OneToThree
import controllers.nonsipp.schemedesignatory
import controllers.nonsipp.accountingperiod.routes
import pages.nonsipp.accountingperiod._
import navigation.{Navigator, NavigatorBehaviours}
import models.NormalMode
import generators.IndexGen
import utils.IntUtils.given

class AccountingPeriodNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "AccountPeriodNavigator" - {

    act.like(
      normalmode
        .navigateTo(
          AccountingPeriodPage(_, 1, NormalMode),
          (srn, _) => routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from accounting period page to check answers page")
    )

    act.like(
      normalmode
        .navigateTo(
          AccountingPeriodCheckYourAnswersPage(_, NormalMode),
          routes.AccountingPeriodListController.onPageLoad
        )
        .withName("go from check your answers page to list page")
    )

    act.like(
      normalmode
        .navigateFromListPage(
          AccountingPeriodListPage(_, addPeriod = true, NormalMode),
          AccountingPeriodPage(_, _, NormalMode),
          dateRangeGen,
          IndexGen[OneToThree](1, 3),
          routes.AccountingPeriodController.onPageLoad,
          schemedesignatory.routes.ActiveBankAccountController.onPageLoad
        )
        .withName("go from list page")
    )

    act.like(
      normalmode
        .navigateTo(
          AccountingPeriodListPage(_, addPeriod = false, NormalMode),
          schemedesignatory.routes.ActiveBankAccountController.onPageLoad
        )
        .withName("go from list page to bank account page when no selected")
    )

    act.like(
      normalmode
        .navigateTo(
          RemoveAccountingPeriodPage(_, NormalMode),
          routes.AccountingPeriodListController.onPageLoad
        )
        .withName("go from remove page to list page")
    )
  }
}
