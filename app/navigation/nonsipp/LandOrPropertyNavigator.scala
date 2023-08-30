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
import models.{NormalMode, SchemeHoldLandProperty, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
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
          controllers.nonsipp.landorproperty.routes.PropertyAcquiredFromController
            .onPageLoad(srn, index, NormalMode)
      }

    //27h1
    case page @ PropertyAcquiredFromPage(srn, index) =>
      //TODO change to 27h6 and all the different pages
      controllers.routes.UnauthorisedController.onPageLoad()

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

    case page @ IsLandPropertyLeasedPage(srn, index) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorproperty.routes.LandOrPropertyLeaseDetailsController
          .onPageLoad(srn, index, NormalMode)
      } else {
        controllers.routes.UnauthorisedController.onPageLoad()
      }

    case LandOrPropertyLeaseDetailsPage(srn, index) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case CompanySellerNamePage(srn, index) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case page @ IndividualSellerNiPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case LandOrPropertySellerConnectedPartyPage(srn, index) =>
      controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController
        .onPageLoad(srn, index, NormalMode)

  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
}
