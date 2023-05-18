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
import models.CheckOrChange.Check
import models.{CheckMode, CheckOrChange, ManualOrUpload, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.{CheckingMemberDetailsFilePage, Page}
import pages.nonsipp.memberdetails.MembersDetails.MembersDetailsOps
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberdetails.upload.FileUploadSuccessPage
import play.api.mvc.Call

object MemberDetailsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case PensionSchemeMembersPage(srn) =>
      if (userAnswers.get(PensionSchemeMembersPage(srn)).contains(ManualOrUpload.Manual)) {
        routes.MemberDetailsController.onPageLoad(srn, refineMV(1), NormalMode)
      } else {
        controllers.nonsipp.memberdetails.routes.HowToUploadController.onPageLoad(srn)
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
        index => routes.MemberDetailsController.onPageLoad(srn, index, NormalMode)
      )

    case RemoveMemberDetailsPage(srn) => routes.SchemeMembersListController.onPageLoad(srn, page = 1)

    case HowToUploadPage(srn) =>
      controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)

    case UploadMemberDetailsPage(srn) => routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode)

    case page @ CheckMemberDetailsFilePage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.memberdetails.upload.routes.CheckingMemberDetailsFileController.onPageLoad(srn, NormalMode)
      } else {
        routes.UploadMemberDetailsController.onPageLoad(srn)
      }

    case CheckingMemberDetailsFilePage(srn, uploadSuccessful) =>
      if (uploadSuccessful) {
        controllers.nonsipp.memberdetails.upload.routes.FileUploadSuccessController.onPageLoad(srn, NormalMode)
      } else {
        controllers.routes.UnauthorisedController.onPageLoad()
      }

    case FileUploadSuccessPage(srn) =>
      controllers.nonsipp.memberdetails.routes.SchemeMembersListController.onPageLoad(srn, 1)
  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case MemberDetailsPage(srn, index) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Check)
    case page @ DoesMemberHaveNinoPage(srn, index) =>
      userAnswers.get(page) match {
        case Some(true) if userAnswers.get(MemberDetailsNinoPage(srn, index)).isEmpty =>
          routes.MemberDetailsNinoController.onPageLoad(srn, index, CheckMode)
        case Some(false) if userAnswers.get(NoNINOPage(srn, index)).isEmpty =>
          routes.NoNINOController.onPageLoad(srn, index, CheckMode)
        case Some(_) =>
          routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Check)
        case None =>
          routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, CheckMode)
      }
    case MemberDetailsNinoPage(srn, index) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Check)
    case NoNINOPage(srn, index) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Check)
    case UploadMemberDetailsPage(srn) => routes.CheckMemberDetailsFileController.onPageLoad(srn, CheckMode)
    case page @ CheckMemberDetailsFilePage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.routes.UnauthorisedController.onPageLoad()
      } else {
        routes.UploadMemberDetailsController.onPageLoad(srn)
      }
  }
}
