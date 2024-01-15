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
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import models.{NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
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
      controllers.routes.UnauthorisedController.onPageLoad()
  }

  val checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ => _ => PartialFunction.empty
}
