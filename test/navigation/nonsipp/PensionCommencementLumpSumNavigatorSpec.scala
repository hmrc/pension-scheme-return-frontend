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
import pages.nonsipp.memberreceivedpcls._
import config.RefinedTypes.OneTo300
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import org.scalacheck.Gen
import navigation.{Navigator, NavigatorBehaviours}
import models.NormalMode

class PensionCommencementLumpSumNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo300](1)

  "PensionCommencementLumpSumNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          PensionCommencementLumpSumPage,
          Gen.const(true),
          (s, _) =>
            controllers.nonsipp.memberreceivedpcls.routes.WhatYouWillNeedPensionCommencementLumpSumController
              .onPageLoad(s)
        )
        .withName(
          "go from pension commencement lump sum page to what you will need pension commencement page when yes selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithData(
          PensionCommencementLumpSumPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from pension commencement lump sum page to task list page when no selected")
    )

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedPensionCommencementLumpSumPage,
          (srn, _) =>
            controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from what you will need pension commencement page to member list page when continue is selected")
    )

    act.like(
      normalmode
        .navigateTo(
          PclsMemberListPage.apply,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from pcls member list page page to task list page")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          (srn, index) => PensionCommencementLumpSumAmountPage(srn, index),
          (srn, index, _) =>
            controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from pcls amount page to CYA page")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          (srn, index) => PclsCYAPage(srn, index),
          (srn, _: Int, _) =>
            controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from pcls CYA page to members list page")
    )

  }

  "PensionCommencementLumpSumNavigator in check mode" - {

    act.like(
      checkmode
        .navigateTo(
          srn => PensionCommencementLumpSumAmountPage(srn, index),
          (srn, _) => controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from pcls amount page in check mode to CYA page in normal mode")
    )

    act.like(
      checkmode
        .navigateTo(
          srn => PclsCYAPage(srn, index),
          (srn, _) =>
            controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from pcls CYA page in check mode to members list page in normal mode")
    )
  }
}
