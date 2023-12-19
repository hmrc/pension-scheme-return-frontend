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
import config.Refined.{Max300, Max5}
import models.SchemeId.Srn
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
      (
        for {
          map <- userAnswers.get(WhenWasTransferMadePages(srn, index)).getOrRecoverJourney
          indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
          _ <- navToCYAOnMaxTransfersOut(srn, index, indexes)
        } yield controllers.nonsipp.membertransferout.routes.ReportAnotherTransferOutController
          .onPageLoad(srn, index, transferIndex, NormalMode)
      ).merge

    case page @ ReportAnotherTransferOutPage(srn, index, _) =>
      if (userAnswers.get(page).contains(true)) {
        (
          for {
            map <- userAnswers.get(WhenWasTransferMadePages(srn, index)).getOrRecoverJourney
            indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
            nextIndex <- findNextOpenIndex[Max5.Refined](indexes).getOrRecoverJourney
          } yield controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
            .onPageLoad(srn, index, nextIndex, NormalMode)
        ).merge
      } else {
        controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
          .onPageLoad(srn, index, NormalMode)
      }

    case TransfersOutCYAPage(srn) =>
      controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
        .onPageLoad(srn, 1, NormalMode)

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

        case ReceivingSchemeNamePage(srn, index, _) =>
          controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
            .onPageLoad(srn, index, NormalMode)

        case ReceivingSchemeTypePage(srn, index, _) =>
          controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
            .onPageLoad(srn, index, NormalMode)

        case WhenWasTransferMadePage(srn, index, transferIndex) =>
          (
            for {
              map <- userAnswers.get(WhenWasTransferMadePages(srn, index)).getOrRecoverJourney
              indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
              _ <- navToCYAOnMaxTransfersOut(srn, index, indexes)
            } yield controllers.nonsipp.membertransferout.routes.ReportAnotherTransferOutController
              .onPageLoad(srn, index, transferIndex, NormalMode)
          ).merge

        case page @ ReportAnotherTransferOutPage(srn, index, _) =>
          if (userAnswers.get(page).contains(true)) {
            (
              for {
                map <- userAnswers.get(WhenWasTransferMadePages(srn, index)).getOrRecoverJourney
                indexes <- map.keys.toList.traverse(_.toIntOption).getOrRecoverJourney
                nextIndex <- findNextOpenIndex[Max5.Refined](indexes).getOrRecoverJourney
              } yield controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                .onPageLoad(srn, index, nextIndex, NormalMode)
            ).merge
          } else {
            controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
              .onPageLoad(srn, index, NormalMode)
          }

        case TransfersOutCYAPage(srn) =>
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoad(srn, 1, NormalMode)

      }

  private def navToCYAOnMaxTransfersOut(srn: Srn, index: Max300, indexes: List[Int]): Either[Call, Unit] =
    if (indexes.size == 5) {
      Left(
        controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
          .onPageLoad(srn, index, NormalMode)
      )
    } else {
      Right(())
    }
}
