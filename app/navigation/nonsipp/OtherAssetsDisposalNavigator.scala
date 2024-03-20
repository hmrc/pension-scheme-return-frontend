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

import eu.timepit.refined.refineMV
import navigation.JourneyNavigator
import pages.Page
import play.api.mvc.Call
import models._
import pages.nonsipp.otherassetsdisposal._

object OtherAssetsDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ OtherAssetsDisposalPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.otherassetsdisposal.routes.WhatYouWillNeedOtherAssetsDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedOtherAssetsDisposalPage(srn) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case page @ HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex, _) =>
      userAnswers.get(page) match {
        case None => controllers.routes.JourneyRecoveryController.onPageLoad()
        case Some(HowDisposed.Sold) =>
          controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
            .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
        case Some(HowDisposed.Transferred) | Some(HowDisposed.Other(_)) =>
          controllers.routes.UnauthorisedController.onPageLoad()
      }

    case WhenWasAssetSoldPage(srn, assetIndex, disposalIndex) =>
      controllers.nonsipp.otherassetsdisposal.routes.TotalConsiderationSaleAssetController
        .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)

    case TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ => _ => PartialFunction.empty

}
