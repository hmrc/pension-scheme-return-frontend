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

import config.Refined.Max5000
import eu.timepit.refined.{refineMV, refineV}
import models.{CheckOrChange, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.moneyborrowed._
import play.api.mvc.Call

object MoneyBorrowedNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ MoneyBorrowedPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.moneyborrowed.routes.WhatYouWillNeedMoneyBorrowedController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedMoneyBorrowedPage(srn) =>
      controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, refineMV(1), NormalMode)

    case LenderNamePage(srn, index) =>
      controllers.nonsipp.moneyborrowed.routes.IsLenderConnectedPartyController.onPageLoad(srn, index, NormalMode)

    case IsLenderConnectedPartyPage(srn, index) =>
      controllers.nonsipp.moneyborrowed.routes.BorrowedAmountAndRateController.onPageLoad(srn, index, NormalMode)

    case BorrowedAmountAndRatePage(srn, index) =>
      controllers.nonsipp.moneyborrowed.routes.WhenBorrowedController.onPageLoad(srn, index, NormalMode)

    case WhenBorrowedPage(srn, index) =>
      controllers.nonsipp.moneyborrowed.routes.ValueOfSchemeAssetsWhenMoneyBorrowedController
        .onPageLoad(srn, index, NormalMode)

    case ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index) =>
      controllers.nonsipp.moneyborrowed.routes.WhySchemeBorrowedMoneyController.onPageLoad(srn, index, NormalMode)

    case WhySchemeBorrowedMoneyPage(srn, index) =>
      controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, index, CheckOrChange.Check)

    case MoneyBorrowedCYAPage(srn) =>
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, page = 1, NormalMode)

    case BorrowInstancesListPage(srn, addBorrow) =>
      if (addBorrow) {
        val answers = userAnswers.map(BorrowedAmountAndRatePages(srn))
        val nextDataKey = if (answers.isEmpty) 1 else answers.maxBy(_._1)._1.toIntOption.orElse(Some(0)).get + 1
        refineV[Max5000.Refined](nextDataKey + 1).fold(
          err => controllers.routes.JourneyRecoveryController.onPageLoad(),
          nextIndex =>
            controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, nextIndex, NormalMode)
        )
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case RemoveBorrowInstancesPage(srn, _) =>
      if (userAnswers.map(LenderNamePages(srn)).isEmpty) {
        controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
          .onPageLoad(srn = srn, page = 1, mode = NormalMode)
      }
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      _ => {

        case LenderNamePage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, CheckOrChange.Check)

        case IsLenderConnectedPartyPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, CheckOrChange.Check)

        case BorrowedAmountAndRatePage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, CheckOrChange.Check)

        case WhenBorrowedPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, CheckOrChange.Check)

        case ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, CheckOrChange.Check)

        case WhySchemeBorrowedMoneyPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, CheckOrChange.Check)
      }
}
