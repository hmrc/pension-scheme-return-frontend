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

import utils.BaseSpec
import config.RefinedTypes.{Max300, Max5}
import models.SchemeId.Srn
import utils.IntUtils.given
import pages.nonsipp.receivetransfer._
import navigation.{Navigator, NavigatorBehaviours}
import models.NormalMode
import viewmodels.models.SectionJourneyStatus
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class ReceiveTransferNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index: Max300 = 1
  private val secondaryIndex: Max5 = 1

  "ReceiveTransferNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeReceiveTransferPage.apply,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.receivetransfer.routes.WhatYouWillNeedReceivedTransferController.onPageLoad(srn)
        )
        .withName("go from did scheme receive transfer page to unauthorised page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeReceiveTransferPage.apply,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from did scheme receive transfer page to scheme transfer out page when no selected")
    )
  }

  "WhatYouWillNeedReceivedTransferPage" - {

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedReceivedTransferPage.apply,
          (srn, _) =>
            controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
              .onPageLoad(srn, 1, NormalMode)
        )
    )
  }

  "TransferReceivedMemberListPage" - {

    act.like(
      normalmode
        .navigateTo(
          TransferReceivedMemberListPage.apply,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from transfer received member list page to task list page")
    )
  }

  "TransferringSchemeNamePage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          TransferringSchemeNamePage.apply,
          (srn, index: Int, secondaryIndex: Int, _) =>
            controllers.nonsipp.receivetransfer.routes.TransferringSchemeTypeController
              .onPageLoad(srn, index, secondaryIndex, NormalMode)
        )
        .withName("go from transferring scheme name page to total value transfer page")
    )
  }

  "TotalValueTransferPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          TotalValueTransferPage.apply,
          controllers.nonsipp.receivetransfer.routes.WhenWasTransferReceivedController.onPageLoad
        )
        .withName("go from total value transfer page to when was transfer received page")
    )

  }

  "ReportAnotherTransferInPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          secondaryIndex,
          ReportAnotherTransferInPage.apply,
          Gen.const(false),
          (srn, index: Int, _: Int, _) =>
            controllers.nonsipp.receivetransfer.routes.TransfersInCYAController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from report another transfer in page to CYA page")
    )

    List(
      (List("0"), 2),
      (List("0", "1", "2"), 4),
      (List("1", "2"), 1), // deleted first entry
      (List("0", "1", "3"), 3), // deleted one entry in the middle
      (List("0", "1", "2", "5", "6"), 4), // deleted two entry in the middle
      (List("0", "1", "3", "5", "6"), 3) // deleted entry in the middle of two sections
    ).foreach { case (existingIndexes, expectedRedirectIndex) =>
      def userAnswers(srn: Srn) =
        defaultUserAnswers
          .unsafeSet(ReportAnotherTransferInPage(srn, index, secondaryIndex), true)
          .unsafeSet(TotalValueTransferPages(srn, index), existingIndexes.map(_ -> money).toMap)
          .unsafeSet(
            ReceiveTransferProgress.all(index),
            existingIndexes.map(_ -> SectionJourneyStatus.Completed).toMap
          )

      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            index,
            secondaryIndex,
            ReportAnotherTransferInPage.apply,
            (srn, index: Int, _: Int, _) =>
              controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
                .onPageLoad(srn, index, expectedRedirectIndex, NormalMode),
            userAnswers
          )
          .withName(
            s"go from report another transfer in  page to transferring scheme name page with index ${expectedRedirectIndex} when indexes $existingIndexes already exist"
          )
      )
    }
  }

  "DidTransferIncludeAssetPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          DidTransferIncludeAssetPage.apply,
          (srn, index: Int, secondaryIndex: Int, _) =>
            controllers.nonsipp.receivetransfer.routes.ReportAnotherTransferInController
              .onPageLoad(srn, index, secondaryIndex, NormalMode),
          srn => defaultUserAnswers.unsafeSet(TotalValueTransferPage(srn, index, secondaryIndex), money)
        )
        .withName("go from DidTransferIncludeAssetPage to report another transfer in page")
    )
  }

  "RemoveTransferInPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          RemoveTransferInPage.apply,
          (srn, _: Int, _) =>
            controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from remove transfer in page to transfer received member list page")
    )
  }

  "TransfersInCYACompletedPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          TransfersInSectionCompleted.apply,
          (_, _: Int, _: Int, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from TransfersInCYACompletedPage to ??? page")
    )
  }
}
