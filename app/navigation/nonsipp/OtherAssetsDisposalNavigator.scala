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
import pages.Page
import config.RefinedTypes.Max50
import utils.IntUtils.toInt
import cats.implicits.{catsSyntaxEitherId, toBifunctorOps, toTraverseOps}
import eu.timepit.refined.refineV
import navigation.JourneyNavigator
import models._
import viewmodels.models.SectionJourneyStatus

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

    case OtherAssetsDisposalListPage(srn, assetIndex) => {
      val inProgressIndex: Either[Call, Option[Max50]] = userAnswers
        .map(OtherAssetsDisposalProgress.all(srn, assetIndex))
        .find {
          case (_, SectionJourneyStatus.InProgress(_)) => true
          case _ => false
        }
        .flatTraverse {
          case (index, _) => index.toIntOption.traverse(i => refineV[Max50.Refined](i + 1))
        }
        .leftMap(_ => controllers.routes.JourneyRecoveryController.onPageLoad())

      inProgressIndex.flatMap {
        case Some(nextIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController
            .onPageLoad(srn, assetIndex, nextIndex, NormalMode)
            .asRight
        case None =>
          for {
            indexes <- userAnswers
              .map(OtherAssetsDisposalProgress.all(srn, assetIndex))
              .keys
              .toList
              .traverse(_.toIntOption)
              .getOrRecoverJourney
            nextIndex <- findNextOpenIndex[Max50.Refined](indexes).getOrRecoverJourney
          } yield controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController
            .onPageLoad(srn, assetIndex, nextIndex, NormalMode)
      }
    }.merge

    case page @ HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex, _) =>
      userAnswers.get(page) match {
        case None => controllers.routes.JourneyRecoveryController.onPageLoad()
        case Some(HowDisposed.Sold) =>
          controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
        case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
          controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
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
      if (userAnswers.get(TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex)).isEmpty ||
        userAnswers.get(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex)).isEmpty ||
        userAnswers.get(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex)).isEmpty) {
        controllers.nonsipp.otherassetsdisposal.routes.AssetSaleIndependentValuationController
          .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
      } else {
        controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
          .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
      }

    case AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case OtherAssetsDisposalCYAPage(srn) =>
      controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, 1)

    case RemoveAssetDisposalPage(srn) =>
      if (!userAnswers
          .map(HowWasAssetDisposedOfPages(srn))
          .exists(_._2.nonEmpty)) {
        controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
          .onPageLoad(srn, page = 1)
      }

    case ReportedOtherAssetsDisposalListPage(srn, addDisposal @ true) =>
      controllers.nonsipp.otherassetsdisposal.routes.StartReportingAssetsDisposalController.onPageLoad(srn, 1)

    case ReportedOtherAssetsDisposalListPage(srn, addDisposal @ false) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {

        case page @ HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex, hasAnswerChanged) =>
          userAnswers.get(page) match {
            case None => controllers.routes.UnauthorisedController.onPageLoad()
            case _ if hasAnswerChanged =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            case Some(HowDisposed.Sold) =>
              controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
              controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          }

        case WhenWasAssetSoldPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

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

        case CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case AssetCompanyBuyerCrnPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case PartnershipBuyerNamePage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case OtherBuyerDetailsPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case PartnershipBuyerUtrPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex) =>
          controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
            .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)

        case OtherAssetsDisposalCYAPage(srn) =>
          controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, 1)

      }

}
