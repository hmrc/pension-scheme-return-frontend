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

import config.Refined.{Max300, Max5}
import eu.timepit.refined.refineMV
import models.NormalMode
import models.SchemeId.Srn
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.receivetransfer._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class ReceiveTransferNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)

  "ReceiveTransferNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeReceiveTransferPage,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.receivetransfer.routes.WhatYouWillNeedReceivedTransferController.onPageLoad(srn)
        )
        .withName("go from did scheme receive transfer page to unauthorised page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeReceiveTransferPage,
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
          WhatYouWillNeedReceivedTransferPage,
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
          TransferReceivedMemberListPage,
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
          TransferringSchemeNamePage,
          (srn, index: Max300, secondaryIndex: Max5, _) =>
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
          TotalValueTransferPage,
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
          ReportAnotherTransferInPage,
          Gen.const(false),
          (srn, index: Max300, _: Max5, _) =>
            controllers.nonsipp.receivetransfer.routes.TransfersInCYAController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from report another transfer in page to CYA page")
    )

    List(
      (List("0"), refineMV[Max5.Refined](2)),
      (List("0", "1", "2"), refineMV[Max5.Refined](4)),
      (List("1", "2"), refineMV[Max5.Refined](1)), // deleted first entry
      (List("0", "1", "3"), refineMV[Max5.Refined](3)), // deleted one entry in the middle
      (List("0", "1", "2", "5", "6"), refineMV[Max5.Refined](4)), // deleted two entry in the middle
      (List("0", "1", "3", "5", "6"), refineMV[Max5.Refined](3)) // deleted entry in the middle of two sections
    ).foreach {
      case (existingIndexes, expectedRedirectIndex) =>
        def userAnswers(srn: Srn) =
          defaultUserAnswers
            .unsafeSet(ReportAnotherTransferInPage(srn, index, secondaryIndex), true)
            .unsafeSet(TotalValueTransferPages(srn, index), existingIndexes.map(_ -> money).toMap)

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              index,
              secondaryIndex,
              ReportAnotherTransferInPage,
              (srn, index: Max300, _: Max5, _) =>
                controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
                  .onPageLoad(srn, index, expectedRedirectIndex, NormalMode),
              userAnswers
            )
            .withName(
              s"go from report another transfer in  page to transferring scheme name page with index ${expectedRedirectIndex.value} when indexes $existingIndexes already exist"
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
          DidTransferIncludeAssetPage,
          (srn, index: Max300, secondaryIndex: Max5, _) =>
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
          RemoveTransferInPage,
          (srn, _: Max300, _) =>
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
          TransfersInSectionCompleted,
          (srn, index: Max300, secondaryIndex: Max5, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from TransfersInCYACompletedPage to ??? page")
    )
  }
}
