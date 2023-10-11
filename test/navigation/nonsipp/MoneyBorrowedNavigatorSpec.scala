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

package navigation.nonsipp

import config.Refined.{Max5000, OneTo5000}
import eu.timepit.refined.refineMV
import models.NormalMode
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.moneyborrowed._

import utils.BaseSpec

class MoneyBorrowedNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)

  "MoneyBorrowedNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          MoneyBorrowedPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.moneyborrowed.routes.WhatYouWillNeedMoneyBorrowedController.onPageLoad(srn)
        )
        .withName("go from money borrowed page to what you will need money borrowed when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          MoneyBorrowedPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from money borrowed page to task list controller page when no selected")
    )
  }

  "WhatYouWillNeedMoneyBorrowedPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          (srn, _: Max5000) => WhatYouWillNeedMoneyBorrowedPage(srn),
          controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad
        )
        .withName("go from what you will need money borrowed page to lender name page")
    )
  }

  "LenderNamePage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          LenderNamePage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.moneyborrowed.routes.BorrowedAmountAndRateController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from lender name page to borrowed amount and rate page")
    )
  }

  "BorrowedAmountAndRatePage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          BorrowedAmountAndRatePage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.moneyborrowed.routes.WhenBorrowedController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from borrowed amount and interest rate to when amount was borrowed rate page")
    )
  }

  "WhenBorrowedPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhenBorrowedPage,
          (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from when amount was borrowed rate page to unauthorised controller")
    )
  }

}
