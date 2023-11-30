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

import config.Refined.OneTo5
import controllers.nonsipp.memberpayments
import eu.timepit.refined.refineV
import models.{NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.receivetransfer._
import play.api.mvc.Call

object ReceiveTransferNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ DidSchemeReceiveTransferPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.receivetransfer.routes.WhatYouWillNeedReceivedTransferController.onPageLoad(srn)

      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedReceivedTransferPage(srn) =>
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController.onPageLoad(srn, 1, NormalMode)

    case TransferReceivedMemberListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case TransferringSchemeNamePage(srn, memberIndex, index) =>
      controllers.nonsipp.receivetransfer.routes.TotalValueTransferController
        .onPageLoad(srn, memberIndex, index, NormalMode)

    case TotalValueTransferPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.receivetransfer.routes.WhenWasTransferReceivedController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case WhenWasTransferReceivedPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.receivetransfer.routes.DidTransferIncludeAssetController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case DidTransferIncludeAssetPage(srn, index, secondaryIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

    case TransferReceivedMemberListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case page @ ReportAnotherTransferInPage(srn, index, secondaryIndex) =>
      if (userAnswers.get(page).contains(true)) {
        refineV[OneTo5](secondaryIndex.value + 1) match {
          case Left(_) => controllers.routes.UnauthorisedController.onPageLoad()
          case Right(nextIndex) =>
            controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
              .onPageLoad(srn, index, nextIndex, NormalMode)
        }
      } else {
        controllers.routes.UnauthorisedController.onPageLoad()
      }
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {
        case page @ DidSchemeReceiveTransferPage(srn) =>
          if (userAnswers.get(page).contains(true)) {
            controllers.routes.UnauthorisedController.onPageLoad()
          } else {
            memberpayments.routes.SchemeTransferOutController.onPageLoad(srn, NormalMode)
          }
      }
}
