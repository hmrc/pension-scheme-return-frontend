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

import play.api.mvc.Call
import pages.Page
import navigation.JourneyNavigator
import models.{CheckOrChange, NormalMode, UserAnswers}
import pages.nonsipp.memberpayments._

object UnallocatedEmployerContributionsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ UnallocatedEmployerContributionsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.memberpayments.routes.UnallocatedEmployerAmountController
          .onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case UnallocatedEmployerAmountPage(srn) =>
      controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
        .onPageLoad(srn, CheckOrChange.Check)

    case UnallocatedContributionCYAPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case RemoveUnallocatedAmountPage(srn) =>
      if (userAnswers.get(UnallocatedEmployerContributionsPage(srn)).isEmpty) {
        controllers.nonsipp.memberpayments.routes.UnallocatedEmployerContributionsController
          .onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
          .onPageLoad(srn, CheckOrChange.Check)
      }
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      _ => {

        case UnallocatedEmployerAmountPage(srn) =>
          controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
            .onPageLoad(srn, CheckOrChange.Check)

      }
}
