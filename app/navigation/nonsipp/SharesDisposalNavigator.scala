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

import models.PointOfEntry._
import models.{CheckMode, HowSharesDisposed, IdentityType, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.sharesdisposal._
import play.api.mvc.Call

object SharesDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case WhoWereTheSharesSoldToPage(srn, index, disposalIndex) =>
      userAnswers.get(WhoWereTheSharesSoldToPage(srn, index, disposalIndex)) match {

        case Some(IdentityType.Individual) =>
          controllers.nonsipp.sharesdisposal.routes.SharesIndividualBuyerNameController
            .onPageLoad(srn, index, disposalIndex, NormalMode)

        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.sharesdisposal.routes.CompanyNameOfSharesBuyerController
            .onPageLoad(srn, index, disposalIndex, NormalMode)

        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerNameController
            .onPageLoad(srn, index, disposalIndex, NormalMode)

        case Some(IdentityType.Other) =>
          controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
            .onPageLoad(srn, index, disposalIndex, NormalMode)
      }

    case RemoveShareDisposalPage(srn, index, disposalIndex) =>
      if (!userAnswers
          .map(HowWereSharesDisposedPages(srn))
          .exists(_._2.nonEmpty)) {
        controllers.nonsipp.sharesdisposal.routes.SharesDisposalController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
          .onPageLoad(srn, page = 1)
      }

    case SharesIndividualBuyerNamePage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case CompanyBuyerNamePage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.CompanyBuyerCrnController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case PartnershipBuyerNamePage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerUtrController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case IndividualBuyerNinoNumberPage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case CompanyBuyerCrnPage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case PartnershipBuyerUtrPage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case OtherBuyerDetailsPage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.IndependentValuationController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case page @ SharesDisposalPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.sharesdisposal.routes.WhatYouWillNeedSharesDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedSharesDisposalPage(srn) =>
      controllers.nonsipp.sharesdisposal.routes.SharesDisposalListController
        .onPageLoad(srn, page = 1)

    case page @ SharesDisposalListPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.HowWereSharesDisposedController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case page @ HowWereSharesDisposedPage(srn, shareIndex, disposalIndex, _) =>
      userAnswers.get(page) match {
        case None =>
          controllers.routes.JourneyRecoveryController.onPageLoad()
        case Some(HowSharesDisposed.Sold) =>
          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
        case Some(HowSharesDisposed.Redeemed) =>
          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
        case Some(HowSharesDisposed.Transferred) | Some(HowSharesDisposed.Other(_)) =>
          controllers.nonsipp.sharesdisposal.routes.HowManySharesController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
      }

    case WhenWereSharesSoldPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.HowManySharesSoldController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.HowManySharesRedeemedController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case HowManySharesSoldPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesSoldController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.WhoWereTheSharesSoldToController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case HowManySharesRedeemedPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesRedeemedController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case TotalConsiderationSharesRedeemedPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.HowManySharesController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case IndependentValuationPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.HowManySharesController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case HowManyDisposalSharesPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case SharesDisposalCompletedPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
        .onPageLoad(srn, page = 1)

    case ReportedSharesDisposalListPage(srn, addDisposal @ true) =>
      controllers.nonsipp.sharesdisposal.routes.SharesDisposalListController.onPageLoad(srn, 1)

    case ReportedSharesDisposalListPage(srn, addDisposal @ false) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {

        case page @ HowWereSharesDisposedPage(srn, shareIndex, disposalIndex, _) => //c
          userAnswers.get(page) match {
            case None =>
              controllers.routes.JourneyRecoveryController.onPageLoad()
            case Some(HowSharesDisposed.Sold) =>
              controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController //d
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
            case Some(HowSharesDisposed.Redeemed) =>
              controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController //l
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
            case Some(HowSharesDisposed.Transferred) | Some(HowSharesDisposed.Other(_)) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
          }

        case WhenWereSharesSoldPage(srn, shareIndex, disposalIndex) => //d
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.HowManySharesSoldController //e
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case HowManySharesSoldPage(srn, shareIndex, disposalIndex) => //e
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesSoldController //f
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex) => //f
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.WhoWereTheSharesSoldToController //g
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex) => //g
          // Ignore PointOfEntry here, as the next page in CheckMode is always the 'BuyerNamePage' for that IdentityType
          userAnswers.get(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex)) match {
            case Some(IdentityType.Individual) =>
              controllers.nonsipp.sharesdisposal.routes.SharesIndividualBuyerNameController //h
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
            case Some(IdentityType.UKCompany) =>
              controllers.nonsipp.sharesdisposal.routes.CompanyNameOfSharesBuyerController //i
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
            case Some(IdentityType.UKPartnership) =>
              controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerNameController //j
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
            case Some(IdentityType.Other) =>
              controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController //k
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex) => //h
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController //h1
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case CompanyBuyerNamePage(srn, shareIndex, disposalIndex) => //i
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.CompanyBuyerCrnController //i1
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case PartnershipBuyerNamePage(srn, shareIndex, disposalIndex) => //j
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerUtrController //j1
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case OtherBuyerDetailsPage(srn, shareIndex, disposalIndex) => //k
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController //o
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex) => //h1
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController //o
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case CompanyBuyerCrnPage(srn, shareIndex, disposalIndex) => //i1
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController //o
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case PartnershipBuyerUtrPage(srn, shareIndex, disposalIndex) => //j1
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController //o
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex) => //o
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) | Some(WhoWereTheSharesSoldToPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.IndependentValuationController //p
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case IndependentValuationPage(srn, shareIndex, disposalIndex) => //p
          controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

        case WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex) => //l
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.HowManySharesRedeemedController //m
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case HowManySharesRedeemedPage(srn, shareIndex, disposalIndex) => //m
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) match {
            case Some(NoPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            case Some(HowWereSharesDisposedPointOfEntry) =>
              controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesRedeemedController //n
                .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
          }

        case TotalConsiderationSharesRedeemedPage(srn, shareIndex, disposalIndex) => //n
          controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

        case HowManyDisposalSharesPage(srn, shareIndex, disposalIndex) => //q
          controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

        case SharesDisposalCompletedPage(srn, shareIndex, disposalIndex) =>
          controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
            .onPageLoad(srn, page = 1)
      }
}
