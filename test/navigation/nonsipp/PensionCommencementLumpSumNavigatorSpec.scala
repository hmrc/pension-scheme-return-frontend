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

import models.NormalMode
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.memberreceivedpcls.{
  PclsMemberListPage,
  PensionCommencementLumpSumPage,
  WhatYouWillNeedPensionCommencementLumpSumPage
}
import utils.BaseSpec

class PensionCommencementLumpSumNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

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
          PclsMemberListPage,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from pcls member list page page to task list page")
    )
  }
}
