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
import config.Refined.{Max5000, OneTo5000}
import models.SchemeId.Srn
import eu.timepit.refined.refineMV
import navigation.{Navigator, NavigatorBehaviours}
import models.{CheckOrChange, NormalMode, UserAnswers}
import pages.nonsipp.moneyborrowed._
import models.CheckOrChange.Check
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

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
            controllers.nonsipp.moneyborrowed.routes.IsLenderConnectedPartyController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from lender name page to is lender connected party page")
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
          (srn, _: Max5000, _) =>
            controllers.nonsipp.moneyborrowed.routes.ValueOfSchemeAssetsWhenMoneyBorrowedController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from when amount was borrowed rate page to value of scheme assets when money borrowed page")
    )
  }

  "ValueOfSchemeAssetsWhenMoneyBorrowedPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          ValueOfSchemeAssetsWhenMoneyBorrowedPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.moneyborrowed.routes.WhySchemeBorrowedMoneyController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from value of scheme assets when money borrowed page to unauthorised controller")
    )
  }

  "WhySchemeBorrowedMoneyPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhySchemeBorrowedMoneyPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
              .onPageLoad(srn, index, CheckOrChange.Check)
        )
        .withName("go from why scheme borrowed amount page to money borrowed CYA page")
    )
  }

  "RemoveBorrowInstancesPage" - {
    act.like(
      normalmode
        .navigateTo(
          srn => RemoveBorrowInstancesPage(srn, refineMV(1)),
          (srn, _) => controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, NormalMode)
        )
        .withName("go from remove page to Money Borrowed page")
    )

    val completedLoanUserAnswers: Srn => UserAnswers =
      srn => defaultUserAnswers.unsafeSet(LenderNamePage(srn, index), "LenderName")

    act.like(
      normalmode
        .navigateTo(
          srn => RemoveBorrowInstancesPage(srn, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
              .onPageLoad(srn = srn, page = 1, mode = NormalMode),
          completedLoanUserAnswers
        )
        .withName("go from remove page to Borrow Instances List page")
    )
  }

  "BorrowInstancesListPage" - {
    "Record at index 1" - {
      act.like(
        normalmode
          .navigateTo(
            srn => BorrowInstancesListPage(srn, addBorrow = true),
            (srn, mode) =>
              controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, refineMV(2), mode),
            srn =>
              defaultUserAnswers
                .unsafeSet(BorrowedAmountAndRatePage(srn, refineMV(1)), (money, percentage))
          )
          .withName("Add Borrow instance with a record at index 1")
      )
    }

    "Record at index 2" - {
      act.like(
        normalmode
          .navigateTo(
            srn => BorrowInstancesListPage(srn, addBorrow = true),
            (srn, mode) =>
              controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, refineMV(1), mode),
            srn =>
              defaultUserAnswers
                .unsafeSet(BorrowedAmountAndRatePage(srn, refineMV(2)), (money, percentage))
          )
          .withName("Add Borrow instance with a record at index 2")
      )
    }

    "No records" - {
      act.like(
        normalmode
          .navigateTo(
            srn => BorrowInstancesListPage(srn, addBorrow = true),
            (srn, mode) =>
              controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, refineMV(1), mode)
          )
          .withName("Add Borrow instance with no records")
      )
    }
  }

  "MoneyBorrowedNavigator in check mode" - {

    act.like(
      checkmode
        .navigateTo(
          LenderNamePage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from lender name page to money borrowed CYA page")
    )

    act.like(
      checkmode
        .navigateTo(
          IsLenderConnectedPartyPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from is lender connected party page to money borrowed CYA page")
    )

    act.like(
      checkmode
        .navigateTo(
          BorrowedAmountAndRatePage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from borrowed amount and interest rate page to money borrowed CYA page")
    )

    act.like(
      checkmode
        .navigateTo(
          WhenBorrowedPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from when amount was borrowed rate page to money borrowed CYA page")
    )

    act.like(
      checkmode
        .navigateTo(
          ValueOfSchemeAssetsWhenMoneyBorrowedPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from value of scheme assets when money borrowed page to money borrowed CYA page")
    )

    act.like(
      checkmode
        .navigateTo(
          WhySchemeBorrowedMoneyPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from why scheme borrowed amount page to money borrowed CYA page")
    )

  }

}
