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

import models.{HowSharesDisposed, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.sharesdisposal._
import play.api.mvc.Call

object SharesDisposalNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ SharesDisposalPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.sharesdisposal.routes.WhatYouWillNeedSharesDisposalController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedSharesDisposalPage(srn) =>
      controllers.routes.UnauthorisedController.onPageLoad()
//      controllers.nonsipp.sharesdisposal.routes.SharesDisposalListController
//        .onPageLoad(srn, page = 1)

//    case SharesDisposalListPage(srn, shareIndex, disposalIndex) =>
//      controllers.nonsipp.sharesdisposal.routes.HowWereSharesDisposedController
//        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

    case page @ HowWereSharesDisposedPage(srn, shareIndex, disposalIndex, _) =>
      userAnswers.get(page) match {
        case None =>
          controllers.routes.UnauthorisedController.onPageLoad()
        case Some(HowSharesDisposed.Sold) =>
          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController
            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
        case Some(HowSharesDisposed.Redeemed) =>
          controllers.routes.UnauthorisedController.onPageLoad()
//          controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController
//            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
        case Some(HowSharesDisposed.Transferred) | Some(HowSharesDisposed.Other(_)) =>
          controllers.routes.UnauthorisedController.onPageLoad()
//          controllers.nonsipp.sharesdisposal.routes.HowManySharesHeldController
//            .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
      }

    case WhenWereSharesSoldPage(srn, shareIndex, disposalIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()
//      controllers.nonsipp.sharesdisposal.routes.HowManySharesSoldController
//        .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      _ => {
        PartialFunction.empty
      }
}
