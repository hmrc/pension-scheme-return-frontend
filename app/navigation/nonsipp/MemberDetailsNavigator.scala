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

import config.Refined.OneTo99
import controllers.nonsipp.employercontributions
import controllers.nonsipp.memberdetails.routes
import eu.timepit.refined.{refineMV, refineV}
import models.{CheckOrChange, ManualOrUpload, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.memberdetails.MembersDetails.MembersDetailsOps
import pages.nonsipp.memberdetails.{
  DoesMemberHaveNinoPage,
  MemberDetailsNinoPage,
  MemberDetailsPage,
  NoNINOPage,
  PensionSchemeMembersPage,
  RemoveMemberDetailsPage,
  SchemeMemberDetailsAnswersPage,
  SchemeMembersListPage
}
import play.api.mvc.Call

object MemberDetailsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case PensionSchemeMembersPage(srn) =>
      if (userAnswers.get(PensionSchemeMembersPage(srn)).contains(ManualOrUpload.Manual)) {
        routes.MemberDetailsController.onPageLoad(srn, refineMV(1))
      } else {
        controllers.routes.UnauthorisedController.onPageLoad()
      }

    case MemberDetailsPage(srn, index) => routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, NormalMode)

    case page @ DoesMemberHaveNinoPage(srn, index) =>
      if (userAnswers.get(page).contains(true)) {
        routes.MemberDetailsNinoController.onPageLoad(srn, index, NormalMode)
      } else {
        routes.NoNINOController.onPageLoad(srn, index, NormalMode)
      }

    case MemberDetailsNinoPage(srn, index) =>
      routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckOrChange.Check)

    case SchemeMemberDetailsAnswersPage(srn) => routes.SchemeMembersListController.onPageLoad(srn, page = 1)

    case NoNINOPage(srn, index) =>
      routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckOrChange.Check)

    case SchemeMembersListPage(srn, false) =>
      employercontributions.routes.EmployerContributionsController.onPageLoad(srn, NormalMode)

    case SchemeMembersListPage(srn, true) =>
      refineV[OneTo99](userAnswers.membersDetails(srn).length + 1).fold(
        _ => employercontributions.routes.EmployerContributionsController.onPageLoad(srn, NormalMode),
        index => routes.MemberDetailsController.onPageLoad(srn, index)
      )

    case RemoveMemberDetailsPage(srn) => routes.SchemeMembersListController.onPageLoad(srn, page = 1)
  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => {
    case NoNINOPage(srn, _) => controllers.routes.UnauthorisedController.onPageLoad()
  }
}
