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

import config.Refined.OneTo300
import controllers.nonsipp.memberdetails.routes
import eu.timepit.refined.refineV
import models.CheckOrChange.Check
import models.ManualOrUpload.{Manual, Upload}
import models._
import navigation.JourneyNavigator
import pages._
import pages.nonsipp.BasicDetailsCheckYourAnswersPage
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberdetails.upload.{FileUploadErrorPage, FileUploadSuccessPage}
import play.api.mvc.Call

object MemberDetailsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case PensionSchemeMembersPage(srn) =>
      if (userAnswers.get(PensionSchemeMembersPage(srn)).contains(ManualOrUpload.Manual)) {
        routeMemberDetailsFirstManualPage(userAnswers, srn)
      } else {
        controllers.nonsipp.memberdetails.routes.HowToUploadController.onPageLoad(srn)
      }

    case MemberDetailsPage(srn, index) => routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, NormalMode)

    case page @ DoesMemberHaveNinoPage(srn, index) =>
      routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckOrChange.Check)

    case MemberDetailsNinoPage(srn, index) =>
      routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckOrChange.Check)

    case SchemeMemberDetailsAnswersPage(srn) => routes.SchemeMembersListController.onPageLoad(srn, page = 1, Manual)

    case NoNINOPage(srn, index) =>
      routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckOrChange.Check)

    case SchemeMembersListPage(srn, false, _) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case SchemeMembersListPage(srn, true, Manual) =>
      refineV[OneTo300](userAnswers.membersDetails(srn).length + 1).fold(
        _ => controllers.nonsipp.routes.TaskListController.onPageLoad(srn),
        index => routes.PensionSchemeMembersController.onPageLoad(srn)
      )

    case SchemeMembersListPage(srn, true, Upload) => routes.PensionSchemeMembersController.onPageLoad(srn)

    case RemoveMemberDetailsPage(srn) => routes.SchemeMembersListController.onPageLoad(srn, page = 1, Manual)

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
        controllers.nonsipp.memberdetails.upload.routes.FileUploadErrorController.onPageLoad(srn, NormalMode)
      }

    case FileUploadSuccessPage(srn) =>
      controllers.nonsipp.memberdetails.routes.SchemeMembersListController
        .onPageLoad(srn, 1, Manual)

    case FileUploadErrorPage(srn, UploadFormatError) =>
      controllers.nonsipp.memberdetails.upload.routes.FileUploadErrorMissingInformationController
        .onPageLoad(srn, NormalMode)

    case FileUploadErrorPage(srn, UploadErrors(errs)) if errs.size > 10 =>
      controllers.nonsipp.memberdetails.upload.routes.FileUploadTooManyErrorsController.onPageLoad(srn, NormalMode)

    case FileUploadErrorPage(srn, _: UploadErrors) =>
      controllers.nonsipp.memberdetails.upload.routes.FileUploadErrorSummaryController.onPageLoad(srn, NormalMode)

    case FileUploadErrorPage(srn, UploadMaxRowsError) =>
      controllers.nonsipp.memberdetails.upload.routes.FileUploadTooManyRowsController.onPageLoad(srn, NormalMode)

    case FileUploadErrorMissingInformationPage(srn) =>
      controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)

    case FileUploadErrorSummaryPage(srn) =>
      controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)

    case FileUploadTooManyErrorsPage(srn) =>
      controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)

    case FileUploadTooManyRowsPage(srn) =>
      controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)

    case BasicDetailsCheckYourAnswersPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
  }

  private def routeMemberDetailsFirstManualPage(userAnswers: UserAnswers, srn: SchemeId.Srn) =
    refineV[OneTo300](userAnswers.membersDetails(srn).length + 1) match {
      case Left(_) => routes.SchemeMembersListController.onPageLoad(srn, page = 1, Manual)
      case Right(nextIndex) => routes.MemberDetailsController.onPageLoad(srn, nextIndex, NormalMode)
    }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case MemberDetailsPage(srn, index) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Check)

    case DoesMemberHaveNinoPage(srn, index) =>
      routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Check)
//      userAnswers.get(page) match {
//
//        case Some(_) =>
//          routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, Check)
//        case None =>
//          routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, CheckMode)
//      }

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
