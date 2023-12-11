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
import models.{CheckOrChange, NormalMode}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.membercontributions.{
  MemberContributionsPage,
  TotalMemberContributionPage,
  WhatYouWillNeedMemberContributionsPage
}
import utils.BaseSpec

class MemberContributionsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

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
            controllers.nonsipp.membercontributions.routes.ReportMemberContributionListController
              .onPageLoad(srn, page = 1, NormalMode)
        )
        .withName("go from what you will need member contributions page to total member contribution")
    )
  }

  "TotalMemberContributionPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          TotalMemberContributionPage,
          (srn, index: Max300, secondaryIndex: Max50, _) =>
            controllers.nonsipp.membercontributions.routes.CYAMemberContributionsController
              .onPageLoad(srn, index, secondaryIndex, CheckOrChange.Check)
        )
        .withName("go from total member contribution to unauthorised page")
    )
  }

}
