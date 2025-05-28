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

import play.api.mvc.Call
import pages.Page
import config.RefinedTypes.Max5000
import utils.IntUtils.toInt
import cats.implicits.toTraverseOps
import navigation.JourneyNavigator
import models.{NormalMode, UserAnswers}
import pages.nonsipp.moneyborrowed._

object MoneyBorrowedNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ MoneyBorrowedPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.moneyborrowed.routes.WhatYouWillNeedMoneyBorrowedController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedMoneyBorrowedPage(srn) =>
      controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, 1, NormalMode)

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
      controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, index, NormalMode)

    case MoneyBorrowedCYAPage(srn) =>
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, page = 1, NormalMode)

    case BorrowInstancesListPage(srn, addBorrow) =>
      if (addBorrow) {
        (
          for {
            indexes <- userAnswers
              .map(LenderNamePages(srn))
              .keys
              .toList
              .traverse(_.toIntOption)
              .getOrRecoverJourney
            nextIndex <- findNextOpenIndex[Max5000.Refined](indexes).getOrRecoverJourney
          } yield controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, nextIndex, NormalMode)
        ).merge
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
            .onPageLoad(srn, index, NormalMode)

        case IsLenderConnectedPartyPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, NormalMode)

        case BorrowedAmountAndRatePage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, NormalMode)

        case WhenBorrowedPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, NormalMode)

        case ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, NormalMode)

        case WhySchemeBorrowedMoneyPage(srn, index) =>
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
            .onPageLoad(srn, index, NormalMode)
      }
}
