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

import models.{CheckMode, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.memberreceivedpcls._
import play.api.mvc.Call

object PensionCommencementLumpSumNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ PensionCommencementLumpSumPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.memberreceivedpcls.routes.WhatYouWillNeedPensionCommencementLumpSumController
          .onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case PensionCommencementLumpSumAmountPage(srn, index, NormalMode) =>
      controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
        .onPageLoad(srn, index, NormalMode)

    case WhatYouWillNeedPensionCommencementLumpSumPage(srn) =>
      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onPageLoad(srn, 1, NormalMode)

    case PclsMemberListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case PclsCYAPage(srn, _) =>
      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onPageLoad(srn, 1, NormalMode)

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      _ => {
        case PensionCommencementLumpSumAmountPage(srn, memberIndex, CheckMode) =>
          controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
            .onPageLoad(srn, memberIndex, NormalMode)

        case PclsCYAPage(srn, _) =>
          controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
            .onPageLoad(srn, 1, NormalMode)
      }
}
