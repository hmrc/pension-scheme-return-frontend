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
import models.PointOfEntry.{HowWereBondsDisposedPointOfEntry, NoPointOfEntry}
import cats.implicits.toTraverseOps
import navigation.JourneyNavigator
import models._
import pages.nonsipp.bondsdisposal._
import config.Refined.Max50
import pages.Page

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

    case BondsDisposalListPage(srn, bondIndex) =>
      (
        for {
          indexes <- userAnswers
            .map(BondsDisposalCompleted.all(srn, bondIndex))
            .keys
            .toList
            .traverse(_.toIntOption)
            .getOrRecoverJourney
          nextIndex <- findNextOpenIndex[Max50.Refined](indexes).getOrRecoverJourney
        } yield controllers.nonsipp.bondsdisposal.routes.HowWereBondsDisposedOfController
          .onPageLoad(srn, bondIndex, nextIndex, NormalMode)
      ).merge

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
      controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
        .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)

    case BondsDisposalCompletedPage(srn, _, _) =>
      controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
        .onPageLoad(srn, page = 1)

    case ReportBondsDisposalListPage(srn, addDisposal @ true) =>
      controllers.nonsipp.bondsdisposal.routes.BondsDisposalListController.onPageLoad(srn, 1, NormalMode)

    case ReportBondsDisposalListPage(srn, addDisposal @ false) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case RemoveBondsDisposalPage(srn, bondIndex, disposalIndex) =>
      if (!userAnswers
          .map(HowWereBondsDisposedOfPages(srn))
          .exists(_._2.nonEmpty)) {
        controllers.nonsipp.bondsdisposal.routes.BondsDisposalController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController.onPageLoad(srn, page = 1)
      }

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {

        case page @ HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex, _) =>
          userAnswers.get(page) match {
            case Some(HowDisposed.Sold) =>
              controllers.nonsipp.bondsdisposal.routes.WhenWereBondsSoldController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case None =>
              controllers.routes.JourneyRecoveryController.onPageLoad()
          }

        case WhenWereBondsSoldPage(srn, bondIndex, disposalIndex) =>
          userAnswers.get(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case Some(HowWereBondsDisposedPointOfEntry) =>
              controllers.nonsipp.bondsdisposal.routes.TotalConsiderationSaleBondsController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case _ =>
              controllers.routes.JourneyRecoveryController.onPageLoad()
          }

        case TotalConsiderationSaleBondsPage(srn, bondIndex, disposalIndex) =>
          userAnswers.get(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case Some(HowWereBondsDisposedPointOfEntry) =>
              controllers.nonsipp.bondsdisposal.routes.BuyerNameController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case _ =>
              controllers.routes.JourneyRecoveryController.onPageLoad()
          }

        case BuyerNamePage(srn, bondIndex, disposalIndex) =>
          userAnswers.get(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case Some(HowWereBondsDisposedPointOfEntry) =>
              controllers.nonsipp.bondsdisposal.routes.IsBuyerConnectedPartyController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
            case _ =>
              controllers.routes.JourneyRecoveryController.onPageLoad()
          }

        case IsBuyerConnectedPartyPage(srn, bondIndex, disposalIndex) =>
          controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
            .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)

        case BondsStillHeldPage(srn, bondIndex, disposalIndex) =>
          controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
            .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)

        case BondsDisposalCompletedPage(srn, _, _) =>
          controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
            .onPageLoad(srn, page = 1)

      }
}
