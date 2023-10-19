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

import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.memberpayments.EmployerContributionsPage
import utils.BaseSpec

class EmployerContributionsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "EmployerContributionsNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          EmployerContributionsPage,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.employercontributions.routes.WhatYouWillNeedEmployerContributionsController
              .onPageLoad(srn)
        )
        .withName(
          "go from employer contribution page to what you will need employer contributions page when yes selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithData(
          EmployerContributionsPage,
          Gen.const(false),
          controllers.nonsipp.memberpayments.routes.UnallocatedEmployerContributionsController.onPageLoad
        )
        .withName("go from employer contribution page to unallocated employer contributions page when no selected")
    )

  }
}
