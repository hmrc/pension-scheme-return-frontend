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

import utils.BaseSpec
import navigation.{Navigator, NavigatorBehaviours}
import models.CheckOrChange
import pages.nonsipp.memberpayments._
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class UnallocatedEmployerContributionsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "UnallocatedEmployerContributionsNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          UnallocatedEmployerContributionsPage,
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

  "UnallocatedEmployerAmountPage" - {
    act.like(
      normalmode
        .navigateToWithData(
          UnallocatedEmployerAmountPage,
          Gen.const(money),
          (srn, _) =>
            controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
              .onPageLoad(srn, CheckOrChange.Check)
        )
        .withName("go from Unallocated amount page to CYA and Remove page ")
    )
  }

  "UnallocatedContributionCYAPage" - {
    act.like(
      normalmode
        .navigateTo(
          srn => UnallocatedContributionCYAPage(srn),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from remove page to Money Borrowed page")
    )
  }

  "RemoveUnallocatedAmountsPage" - {
    act.like(
      normalmode
        .navigateToWithData(
          RemoveUnallocatedAmountPage,
          Gen.const(false),
          (srn, _) =>
            controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
              .onPageLoad(srn, CheckOrChange.Check),
          srn =>
            defaultUserAnswers
              .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
              .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
        )
        .withName("go from remove page to CYA")
    )
  }
}
