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
import models.{NormalMode, PensionSchemeType}
import models.PensionSchemeType.PensionSchemeType
import models.SchemeId.Srn
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.membertransferout.{ReceivingSchemeTypePages, ReportAnotherTransferOutPage, SchemeTransferOutPage, TransferOutMemberListPage, WhatYouWillNeedTransferOutPage}
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

}
