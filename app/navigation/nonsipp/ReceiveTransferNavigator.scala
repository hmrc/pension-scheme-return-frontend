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

import play.api.mvc.Call
import models.SchemeId.Srn
import navigation.JourneyNavigator
import models.{CheckMode, NormalMode, UserAnswers}
import config.Refined.{Max300, Max5}
import pages.Page
import cats.implicits.toTraverseOps
import pages.nonsipp.receivetransfer._

object ReceiveTransferNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case TransfersInSectionCompleted(srn, index, secondaryIndex) =>
      controllers.routes.UnauthorisedController.onPageLoad()

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
      controllers.nonsipp.receivetransfer.routes.TransferringSchemeTypeController
        .onPageLoad(srn, memberIndex, index, NormalMode)

    case TransferringSchemeTypePage(srn, memberIndex, index) =>
      controllers.nonsipp.receivetransfer.routes.TotalValueTransferController
        .onPageLoad(srn, memberIndex, index, NormalMode)

    case TotalValueTransferPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.receivetransfer.routes.WhenWasTransferReceivedController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case WhenWasTransferReceivedPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.receivetransfer.routes.DidTransferIncludeAssetController
        .onPageLoad(srn, index, secondaryIndex, NormalMode)

    case DidTransferIncludeAssetPage(srn, index, secondaryIndex) =>
      (
        for {
          map <- userAnswers.get(TotalValueTransferPages(srn, index)).getOrRecoverJourney
          indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
          _ <- navToCYAOnMaxTransfersIn(srn, index, indexes)
        } yield controllers.nonsipp.receivetransfer.routes.ReportAnotherTransferInController
          .onPageLoad(srn, index, secondaryIndex, NormalMode)
      ).merge

    case TransferReceivedMemberListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case page @ ReportAnotherTransferInPage(srn, index, _) =>
      if (userAnswers.get(page).contains(true)) {
        (
          for {
            map <- userAnswers.get(TotalValueTransferPages(srn, index)).getOrRecoverJourney
            indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
            nextIndex <- findNextOpenIndex[Max5.Refined](indexes).getOrRecoverJourney
          } yield controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
            .onPageLoad(srn, index, nextIndex, NormalMode)
        ).merge
      } else {
        controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
          .onPageLoad(srn, index, NormalMode)
      }

    case RemoveTransferInPage(srn, memberIndex) =>
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onPageLoad(srn, 1, NormalMode)

    case TransfersInCYAPage(srn) =>
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onPageLoad(srn, 1, NormalMode)
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {
        case TransferringSchemeNamePage(srn, memberIndex, index) =>
          controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
            .onPageLoad(srn, memberIndex, NormalMode)

        case page @ DidSchemeReceiveTransferPage(srn) =>
          if (userAnswers.get(page).contains(true)) {
            controllers.routes.UnauthorisedController.onPageLoad()
          } else {
            controllers.nonsipp.membertransferout.routes.SchemeTransferOutController.onPageLoad(srn, NormalMode)
          }

        case TransferringSchemeNamePage(srn, index, _) =>
          controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
            .onPageLoad(srn, index, NormalMode)

        case TransferringSchemeTypePage(srn, index, _) =>
          controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
            .onPageLoad(srn, index, NormalMode)

        case TotalValueTransferPage(srn, index, secondaryIndex) =>
          controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
            .onPageLoad(srn, index, NormalMode)

        case WhenWasTransferReceivedPage(srn, index, secondaryIndex) =>
          controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
            .onPageLoad(srn, index, NormalMode)

        case DidTransferIncludeAssetPage(srn, index, secondaryIndex) =>
          (
            for {
              map <- userAnswers.get(TotalValueTransferPages(srn, index)).getOrRecoverJourney
              indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
              _ <- navToCYAOnMaxTransfersIn(srn, index, indexes)
            } yield controllers.nonsipp.receivetransfer.routes.ReportAnotherTransferInController
              .onPageLoad(srn, index, secondaryIndex, CheckMode)
          ).merge

        case page @ ReportAnotherTransferInPage(srn, index, _) =>
          if (userAnswers.get(page).contains(true)) {
            (
              for {
                map <- userAnswers.get(TotalValueTransferPages(srn, index)).getOrRecoverJourney
                indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
                nextIndex <- findNextOpenIndex[Max5.Refined](indexes).getOrRecoverJourney
              } yield controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
                .onPageLoad(srn, index, nextIndex, NormalMode)
            ).merge
          } else {
            controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
              .onPageLoad(srn, index, NormalMode)
          }

        case TransfersInCYAPage(srn) =>
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoad(srn, 1, NormalMode)
      }

  private def navToCYAOnMaxTransfersIn(srn: Srn, index: Max300, indexes: List[Int]): Either[Call, Unit] =
    if (indexes.size == 5) {
      Left(
        controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
          .onPageLoad(srn, index, NormalMode)
      )
    } else {
      Right(())
    }
}
