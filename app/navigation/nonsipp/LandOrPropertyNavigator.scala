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
import models.{CheckOrChange, IdentitySubject, IdentityType, NormalMode, SchemeHoldLandProperty, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage, OtherRecipientDetailsPage, PartnershipRecipientUtrPage}
import pages.nonsipp.landorproperty._
import play.api.mvc.Call

object LandOrPropertyNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ LandOrPropertyHeldPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorproperty.routes.WhatYouWillNeedLandOrPropertyController.onPageLoad(srn)
      } else {
        controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, NormalMode)
      }

    case WhatYouWillNeedLandOrPropertyPage(srn) =>
      controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad(srn, refineMV(1), NormalMode)

    case LandPropertyInUKPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertyAddressLookupController.onPageLoad(srn, index)

    case LandOrPropertyAddressLookupPage(srn, index) =>
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

        case _ => controllers.routes.UnauthorisedController.onPageLoad() //TODO 27h8

      }

    case CompanySellerNamePage(srn, index) =>
      controllers.nonsipp.common.routes.CompanyRecipientCrnController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.LandOrPropertySeller)

    case CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case LandOrPropertyTotalCostPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.IsLandOrPropertyResidentialController.onPageLoad(srn, index, NormalMode)

    case LandPropertyIndependentValuationPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController.onPageLoad(srn, index, NormalMode)

    case page @ IsLandOrPropertyResidentialPage(srn, index) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorproperty.routes.IsLandPropertyLeasedController.onPageLoad(srn, index, NormalMode)
      } else {
        controllers.routes.UnauthorisedController.onPageLoad()
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

    case LandOrPropertySellerConnectedPartyPage(srn, index) => //27i1
      controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController
        .onPageLoad(srn, index, NormalMode)

    case OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case LandOrPropertyTotalIncomePage(srn, index) => //27j5
      controllers.nonsipp.landorproperty.routes.LandOrPropertyCYAController.onPageLoad(srn, index, CheckOrChange.Check)

    case LandOrPropertyCYAPage(srn) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController.onPageLoad(srn, page = 1, NormalMode)

    case LandOrPropertyListPage(srn, addLandOrProperty) => //27j7
      if (addLandOrProperty) {
        val count = userAnswers.map(LandOrPropertyAddressLookupPages(srn)).size
        refineV[Max5000.Refined](count + 1).fold(
          err => controllers.routes.JourneyRecoveryController.onPageLoad(),
          nextIndex =>
            controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad(srn, nextIndex, NormalMode)
        )
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }
  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
}
