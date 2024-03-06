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

import models.{HowDisposed, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.bondsdisposal._
import play.api.mvc.Call

object BondsDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ BondsDisposalPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.bondsdisposal.routes.WhatYouWillNeedBondsDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedBondsDisposalPage(srn) =>
      controllers.nonsipp.bondsdisposal.routes.BondsDisposalListController.onPageLoad(srn, 1, NormalMode)

    case BondsDisposalListPage(srn, bondIndex, disposalIndex) =>
      controllers.nonsipp.bondsdisposal.routes.HowWereBondsDisposedOfController
        .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)

    case page @ HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex, _) =>
      userAnswers.get(page) match {
        case None => controllers.routes.JourneyRecoveryController.onPageLoad()
        case Some(HowDisposed.Sold) =>
          controllers.nonsipp.bondsdisposal.routes.WhenWereBondsSoldController
            .onSubmit(srn, bondIndex, disposalIndex, NormalMode)
        case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
          controllers.nonsipp.bondsdisposal.routes.BondsStillHeldController
            .onSubmit(srn, bondIndex, disposalIndex, NormalMode)
      }

    case WhenWereBondsSoldPage(srn, bondIndex, disposalIndex) =>
      controllers.nonsipp.bondsdisposal.routes.TotalConsiderationSaleBondsController
        .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)

    case TotalConsiderationSaleBondsPage(srn, bondIndex, disposalIndex) =>
      controllers.nonsipp.bondsdisposal.routes.BuyerNameController.onPageLoad(srn, bondIndex, disposalIndex, NormalMode)

    case BuyerNamePage(srn, bondIndex, disposalIndex) =>
      controllers.nonsipp.bondsdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)

    case IsBuyerConnectedPartyPage(srn, bondIndex, disposalIndex) =>
      controllers.nonsipp.bondsdisposal.routes.BondsStillHeldController
        .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)

    case BondsStillHeldPage(srn, bondIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ => _ => PartialFunction.empty
}
