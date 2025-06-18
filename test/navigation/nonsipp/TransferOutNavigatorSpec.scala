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
import navigation.{Navigator, NavigatorBehaviours}
import models.NormalMode
import pages.nonsipp.membertransferout._
import viewmodels.models.SectionJourneyStatus
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class TransferOutNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index: Max300 = 1
  private val secondaryIndex: Max5 = 1

  "TransferOutNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          SchemeTransferOutPage.apply,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.membertransferout.routes.WhatYouWillNeedTransferOutController.onPageLoad(srn)
        )
        .withName("go from did scheme transfer out page to What you Will need page")
    )

    act.like(
      normalmode
        .navigateToWithData(
          SchemeTransferOutPage.apply,
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
          WhatYouWillNeedTransferOutPage.apply,
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
          TransferOutMemberListPage.apply,
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
          ReceivingSchemeNamePage.apply,
          (srn, index: Int, secondaryIndex: Int, _) =>
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
          WhenWasTransferMadePage.apply,
          (srn, index: Int, secondaryIndex: Int, _) =>
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
        .navigateToWithDoubleIndexAndData(
          index,
          secondaryIndex,
          ReportAnotherTransferOutPage.apply,
          Gen.const(false),
          (srn, index: Int, _: Int, _) =>
            controllers.nonsipp.membertransferout.routes.TransfersOutCYAController.onPageLoad(srn, index, NormalMode)
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
          .unsafeSet(ReportAnotherTransferOutPage(srn, index, secondaryIndex), true)
          .unsafeSet(WhenWasTransferMadePages(srn, index), existingIndexes.map(_ -> localDate).toMap)
          .unsafeSet(
            MemberTransferOutProgress.all(srn, index),
            existingIndexes.map(_ -> SectionJourneyStatus.Completed).toMap
          )

      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            index,
            secondaryIndex,
            ReportAnotherTransferOutPage.apply,
            (srn, index: Int, _: Int, _) =>
              controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                .onPageLoad(srn, index, expectedRedirectIndex, NormalMode),
            userAnswers
          )
          .withName(
            s"go from report another transfer out  page to receiving scheme name page with index ${expectedRedirectIndex} when indexes $existingIndexes already exist"
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
          ReceivingSchemeNamePage.apply,
          (srn, index: Int, secondaryIndex: Int, _) =>
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
          WhenWasTransferMadePage.apply,
          (srn, index: Int, secondaryIndex: Int, _) =>
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
        .navigateToWithDoubleIndexAndData(
          index,
          secondaryIndex,
          ReportAnotherTransferOutPage.apply,
          Gen.const(false),
          (srn, index: Int, _: Int, _) =>
            controllers.nonsipp.membertransferout.routes.TransfersOutCYAController.onPageLoad(srn, index, NormalMode)
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
          .unsafeSet(ReportAnotherTransferOutPage(srn, index, secondaryIndex), true)
          .unsafeSet(WhenWasTransferMadePages(srn, index), existingIndexes.map(_ -> localDate).toMap)
          .unsafeSet(
            MemberTransferOutProgress.all(srn, index),
            existingIndexes.map(_ -> SectionJourneyStatus.Completed).toMap
          )

      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            index,
            secondaryIndex,
            ReportAnotherTransferOutPage.apply,
            (srn, index: Int, _: Int, _) =>
              controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                .onPageLoad(srn, index, expectedRedirectIndex, NormalMode),
            userAnswers
          )
          .withName(
            s"go from report another transfer out  page to receiving scheme name page with index ${expectedRedirectIndex} when indexes $existingIndexes already exist"
          )
      )
    }
  }

  "RemoveTransferOutPage" - {

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          RemoveTransferOutPage.apply,
          (srn, _: Int, _) =>
            controllers.nonsipp.membertransferout.routes.TransferOutMemberListController.onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from remove transfer out page to transfer out member list page")
    )
  }

}
