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

import pages.nonsipp.membercontributions._
import play.api.mvc.Call
import pages.Page
import utils.IntUtils.toInt
import navigation.JourneyNavigator
import models.{NormalMode, UserAnswers}

object MemberContributionsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ MemberContributionsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.membercontributions.routes.MemberContributionListController
          .onPageLoad(srn, page = 1, NormalMode)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case MemberContributionsListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case TotalMemberContributionPage(srn, index) =>
      controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
        .onPageLoad(srn, index, NormalMode)

    case RemoveMemberContributionPage(srn, _) =>
      controllers.nonsipp.membercontributions.routes.MemberContributionListController
        .onPageLoad(srn, page = 1, NormalMode)

    case MemberContributionsCYAPage(srn) =>
      controllers.nonsipp.membercontributions.routes.MemberContributionListController
        .onPageLoad(srn, page = 1, NormalMode)

  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {
        case page @ MemberContributionsPage(srn) =>
          if (userAnswers.get(page).contains(true)) {
            controllers.routes.UnauthorisedController.onPageLoad()
          } else {
            controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController.onPageLoad(srn, NormalMode)
          }

        case TotalMemberContributionPage(srn, index) =>
          controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
            .onPageLoad(srn, index, NormalMode)

        case MemberContributionsCYAPage(srn) =>
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoad(srn, page = 1, NormalMode)
      }
}
