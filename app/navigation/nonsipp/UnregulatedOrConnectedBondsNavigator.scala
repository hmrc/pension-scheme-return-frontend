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

import eu.timepit.refined.refineMV
import models.SchemeHoldBond.{Acquisition, Contribution, Transfer}
import models.{CheckMode, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.unregulatedorconnectedbonds._
import play.api.mvc.Call

object UnregulatedOrConnectedBondsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ UnregulatedOrConnectedBondsHeldPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.unregulatedorconnectedbonds.routes.WhatYouWillNeedBondsController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedBondsPage(srn) =>
      controllers.nonsipp.unregulatedorconnectedbonds.routes.NameOfBondsController
        .onPageLoad(srn, refineMV(1), NormalMode)

    case NameOfBondsPage(srn, index) =>
      controllers.nonsipp.unregulatedorconnectedbonds.routes.WhyDoesSchemeHoldBondsController
        .onPageLoad(srn, index, NormalMode)

    case WhyDoesSchemeHoldBondsPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, index)) match {
        case Some(Acquisition) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.WhenDidSchemeAcquireBondsController
            .onPageLoad(srn, index, NormalMode)
        case Some(Contribution) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.WhenDidSchemeAcquireBondsController
            .onPageLoad(srn, index, NormalMode)
        case Some(Transfer) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.CostOfBondsController
            .onPageLoad(srn, index, NormalMode)
        case _ => controllers.routes.UnauthorisedController.onPageLoad()
      }

    case WhenDidSchemeAcquireBondsPage(srn, index) =>
      controllers.nonsipp.unregulatedorconnectedbonds.routes.CostOfBondsController
        .onPageLoad(srn, index, NormalMode)

    case CostOfBondsPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, index)) match {
        case Some(Acquisition) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.BondsFromConnectedPartyController
            .onPageLoad(srn, index, NormalMode)
        case _ =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.AreBondsUnregulatedController
            .onPageLoad(srn, index, NormalMode)
      }

    case BondsFromConnectedPartyPage(srn, index) =>
      controllers.nonsipp.unregulatedorconnectedbonds.routes.AreBondsUnregulatedController
        .onPageLoad(srn, index, NormalMode)

    case AreBondsUnregulatedPage(srn, index) =>
      controllers.nonsipp.unregulatedorconnectedbonds.routes.IncomeFromBondsController
        .onPageLoad(srn, index, NormalMode)

    case IncomeFromBondsPage(srn, index) =>
      controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
        .onPageLoad(srn, index, NormalMode)

    case UnregulatedOrConnectedBondsHeldCYAPage(srn) =>
      controllers.routes.UnauthorisedController.onPageLoad()
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {
        case NameOfBondsPage(srn, index) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
            .onPageLoad(srn, index, CheckMode)

        case WhyDoesSchemeHoldBondsPage(srn, index) =>
          userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, index)) match {
            case Some(Acquisition) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.WhenDidSchemeAcquireBondsController
                .onPageLoad(srn, index, CheckMode)
            case Some(Contribution) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.WhenDidSchemeAcquireBondsController
                .onPageLoad(srn, index, CheckMode)
            case Some(Transfer) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                .onPageLoad(srn, index, CheckMode)
            case _ => controllers.routes.UnauthorisedController.onPageLoad()
          }

        case WhenDidSchemeAcquireBondsPage(srn, index) =>
          userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, index)) match {
            case Some(Acquisition) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.BondsFromConnectedPartyController
                .onPageLoad(srn, index, CheckMode)
            case _ =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
                .onPageLoad(srn, index, CheckMode)
          }

        case CostOfBondsPage(srn, index) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
            .onPageLoad(srn, index, CheckMode)

        case BondsFromConnectedPartyPage(srn, index) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
            .onPageLoad(srn, index, CheckMode)

        case AreBondsUnregulatedPage(srn, index) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
            .onPageLoad(srn, index, CheckMode)

        case IncomeFromBondsPage(srn, index) =>
          controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldCYAController
            .onPageLoad(srn, index, CheckMode)
      }
}
