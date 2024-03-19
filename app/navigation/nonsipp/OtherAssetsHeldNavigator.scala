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

import cats.implicits.toTraverseOps
import config.Refined.Max5000
import eu.timepit.refined.refineMV
import models.SchemeHoldAsset.{Acquisition, Contribution, Transfer}
import models.{IdentitySubject, IdentityType, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.otherassetsheld._
import play.api.mvc.Call

object OtherAssetsHeldNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ OtherAssetsHeldPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.otherassetsheld.routes.WhatYouWillNeedOtherAssetsController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedOtherAssetsPage(srn) =>
      controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController.onPageLoad(srn, refineMV(1), NormalMode)

    case WhatIsOtherAssetPage(srn, index) =>
      controllers.nonsipp.otherassetsheld.routes.IsAssetTangibleMoveablePropertyController
        .onPageLoad(srn, index, NormalMode)

    case IsAssetTangibleMoveablePropertyPage(srn, index) =>
      controllers.nonsipp.otherassetsheld.routes.WhyDoesSchemeHoldAssetsController
        .onPageLoad(srn, index, NormalMode)

    case WhyDoesSchemeHoldAssetsPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index)) match {
        case Some(Acquisition) =>
          controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
            .onPageLoad(srn, index, NormalMode)
        case Some(Contribution) =>
          controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
            .onPageLoad(srn, index, NormalMode)
        case Some(Transfer) =>
          controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
            .onPageLoad(srn, index, NormalMode)
        case _ =>
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case WhenDidSchemeAcquireAssetsPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index)) match {
        case Some(Acquisition) =>
          controllers.nonsipp.common.routes.IdentityTypeController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.OtherAssetSeller)
        case _ =>
          controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
            .onPageLoad(srn, index, NormalMode)
      }

    case IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller) =>
      userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller)) match {
        case Some(IdentityType.Other) =>
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, NormalMode, IdentitySubject.OtherAssetSeller)
        case Some(IdentityType.Individual) =>
          controllers.nonsipp.otherassetsheld.routes.IndividualNameOfOtherAssetSellerController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.otherassetsheld.routes.CompanyNameOfOtherAssetSellerController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.otherassetsheld.routes.PartnershipNameOfOtherAssetsSellerController
            .onPageLoad(srn, index, NormalMode)
        case _ =>
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case IndividualNameOfOtherAssetSellerPage(srn, index) =>
      controllers.nonsipp.otherassetsheld.routes.OtherAssetIndividualSellerNINumberController
        .onPageLoad(srn, index, NormalMode)

    case CompanyNameOfOtherAssetSellerPage(srn, index) =>
      controllers.nonsipp.common.routes.CompanyRecipientCrnController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.OtherAssetSeller)

    case PartnershipOtherAssetSellerNamePage(srn, index) =>
      controllers.nonsipp.common.routes.PartnershipRecipientUtrController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.OtherAssetSeller)

    case OtherAssetIndividualSellerNINumberPage(srn, index) =>
      controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case CompanyRecipientCrnPage(srn, index, IdentitySubject.OtherAssetSeller) =>
      controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case PartnershipRecipientUtrPage(srn, index, IdentitySubject.OtherAssetSeller) =>
      controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case OtherRecipientDetailsPage(srn, index, IdentitySubject.OtherAssetSeller) =>
      controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case OtherAssetSellerConnectedPartyPage(srn, index) =>
      controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
        .onPageLoad(srn, index, NormalMode)

    case CostOfOtherAssetPage(srn, index) =>
      userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index)) match {
        case Some(Transfer) =>
          controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
            .onPageLoad(srn, index, NormalMode)
        case _ =>
          controllers.nonsipp.otherassetsheld.routes.IndependentValuationController
            .onPageLoad(srn, index, NormalMode)
      }

    case IndependentValuationPage(srn, index) =>
      controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
        .onPageLoad(srn, index, NormalMode)

    case IncomeFromAssetPage(srn, index) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    // CYA PAGE NAV HERE

    case page @ OtherAssetsListPage(srn) =>
      userAnswers.get(page) match {
        case None => controllers.routes.JourneyRecoveryController.onPageLoad()
        case Some(true) =>
          (
            for {
              map <- userAnswers.get(OtherAssetsCompleted.all(srn)).getOrRecoverJourney
              indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
              _ <- if (indexes.size >= 5000) Left(controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
              else Right(())
              nextIndex <- findNextOpenIndex[Max5000.Refined](indexes).getOrRecoverJourney
            } yield controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
              .onPageLoad(srn, nextIndex, NormalMode)
          ).merge
        case Some(false) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case RemoveOtherAssetPage(srn, index) =>
      if (userAnswers
          .map(WhatIsOtherAssetPages(srn))
          .isEmpty) {
        controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
          .onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, 1, NormalMode)
      }
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ => _ => PartialFunction.empty
}
