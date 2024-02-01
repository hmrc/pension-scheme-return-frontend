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

import controllers.nonsipp.shares.CompanyNameOfSharesSellerPage
import eu.timepit.refined.refineMV
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.{IdentitySubject, IdentityType, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.shares._
import play.api.mvc.Call

object SharesNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ DidSchemeHoldAnySharesPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.shares.routes.WhatYouWillNeedSharesController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedSharesPage(srn) =>
      controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, refineMV(1), NormalMode)

    case TypeOfSharesHeldPage(srn, index) =>
      controllers.nonsipp.shares.routes.WhyDoesSchemeHoldSharesController.onPageLoad(srn, index, NormalMode)

    case WhyDoesSchemeHoldSharesPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {
        case Some(Acquisition) =>
          controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode)
        case Some(Contribution) =>
          controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode)
        case Some(Transfer) =>
          controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode)
        case _ => controllers.routes.UnauthorisedController.onPageLoad()
      }

    case WhenDidSchemeAcquireSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode)

    case CompanyNameRelatedSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesCompanyCrnController.onPageLoad(srn, index, NormalMode)

    case SharesCompanyCrnPage(srn, index) =>
      controllers.nonsipp.shares.routes.ClassOfSharesController.onPageLoad(srn, index, NormalMode)

    case ClassOfSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.HowManySharesController.onPageLoad(srn, index, NormalMode)

    case HowManySharesPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {

        case Some(Acquisition) =>
          controllers.nonsipp.common.routes.IdentityTypeController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)
        case _ =>
          userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
            case Some(Unquoted) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)
            case _ =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
          }

      }

    case IdentityTypePage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.SharesSeller)) match {
        case Some(IdentityType.Other) =>
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)
        case Some(IdentityType.Individual) =>
          controllers.nonsipp.shares.routes.IndividualNameOfSharesSellerController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.shares.routes.CompanyNameOfSharesSellerController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.shares.routes.PartnershipNameOfSharesSellerController
            .onPageLoad(srn, index, NormalMode)
        case _ =>
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case IndividualNameOfSharesSellerPage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesIndividualSellerNINumberController.onPageLoad(srn, index, NormalMode)

    case CompanyNameOfSharesSellerPage(srn, index) =>
      controllers.nonsipp.common.routes.CompanyRecipientCrnController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)

    case PartnershipShareSellerNamePage(srn, index) =>
      controllers.nonsipp.common.routes.PartnershipRecipientUtrController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)

    case SharesIndividualSellerNINumberPage(srn, index) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)

      }

    case CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
      }

    case PartnershipRecipientUtrPage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
      }

    case OtherRecipientDetailsPage(srn, index, IdentitySubject.SharesSeller) =>
      userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
        case Some(Unquoted) =>
          controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad(srn, index, NormalMode)

        case _ =>
          controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
      }

    case SharesFromConnectedPartyPage(srn, index) =>
      controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)

    case CostOfSharesPage(srn, index) =>
      controllers.nonsipp.shares.routes.SharesIndependentValuationController.onPageLoad(srn, index, NormalMode)

    case SharesIndependentValuationPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) match {
        case Some(Acquisition) =>
          userAnswers.get(TypeOfSharesHeldPage(srn, index)) match {
            case Some(SponsoringEmployer) =>
              controllers.routes.UnauthorisedController.onPageLoad()
            case _ =>
              controllers.nonsipp.shares.routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode)
          }
        case _ =>
          controllers.nonsipp.shares.routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode)

      }

    case SharesTotalIncomePage(srn, index) =>
      controllers.routes.UnauthorisedController.onPageLoad()

  }

  val checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ => _ => PartialFunction.empty
}
