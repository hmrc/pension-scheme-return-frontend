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

import pages.nonsipp.otherassetsdisposal._
import play.api.mvc.Call
import cats.implicits.toTraverseOps
import navigation.JourneyNavigator
import models._
import config.Refined.Max50
import pages.Page

object OtherAssetsDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ OtherAssetsDisposalPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.otherassetsdisposal.routes.WhatYouWillNeedOtherAssetsDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedOtherAssetsDisposalPage(srn) =>
      controllers.nonsipp.otherassetsdisposal.routes.StartReportingAssetsDisposalController.onPageLoad(srn, page = 1)

    case OtherAssetsDisposalListPage(srn, assetIndex) =>
      (
        for {
          disposalIndexes <- userAnswers
            .map(OtherAssetsDisposalCompleted.all(srn, assetIndex))
            .keys
            .toList
            .traverse(_.toIntOption)
            .getOrRecoverJourney
          nextIndex <- findNextOpenIndex[Max50.Refined](disposalIndexes).getOrRecoverJourney
        } yield controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController
          .onPageLoad(srn, assetIndex, nextIndex, NormalMode)
      ).merge

    case page @ HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex, _) =>
      userAnswers.get(page) match {
        case None => controllers.routes.JourneyRecoveryController.onPageLoad()
        case Some(HowDisposed.Sold) =>
          controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
        case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case WhenWasAssetSoldPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.TotalConsiderationSaleAssetController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.TypeOfAssetBuyerController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex) =>
      userAnswers.get(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex)) match {
        case Some(IdentityType.Individual) =>
          controllers.nonsipp.otherassetsdisposal.routes.IndividualNameOfAssetBuyerController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.otherassetsdisposal.routes.CompanyNameOfAssetBuyerController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerNameController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
        case Some(IdentityType.Other) =>
          controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
        case None =>
          controllers.routes.JourneyRecoveryController.onPageLoad()
      }

    case IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.AssetIndividualBuyerNiNumberController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case PartnershipBuyerNamePage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerUtrController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case PartnershipBuyerUtrPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case OtherBuyerDetailsPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.AssetCompanyBuyerCrnController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case AssetCompanyBuyerCrnPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ => _ => PartialFunction.empty

}
