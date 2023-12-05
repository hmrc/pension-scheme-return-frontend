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

import models.{CheckOrChange, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.membercontributions.{
  RemoveMemberContributionPage,
  TotalMemberContributionPage,
  WhatYouWillNeedMemberContributionsPage
}
import pages.nonsipp.memberpayments.{MemberContributionsPage, ReportMemberContributionListPage}
import play.api.mvc.Call

object MemberContributionsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ MemberContributionsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.membercontributions.routes.WhatYouWillNeedMemberContributionsController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case WhatYouWillNeedMemberContributionsPage(srn) =>
      controllers.nonsipp.membercontributions.routes.ReportMemberContributionListController
        .onPageLoad(srn, page = 1, NormalMode)

    case ReportMemberContributionListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case TotalMemberContributionPage(srn, index, secondaryIndex) =>
      controllers.nonsipp.membercontributions.routes.CYAMemberContributionsController
        .onPageLoad(srn, index, secondaryIndex, CheckOrChange.Check)

    case RemoveMemberContributionPage(srn, memberIndex, index) =>
      controllers.nonsipp.membercontributions.routes.ReportMemberContributionListController
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

        case TotalMemberContributionPage(srn, index, secondaryIndex) =>
          controllers.nonsipp.membercontributions.routes.CYAMemberContributionsController
            .onPageLoad(srn, index, secondaryIndex, CheckOrChange.Check)
      }
}
