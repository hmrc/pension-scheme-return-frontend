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

import models.{NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.PensionPaymentsReceivedPage
import play.api.mvc.Call

object PensionPaymentsReceivedNavigator extends JourneyNavigator {

  val normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ PensionPaymentsReceivedPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.routes.UnauthorisedController.onPageLoad()
      } else {
        controllers.nonsipp.benefitssurrendered.routes.BenefitsSurrenderedController.onPageLoad(srn, NormalMode)
      }
  }

  val checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
}
