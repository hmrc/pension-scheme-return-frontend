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
import models.{HowDisposed, IdentityType, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.landorpropertydisposal._
import play.api.mvc.Call

object LandOrPropertyDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ LandOrPropertyDisposalPage(srn) => //41
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.landorpropertydisposal.routes.WhatYouWillNeedLandPropertyDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedLandPropertyDisposalPage(srn) =>
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController.onPageLoad(srn, page = 1)

    case page @ HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex) => //41c
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
      controllers.routes.UnauthorisedController.onPageLoad()

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
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerCrnController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.IndividualBuyerNinoNumberController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case LandOrPropertyDisposalListPage(srn, choice) =>
      controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
        .onPageLoad(srn, choice, refineMV(1), NormalMode)

    case PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex) =>
      controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerUtrController
        .onPageLoad(srn, landOrPropertyIndex, disposalIndex, NormalMode)

    case PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex) => //TODO Navigation. Subsequent page still need to be implemented
      controllers.routes.UnauthorisedController.onPageLoad()
  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
}
