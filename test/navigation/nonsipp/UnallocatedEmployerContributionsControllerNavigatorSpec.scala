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

import config.Refined.{Max5000, OneTo5000}
import controllers.routes
import eu.timepit.refined.refineMV
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.memberpayments.UnallocatedEmployerContributionsPage
import utils.BaseSpec

class UnallocatedEmployerContributionsControllerNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  private val index = refineMV[OneTo5000](1)
  val navigator: Navigator = new NonSippNavigator

  "UnallocatedEmployerContributionsControllerNavigator" - {

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          (srn, _: Max5000) => UnallocatedEmployerContributionsPage(srn),
          Gen.const(true),
          controllers.nonsipp.memberpayments.routes.UnallocatedEmployerAmountController.onPageLoad
        )
        .withName("go from unallocated employer contributions page to Unallocated amount")
    )

    act.like(
      normalmode
        .navigateToWithData(
          UnallocatedEmployerContributionsPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from unallocated employer contributions page to task list page when no selected")
    )

  }
}
