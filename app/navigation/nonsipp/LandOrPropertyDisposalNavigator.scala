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

import play.api.mvc.Call
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPages
import cats.implicits.toTraverseOps
import pages.nonsipp.landorpropertydisposal._
import navigation.JourneyNavigator
import models._
import config.Refined.Max50
import pages.Page

object LandOrPropertyDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ LandOrPropertyDisposalPage(srn) => //41
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorpropertydisposal.routes.WhatYouWillNeedLandPropertyDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedLandPropertyDisposalPage(srn) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalAddressListController
        .onPageLoad(srn, page = 1)

    case page @ HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex, _) => //41c
      userAnswers.get(page) match {
        case None => controllers.routes.UnauthorisedController.onPageLoad()
        case Some(HowDisposed.Sold) =>
          controllers.nonsipp.landorpropertydisposal.routes.WhenWasPropertySoldController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)
        case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)
      }

    case LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex) => //41d
      controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.WhoPurchasedLandOrPropertyController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex) =>
      userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex)) match {

        case Some(IdentityType.Individual) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyIndividualBuyerNameController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerNameController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerNameController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

        case Some(IdentityType.Other) =>
          controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)
      }

    case CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerCrnController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.IndividualBuyerNinoNumberController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case page @ LandOrPropertyDisposalAddressListPage(srn, addressChoice, _) =>
      (
        for {
          indexes <- userAnswers
            .map(LandPropertyDisposalCompleted.all(srn, addressChoice))
            .keys
            .toList
            .traverse(_.toIntOption)
            .getOrRecoverJourney
          nextIndex <- findNextOpenIndex[Max50.Refined](indexes).getOrRecoverJourney
        } yield controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
          .onPageLoad(srn, addressChoice, nextIndex, NormalMode)
      ).merge

    case PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerUtrController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndex) =>
      if (userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, index, disposalIndex)).isEmpty ||
        userAnswers.get(DisposalIndependentValuationPage(srn, index, disposalIndex)).isEmpty ||
        userAnswers.get(LandOrPropertyStillHeldPage(srn, index, disposalIndex)).isEmpty) {
        controllers.nonsipp.landorpropertydisposal.routes.TotalProceedsSaleLandPropertyController
          .onPageLoad(srn, index, disposalIndex, NormalMode)
      } else {
        controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
          .onPageLoad(srn, index, disposalIndex, NormalMode)
      }

    case TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.DisposalIndependentValuationController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case DisposalIndependentValuationPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case LandPropertyDisposalCompletedPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController.onPageLoad(srn, 1)

    case LandOrPropertyDisposalListPage(srn, addDisposal @ true) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalAddressListController.onPageLoad(srn, 1)

    case LandOrPropertyDisposalListPage(srn, addDisposal @ false) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex) =>
      if (userAnswers.map(LandOrPropertyAddressLookupPages(srn)).isEmpty) {
        controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
          .onPageLoad(srn, 1)
      }

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {

        case page @ HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex, hasAnswerChanged) => //41c
          userAnswers.get(page) match {
            case None => controllers.routes.UnauthorisedController.onPageLoad()
            case _ if hasAnswerChanged =>
              controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
                .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)
            case Some(HowDisposed.Sold) =>
              controllers.nonsipp.landorpropertydisposal.routes.WhenWasPropertySoldController
                .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)
            case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
              controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
                .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)
          }

        case WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex) => //41d
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case DisposalIndependentValuationPage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex) =>
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex)) match {

            case Some(IdentityType.Individual) =>
              controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyIndividualBuyerNameController
                .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

            case Some(IdentityType.UKCompany) =>
              controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerNameController
                .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

            case Some(IdentityType.UKPartnership) =>
              controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerNameController
                .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

            case Some(IdentityType.Other) =>
              controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
                .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)
          }

        case CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, landOrPropertyIndex, disposalIndex, CheckMode)

        case LandOrPropertyDisposalAddressListPage(srn, addressChoice, disposalChoice) =>
          controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
            .onPageLoad(srn, addressChoice, disposalChoice, CheckMode)
      }
}
