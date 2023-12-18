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
import pages.nonsipp.membertransferout._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class TransferOutNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)

  "TransferOutNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          SchemeTransferOutPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.membertransferout.routes.WhatYouWillNeedTransferOutController.onPageLoad(srn)
        )
        .withName("go from did scheme transfer out page to What you Will need page")
    )

    act.like(
      normalmode
        .navigateToWithData(
          SchemeTransferOutPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from did scheme transfer out page to pension commencement lump sum page when no selected")
    )
  }

  "WhatYouWillNeedTransferOutPage" - {

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedTransferOutPage,
          (srn, _) =>
            controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from what you will need transfer out to transfer out member list page")
    )
  }

  "TransferOutMemberListPage" - {

    act.like(
      normalmode
        .navigateTo(
          TransferOutMemberListPage,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from transfer out member list page to task list page")
    )
  }

  "ReceivingSchemeNamePage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          ReceivingSchemeNamePage,
          (srn, index: Max300, secondaryIndex: Max5, _) =>
            controllers.nonsipp.membertransferout.routes.ReceivingSchemeTypeController
              .onPageLoad(srn, index, secondaryIndex, NormalMode)
        )
        .withName("go from receiving scheme name page to receiving scheme type page")
    )
  }

  "WhenWasTransferMadePage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          WhenWasTransferMadePage,
          (srn, index: Max300, secondaryIndex: Max5, _) =>
            controllers.nonsipp.membertransferout.routes.ReportAnotherTransferOutController
              .onPageLoad(srn, index, secondaryIndex, NormalMode),
          srn => defaultUserAnswers.unsafeSet(WhenWasTransferMadePage(srn, index, secondaryIndex), localDate)
        )
        .withName("go from WhenWasTransferMadePage to report another transfer out page")
    )
  }

  "ReportAnotherTransferOutPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleDataAndIndex(
          index,
          secondaryIndex,
          ReportAnotherTransferOutPage,
          Gen.const(false),
          (srn, index: Max300, _: Max5, _) =>
            controllers.nonsipp.membertransferout.routes.TransfersOutCYAController.onPageLoad(srn, index, NormalMode)
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
            .unsafeSet(ReportAnotherTransferOutPage(srn, index, secondaryIndex), true)
            .unsafeSet(WhenWasTransferMadePages(srn, index), existingIndexes.map(_ -> localDate).toMap)

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              index,
              secondaryIndex,
              ReportAnotherTransferOutPage,
              (srn, index: Max300, _: Max5, _) =>
                controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                  .onPageLoad(srn, index, expectedRedirectIndex, NormalMode),
              userAnswers
            )
            .withName(
              s"go from report another transfer out  page to receiving scheme name page with index ${expectedRedirectIndex.value} when indexes $existingIndexes already exist"
            )
        )
    }
  }

  "ReceivingSchemeNamePage in check mode" - {

    act.like(
      checkmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          ReceivingSchemeNamePage,
          (srn, index: Max300, secondaryIndex: Max5, _) =>
            controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from receiving scheme name page to receiving scheme type page")
    )
  }

  "WhenWasTransferMadePage in check mode" - {

    act.like(
      checkmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          WhenWasTransferMadePage,
          (srn, index: Max300, secondaryIndex: Max5, _) =>
            controllers.nonsipp.membertransferout.routes.ReportAnotherTransferOutController
              .onPageLoad(srn, index, secondaryIndex, NormalMode),
          srn => defaultUserAnswers.unsafeSet(WhenWasTransferMadePage(srn, index, secondaryIndex), localDate)
        )
        .withName("go from WhenWasTransferMadePage to report another transfer out page")
    )
  }

  "ReportAnotherTransferOutPage in check mode" - {

    act.like(
      normalmode
        .navigateToWithDoubleDataAndIndex(
          index,
          secondaryIndex,
          ReportAnotherTransferOutPage,
          Gen.const(false),
          (srn, index: Max300, _: Max5, _) =>
            controllers.nonsipp.membertransferout.routes.TransfersOutCYAController.onPageLoad(srn, index, NormalMode)
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
            .unsafeSet(ReportAnotherTransferOutPage(srn, index, secondaryIndex), true)
            .unsafeSet(WhenWasTransferMadePages(srn, index), existingIndexes.map(_ -> localDate).toMap)

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              index,
              secondaryIndex,
              ReportAnotherTransferOutPage,
              (srn, index: Max300, _: Max5, _) =>
                controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                  .onPageLoad(srn, index, expectedRedirectIndex, NormalMode),
              userAnswers
            )
            .withName(
              s"go from report another transfer out  page to receiving scheme name page with index ${expectedRedirectIndex.value} when indexes $existingIndexes already exist"
            )
        )
    }
  }

}
