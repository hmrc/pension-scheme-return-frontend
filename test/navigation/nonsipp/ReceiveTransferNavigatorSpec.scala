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

import config.Refined.{Max300, Max50}
import eu.timepit.refined.refineMV
import models.NormalMode
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.receivetransfer.{
  DidSchemeReceiveTransferPage,
  ReportAnotherTransferInPage,
  TotalValueTransferPage,
  TransferReceivedMemberListPage,
  TransferringSchemeNamePage,
  WhatYouWillNeedReceivedTransferPage
}
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class ReceiveTransferNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

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
          (srn, index: Max300, secondaryIndex: Max50, _) =>
            controllers.nonsipp.receivetransfer.routes.TotalValueTransferController
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
          (srn, index: Max300, secondaryIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from total value transfer page to unauthorised page")
    )

  }

  "ReportAnotherTransferInPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleDataAndIndex(
          index,
          secondaryIndex,
          ReportAnotherTransferInPage,
          Gen.const(false),
          (srn, index: Max300, secondaryIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from report another transfer in page to unauthorised page")
    )

  }

}
