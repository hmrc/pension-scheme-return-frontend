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
import pages.nonsipp.membersurrenderedbenefits._
import utils.BaseSpec

class SurrenderedBenefitsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val memberIndex = refineMV[Max300.Refined](1)

  "SurrenderedBenefitsNavigator" - {

    "SurrenderedBenefitsPage" - {

      act.like(
        normalmode
          .navigateToWithData(
            SurrenderedBenefitsPage,
            Gen.const(true),
            (srn, _) =>
              controllers.nonsipp.membersurrenderedbenefits.routes.WhatYouWillNeedSurrenderedBenefitsController
                .onPageLoad(srn)
          )
          .withName("go from Surrendered Benefits page to What You Will Need page when yes selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            SurrenderedBenefitsPage,
            Gen.const(false),
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from Surrendered Benefits page to Task List page when no selected")
      )

    }

    "WhatYouWillNeedSurrenderedBenefitsPage" - {

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedSurrenderedBenefitsPage,
            (srn, _) =>
              controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
                .onPageLoad(srn, 1, NormalMode)
          )
          .withName("go from What You Will Need page to Surrendered Benefits Member List page")
      )
    }

    "SurrenderedBenefitsMemberListPage" - {

      act.like(
        normalmode
          .navigateTo(
            SurrenderedBenefitsMemberListPage,
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from Surrendered Benefits Member List page to Task List page")
      )
    }

    "SurrenderedBenefitsAmountPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            memberIndex,
            SurrenderedBenefitsAmountPage,
            (srn, memberIndex: Max300, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from Surrendered Benefits Amount page to When Surrendered Benefits page")
      )

    }
  }
}
