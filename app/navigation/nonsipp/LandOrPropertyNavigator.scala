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
import pages.nonsipp.landorproperty._
import cats.implicits.toTraverseOps
import eu.timepit.refined.refineMV
import navigation.JourneyNavigator
import models._
import pages.nonsipp.common._

object LandOrPropertyNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ LandOrPropertyHeldPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorproperty.routes.WhatYouWillNeedLandOrPropertyController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedLandOrPropertyPage(srn) =>
      controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad(srn, refineMV(1), NormalMode)

    case page @ LandPropertyInUKPage(srn, index) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorproperty.routes.LandOrPropertyPostcodeLookupController
          .onPageLoad(srn, index, NormalMode)
      } else {
        controllers.nonsipp.landorproperty.routes.LandPropertyAddressManualController
          .onPageLoad(srn, index, isUkAddress = false, NormalMode)
      }

    case LandOrPropertyPostcodeLookupPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandPropertyAddressResultsController.onPageLoad(srn, index, NormalMode)

    case LandOrPropertyChosenAddressPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandRegistryTitleNumberController
        .onPageLoad(srn, index, NormalMode)

    case LandRegistryTitleNumberPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.WhyDoesSchemeHoldLandPropertyController
        .onPageLoad(srn, index, NormalMode)

    case page @ WhyDoesSchemeHoldLandPropertyPage(srn, index) =>
      userAnswers.get(page) match {
        case Some(SchemeHoldLandProperty.Transfer) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController.onPageLoad(srn, index, NormalMode)
        case _ =>
          controllers.nonsipp.landorproperty.routes.WhenDidSchemeAcquireController.onPageLoad(srn, index, NormalMode)
      }

    case LandOrPropertyWhenDidSchemeAcquirePage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, index)) match {
        case Some(SchemeHoldLandProperty.Contribution) =>
          controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController
            .onPageLoad(srn, index, NormalMode)
        case _ => //27h1
          controllers.nonsipp.common.routes.IdentityTypeController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.LandOrPropertySeller)
      }

    case page @ IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller) =>
      userAnswers.get(page) match {
        case Some(IdentityType.Individual) =>
          controllers.nonsipp.landorproperty.routes.LandPropertyIndividualSellersNameController
            .onPageLoad(srn, index, NormalMode)

        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.landorproperty.routes.CompanySellerNameController
            .onPageLoad(srn, index, NormalMode) //27h4

        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.landorproperty.routes.PartnershipSellerNameController
            .onPageLoad(srn, index, NormalMode) //27h6

        case Some(IdentityType.Other) =>
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.LandOrPropertySeller)

        case _ => controllers.routes.UnauthorisedController.onPageLoad()

      }

    case CompanySellerNamePage(srn, index) =>
      controllers.nonsipp.common.routes.CompanyRecipientCrnController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.LandOrPropertySeller)

    case CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case LandOrPropertyTotalCostPage(srn, index) =>
      if (userAnswers.get(LandOrPropertyTotalIncomePage(srn, index)).isEmpty ||
        (userAnswers.get(IsLandPropertyLeasedPage(srn, index)).getOrElse(false) && userAnswers
          .get(LandOrPropertyLeaseDetailsPage(srn, index))
          .isEmpty)) {
        controllers.nonsipp.landorproperty.routes.IsLandOrPropertyResidentialController
          .onPageLoad(srn, index, NormalMode)
      } else {
        controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
          .onPageLoad(srn, index, NormalMode)
      }

    case LandPropertyIndependentValuationPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController.onPageLoad(srn, index, NormalMode)

    case page @ IsLandOrPropertyResidentialPage(srn, index) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorproperty.routes.IsLandPropertyLeasedController.onPageLoad(srn, index, NormalMode)
      } else {
        controllers.nonsipp.landorproperty.routes.IsLandPropertyLeasedController.onPageLoad(srn, index, NormalMode)
      }

    case page @ IsLandPropertyLeasedPage(srn, index) => //27j2
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorproperty.routes.LandOrPropertyLeaseDetailsController
          .onPageLoad(srn, index, NormalMode)
      } else {
        //27j5
        controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController
          .onPageLoad(srn, index, NormalMode)
      }

    case LandOrPropertyLeaseDetailsPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.IsLesseeConnectedPartyController.onPageLoad(srn, index, NormalMode)

    case IsLesseeConnectedPartyPage(srn, index) => //27j4
      //27j5
      controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController
        .onPageLoad(srn, index, NormalMode)

    case PartnershipSellerNamePage(srn, index) => //27h6
      controllers.nonsipp.common.routes.PartnershipRecipientUtrController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.LandOrPropertySeller) //27h7

    case PartnershipRecipientUtrPage(srn, index, IdentitySubject.LandOrPropertySeller) => //27h7
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController //27i1
        .onPageLoad(srn, index, NormalMode)

    case LandPropertyIndividualSellersNamePage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.IndividualSellerNiController.onPageLoad(srn, index, NormalMode)

    case page @ IndividualSellerNiPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case LandOrPropertySellerConnectedPartyPage(srn, index) =>
      if (userAnswers.get(LandOrPropertyTotalIncomePage(srn, index)).isEmpty ||
        userAnswers.get(LandPropertyIndependentValuationPage(srn, index)).isEmpty ||
        userAnswers.get(LandOrPropertyTotalCostPage(srn, index)).isEmpty ||
        (userAnswers.get(IsLandPropertyLeasedPage(srn, index)).getOrElse(false) &&
        userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, index)).isEmpty)) {
        controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController
          .onPageLoad(srn, index, NormalMode)
      } else {
        controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
          .onPageLoad(srn, index, NormalMode)
      }

    case OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case LandOrPropertyTotalIncomePage(srn, index) => //27j5
      controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController.onPageLoad(srn, index, NormalMode)

    case LandOrPropertyCYAPage(srn) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, page = 1, NormalMode)

    case RemovePropertyPage(srn, _) =>
      if (userAnswers.map(LandOrPropertyAddressLookupPages(srn)).isEmpty) {
        controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, page = 1, NormalMode)
      }

    case LandOrPropertyListPage(srn, addLandOrProperty) =>
      if (addLandOrProperty) {
        (
          for {
            indexes <- userAnswers
              .map(LandOrPropertyCompleted.all(srn))
              .keys
              .toList
              .traverse(_.toIntOption)
              .getOrRecoverJourney
            nextIndex <- findNextOpenIndex[Max5000.Refined](indexes).getOrRecoverJourney
          } yield controllers.nonsipp.landorproperty.routes.LandPropertyInUKController
            .onPageLoad(srn, nextIndex, NormalMode)
        ).merge
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    oldUserAnswers =>
      userAnswers => {

        case page @ LandPropertyInUKPage(srn, index) =>
          (oldUserAnswers.get(page), userAnswers.get(page)) match {
            // chosen same answer
            case (Some(true), Some(true)) =>
              //check if address answer still exists
              userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)) match {
                case Some(_) =>
                  controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
                    .onPageLoad(srn, index, CheckMode)
                case None =>
                  controllers.nonsipp.landorproperty.routes.LandOrPropertyPostcodeLookupController
                    .onPageLoad(srn, index, CheckMode)
              }
            case (Some(false), Some(false)) =>
              //check if address answer still exists
              userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)) match {
                case Some(_) =>
                  controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
                    .onPageLoad(srn, index, CheckMode)
                case None =>
                  controllers.nonsipp.landorproperty.routes.LandPropertyAddressManualController
                    .onPageLoad(srn, index, isUkAddress = false, CheckMode)
              }
            case (_, Some(true)) =>
              controllers.nonsipp.landorproperty.routes.LandOrPropertyPostcodeLookupController
                .onPageLoad(srn, index, CheckMode)
            case (_, Some(false)) =>
              controllers.nonsipp.landorproperty.routes.LandPropertyAddressManualController
                .onPageLoad(srn, index, isUkAddress = false, CheckMode)
          }

        case LandOrPropertyPostcodeLookupPage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandPropertyAddressResultsController
            .onPageLoad(srn, index, CheckMode)

        case LandOrPropertyChosenAddressPage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case LandOrPropertyTotalIncomePage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case IsLesseeConnectedPartyPage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case LandOrPropertyLeaseDetailsPage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case page @ IsLandPropertyLeasedPage(srn, index) =>
          if (userAnswers.get(page).contains(true)) {
            controllers.nonsipp.landorproperty.routes.LandOrPropertyLeaseDetailsController
              .onPageLoad(srn, index, NormalMode)
          } else {
            controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController
              .onPageLoad(srn, index, NormalMode)
          }

        case page @ IsLandOrPropertyResidentialPage(srn, index) =>
          if (userAnswers.get(page).contains(true)) {
            controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
              .onPageLoad(srn, index, CheckMode)
          } else {
            controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
              .onPageLoad(srn, index, CheckMode)
          }

        case LandOrPropertyTotalCostPage(srn, index) =>
          if (userAnswers.get(LandOrPropertyTotalIncomePage(srn, index)).isEmpty ||
            (userAnswers.get(IsLandPropertyLeasedPage(srn, index)).getOrElse(false) &&
            userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, index)).isEmpty)) {
            controllers.nonsipp.landorproperty.routes.IsLandOrPropertyResidentialController
              .onPageLoad(srn, index, CheckMode)
          } else {
            controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
              .onPageLoad(srn, index, CheckMode)
          }

        case LandOrPropertySellerConnectedPartyPage(srn, index) =>
          if (userAnswers.get(LandOrPropertyTotalIncomePage(srn, index)).isEmpty ||
            userAnswers.get(LandPropertyIndependentValuationPage(srn, index)).isEmpty ||
            userAnswers.get(LandOrPropertyTotalCostPage(srn, index)).isEmpty ||
            (userAnswers.get(IsLandPropertyLeasedPage(srn, index)).getOrElse(false) &&
            userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, index)).isEmpty)) {
            controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController
              .onPageLoad(srn, index, NormalMode)
          } else {
            controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
              .onPageLoad(srn, index, CheckMode)
          }

        case LandPropertyIndependentValuationPage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case page @ WhyDoesSchemeHoldLandPropertyPage(srn, index) =>
          userAnswers.get(page) match {
            case Some(SchemeHoldLandProperty.Transfer) =>
              controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
                .onPageLoad(srn, index, CheckMode)
            case _ =>
              controllers.nonsipp.landorproperty.routes.WhenDidSchemeAcquireController
                .onPageLoad(srn, index, NormalMode)
          }

        case LandOrPropertyWhenDidSchemeAcquirePage(srn, index) =>
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, index)) match {
            case Some(SchemeHoldLandProperty.Contribution) =>
              controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
                .onPageLoad(srn, index, CheckMode)
            case _ => //27h1
              controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
                .onPageLoad(srn, index, CheckMode)
          }

        case page @ IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller) =>
          userAnswers.get(page) match {
            case Some(IdentityType.Individual) =>
              controllers.nonsipp.landorproperty.routes.LandPropertyIndividualSellersNameController
                .onPageLoad(srn, index, NormalMode)

            case Some(IdentityType.UKCompany) =>
              controllers.nonsipp.landorproperty.routes.CompanySellerNameController
                .onPageLoad(srn, index, NormalMode) //27h4

            case Some(IdentityType.UKPartnership) =>
              controllers.nonsipp.landorproperty.routes.PartnershipSellerNameController
                .onPageLoad(srn, index, NormalMode) //27h6

            case Some(IdentityType.Other) =>
              controllers.nonsipp.common.routes.OtherRecipientDetailsController
                .onPageLoad(srn, index, NormalMode, IdentitySubject.LandOrPropertySeller)

            case _ => controllers.routes.UnauthorisedController.onPageLoad()

          }

        case CompanySellerNamePage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case PartnershipSellerNamePage(srn, index) => //27h6
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode) //27h7

        case PartnershipRecipientUtrPage(srn, index, IdentitySubject.LandOrPropertySeller) => //27h7
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case LandPropertyIndividualSellersNamePage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case page @ IndividualSellerNiPage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)

        case LandRegistryTitleNumberPage(srn, index) =>
          controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController
            .onPageLoad(srn, index, CheckMode)
      }
}
