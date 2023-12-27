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

import config.Refined.Max300
import eu.timepit.refined.refineMV
import models.NormalMode
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.membercontributions._
import utils.BaseSpec

class MemberContributionsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)

  "MemberContributionsNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          MemberContributionsPage,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.membercontributions.routes.WhatYouWillNeedMemberContributionsController.onPageLoad(srn)
        )
        .withName("go from member contribution page to what you will need member contributions page when yes select ")
    )

    act.like(
      normalmode
        .navigateToWithData(
          MemberContributionsPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from member contributions page to receive transfer page")
    )
  }

  "WhatYouWillNeedMemberContributions" - {

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedMemberContributionsPage,
          (srn, _) =>
            controllers.nonsipp.membercontributions.routes.MemberContributionListController
              .onPageLoad(srn, page = 1, NormalMode)
        )
        .withName("go from what you will need member contributions page to total member contribution")
    )
  }

  "MemberContributionsListPage" - {
    act.like(
      normalmode
        .navigateTo(
          MemberContributionsListPage,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from member contribution list page to task list page")
    )
  }

  "TotalMemberContributionPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          TotalMemberContributionPage,
          (srn, index: Max300, _) =>
            controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from total member contribution to CYAMemberContributions page")
    )
  }

  "RemoveMemberContributionPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          RemoveMemberContributionPage,
          (srn, _: Max300, _) =>
            controllers.nonsipp.membercontributions.routes.MemberContributionListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from remove page to Member Contribution List page")
    )
  }

  "MemberContributionsCYAPage" - {
    act.like(
      normalmode
        .navigateTo(
          MemberContributionsCYAPage,
          (srn, _) =>
            controllers.nonsipp.membercontributions.routes.MemberContributionListController
              .onPageLoad(srn, page = 1, NormalMode)
        )
        .withName("go from MemberContributionsCYAPage to Member Contribution List page")
    )
  }

}
