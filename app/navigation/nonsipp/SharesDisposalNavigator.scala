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
      if (userAnswers
          .map(HowWereSharesDisposedPages(srn))
          .filter(_._2.nonEmpty)
          .isEmpty) {
        controllers.nonsipp.sharesdisposal.routes.SharesDisposalController.onPageLoad(srn, NormalMode)
      } else {
        // TODO when you have reported [number] share disposals page is done
        controllers.routes.UnauthorisedController.onPageLoad()
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
          controllers.routes.UnauthorisedController.onPageLoad()
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

    case HowManySharesPage(srn, shareIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      _ => {
        PartialFunction.empty
      }
}
