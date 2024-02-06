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

import models.{HowSharesDisposed, IdentityType, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.sharesdisposal._
import play.api.mvc.Call

object SharesDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case CompanyBuyerNamePage(srn, index, secondaryIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case WhoWereTheSharesSoldToPage(srn, index, disposalIndex) =>
      userAnswers.get(WhoWereTheSharesSoldToPage(srn, index, disposalIndex)) match {

        case Some(IdentityType.Individual) =>
          controllers.nonsipp.sharesdisposal.routes.SharesIndividualBuyerNameController
            .onPageLoad(srn, index, disposalIndex, NormalMode)

        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.sharesdisposal.routes.CompanyNameOfSharesBuyerController
            .onPageLoad(srn, index, disposalIndex, NormalMode)

// TODO uncomment as these controllers are introduced:

//        case Some(IdentityType.UKCompany) =>
//          controllers.nonsipp.sharesdisposal.routes.CompanyBuyerNameController
//            .onPageLoad(srn, index, disposalIndex, NormalMode)
//
//        case Some(IdentityType.UKPartnership) =>
//          controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerNameController
//            .onPageLoad(srn, index, disposalIndex, NormalMode)
//
//        case Some(IdentityType.Other) =>
//          controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
//            .onPageLoad(srn, index, disposalIndex, NormalMode)
      }

    case SharesIndividualBuyerNamePage(srn, index, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
        .onPageLoad(srn, index, disposalIndex, NormalMode)

    // TODO uncomment as these controllers are introduced:
//    case CompanyBuyerNamePage(srn, index, disposalIndex) =>
//      controllers.nonsipp.sharesdisposal.routes.CompanyBuyerCrnController
//        .onPageLoad(srn, index, disposalIndex, NormalMode)
//
//    case PartnershipBuyerNamePage(srn, index, disposalIndex) =>
//      controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerUtrController
//        .onPageLoad(srn, index, disposalIndex, NormalMode)
//
//    case IndividualBuyerNinoNumberPage(srn, index, disposalIndex) =>
//      controllers.nonsipp.sharesdisposal.routes.ShareslBuyerConnectedPartyController
//        .onPageLoad(srn, index, disposalIndex, NormalMode)
//
//    case CompanyBuyerCrnPage(srn, index, disposalIndex) =>
//      controllers.nonsipp.sharesdisposal.routes.SharesBuyerConnectedPartyController
//        .onPageLoad(srn, index, disposalIndex, NormalMode)
//
//    case PartnershipBuyerUtrPage(srn, index, disposalIndex) =>
//      controllers.nonsipp.sharesdisposal.routes.SharesDisposalBuyerConnectedPartyController
//        .onPageLoad(srn, index, disposalIndex, NormalMode)
//
//    case OtherBuyerDetailsPage(srn, index, disposalIndex) =>
//      controllers.nonsipp.sharesdisposal.routes.SharesBuyerConnectedPartyController
//        .onPageLoad(srn, index, disposalIndex, NormalMode)

    case page @ SharesDisposalPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.sharesdisposal.routes.WhatYouWillNeedSharesDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedSharesDisposalPage(srn) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case page @ HowWereSharesDisposedPage(srn, shareIndex, disposalIndex, _) =>
      userAnswers.get(page) match {
        case None =>
          controllers.routes.UnauthorisedController.onPageLoad()
        case Some(HowSharesDisposed.Sold) =>
          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
        case Some(HowSharesDisposed.Redeemed) =>
          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
        case Some(HowSharesDisposed.Transferred) | Some(HowSharesDisposed.Other(_)) =>
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case WhenWereSharesSoldPage(srn, shareIndex, disposalIndex) =>
      controllers.nonsipp.sharesdisposal.routes.HowManySharesSoldController
        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case HowManySharesSoldPage(srn, shareIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      _ => {
        PartialFunction.empty
      }
}
