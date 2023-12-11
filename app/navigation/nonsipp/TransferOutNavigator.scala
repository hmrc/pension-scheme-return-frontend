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
import pages.nonsipp.membertransferout._
import play.api.mvc.Call

object TransferOutNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ SchemeTransferOutPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.membertransferout.routes.WhatYouWillNeedTransferOutController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedTransferOutPage(srn) =>
      controllers.nonsipp.membertransferout.routes.TransferOutMemberListController.onPageLoad(srn, 1, NormalMode)

    case TransferOutMemberListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case ReceivingSchemeNamePage(srn, index, transferIndex) =>
      controllers.nonsipp.membertransferout.routes.ReceivingSchemeTypeController
        .onPageLoad(srn, index, transferIndex, NormalMode)

    case ReceivingSchemeTypePage(srn, index, transferIndex) =>
      controllers.nonsipp.membertransferout.routes.WhenWasTransferMadeController
        .onPageLoad(srn, index, transferIndex, NormalMode)

    case WhenWasTransferMadePage(srn, index, transferIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {
        case page @ SchemeTransferOutPage(srn) =>
          if (userAnswers.get(page).contains(true)) {
            controllers.routes.UnauthorisedController.onPageLoad()
          } else {
            controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumController
              .onPageLoad(srn, NormalMode)
          }
      }
}
