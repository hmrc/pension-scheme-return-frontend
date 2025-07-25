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

import models.ManualOrUpload.{Manual, Upload}
import pages.{CheckingMemberDetailsFilePage, FileUploadErrorSummaryPage, FileUploadTooManyErrorsPage}
import models.SchemeId.Srn
import navigation.{Navigator, NavigatorBehaviours}
import pages.nonsipp.memberdetails.upload.{FileUploadErrorPage, FileUploadSuccessPage}
import models._
import utils.BaseSpec
import pages.nonsipp.memberdetails._
import config.RefinedTypes.OneTo300
import controllers.nonsipp.memberdetails.routes
import generators.IndexGen
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class MemberDetailsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "MemberDetailsNavigator" - {

    "NormalMode" - {

      act.like(
        normalmode
          .navigateToWithData(
            PensionSchemeMembersPage.apply,
            Gen.const(ManualOrUpload.Manual),
            routes.MemberDetailsController.onPageLoad(_, 1, _)
          )
          .withName("go from manual or upload to details page when manual is chosen")
      )

      act.like(
        normalmode
          .navigateToWithData(
            PensionSchemeMembersPage.apply,
            Gen.const(ManualOrUpload.Upload),
            (srn, _) => controllers.nonsipp.memberdetails.routes.HowToUploadController.onPageLoad(srn)
          )
          .withName("go from manual or upload to how to upload page when upload is chosen")
      )

      act.like(
        normalmode
          .navigateTo(
            MemberDetailsPage(_, 1),
            routes.DoesSchemeMemberHaveNINOController.onPageLoad(_, 1, _)
          )
          .withName("go from details to have a nino page")
      )

      act.like(
        normalmode
          .navigateTo(
            MemberDetailsPage(_, 1),
            routes.DoesSchemeMemberHaveNINOController.onPageLoad(_, 1, _)
          )
          .withName("go from member details page to does member have nino page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            DoesMemberHaveNinoPage(_, 1),
            Gen.const(true),
            routes.MemberDetailsNinoController.onPageLoad(_, 1, _)
          )
          .withName("go from does member have nino Page to member details nino page when yes selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            DoesMemberHaveNinoPage(_, 1),
            Gen.const(false),
            routes.NoNINOController.onPageLoad(_, 1, _)
          )
          .withName("go from does member have nino Page to no nino page when no selected")
      )

      act.like(
        normalmode
          .navigateTo(
            MemberDetailsNinoPage(_, 1),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, 1, NormalMode)
          )
          .withName("go from nino page to check answers page")
      )

      act.like(
        normalmode
          .navigateTo(
            NoNINOPage(_, 1),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, 1, NormalMode)
          )
          .withName("go from no nino page to scheme member details answers page")
      )

      act.like(
        normalmode
          .navigateTo(
            SchemeMemberDetailsAnswersPage.apply,
            (srn, _) => routes.SchemeMembersListController.onPageLoad(srn, page = 1, Manual)
          )
          .withName("go from scheme member details answers page to scheme members list Page")
      )

      act.like(
        normalmode
          .navigateFromListPage(
            SchemeMembersListPage(_, addMember = true, Manual),
            MemberDetailsPage.apply,
            nameDobGen,
            IndexGen[OneTo300](min = 1, max = 300),
            (srn, _, _) => routes.PensionSchemeMembersController.onPageLoad(srn),
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName(
            "go from scheme members list page to select manual or upload page when yes selected during the manual journey"
          )
      )

      act.like(
        normalmode
          .navigateFromListPage(
            SchemeMembersListPage(_, addMember = true, Upload),
            MemberDetailsPage.apply,
            nameDobGen,
            IndexGen[OneTo300](min = 1, max = 300),
            (srn, _, _) => routes.PensionSchemeMembersController.onPageLoad(srn),
            (srn, _) => routes.PensionSchemeMembersController.onPageLoad(srn)
          )
          .withName(
            "go from scheme members list page to select manual or upload when yes selected during the upload journey"
          )
      )

      act.like(
        normalmode
          .navigateTo(
            SchemeMembersListPage(_, addMember = false, Manual),
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName(
            "go from scheme members list page to tasklist page when no selected during the manual journey"
          )
      )

      act.like(
        normalmode
          .navigateTo(
            SchemeMembersListPage(_, addMember = false, Upload),
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName(
            "go from scheme members list page to tasklist page when no selected during the upload journey"
          )
      )

      act.like(
        normalmode
          .navigateTo(
            RemoveMemberDetailsPage.apply,
            (srn, _) => routes.SchemeMembersListController.onPageLoad(srn, page = 1, Manual)
          )
          .withName("go from remove page to list page")
      )

      act.like(
        normalmode
          .navigateTo(
            HowToUploadPage.apply,
            (srn, _) => controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn, false)
          )
          .withName("go from how to upload page  to upload member details page")
      )

      act.like(
        normalmode
          .navigateTo(
            UploadMemberDetailsPage.apply,
            routes.CheckMemberDetailsFileController.onPageLoad
          )
          .withName("go from upload member details page to check member details file page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckMemberDetailsFilePage.apply,
            Gen.const(true),
            controllers.nonsipp.memberdetails.upload.routes.CheckingMemberDetailsFileController.onPageLoad
          )
          .withName("go from check member details file page to checking member details file page")
      )

      act.like(
        normalmode
          .navigateTo(
            CheckingMemberDetailsFilePage(_, uploadSuccessful = false),
            controllers.nonsipp.memberdetails.upload.routes.FileUploadErrorController.onPageLoad
          )
          .withName("go from checking member details file page to file upload error page when upload failed")
      )

      act.like(
        normalmode
          .navigateTo(
            CheckingMemberDetailsFilePage(_, uploadSuccessful = true),
            controllers.nonsipp.memberdetails.upload.routes.FileUploadSuccessController.onPageLoad
          )
          .withName("go from checking member details file page to upload successful page when upload successful")
      )

      act.like(
        normalmode
          .navigateTo(
            FileUploadSuccessPage.apply,
            (srn, _) => controllers.nonsipp.memberdetails.routes.SchemeMembersListController.onPageLoad(srn, 1, Manual)
          )
          .withName("go from file upload success page to scheme members list page")
      )
      act.like(
        normalmode
          .navigateTo(
            FileUploadErrorPage(_, UploadFormatError),
            controllers.nonsipp.memberdetails.upload.routes.FileUploadErrorMissingInformationController.onPageLoad
          )
          .withName(
            "go from file upload error page to file upload error missing information page on file upload format error"
          )
      )

      act.like(
        normalmode
          .navigateTo(
            FileUploadErrorPage(_, uploadResultErrors),
            controllers.nonsipp.memberdetails.upload.routes.FileUploadErrorSummaryController.onPageLoad
          )
          .withName("go from file upload error page to file upload error summary page on file upload errors")
      )

      act.like(
        normalmode
          .navigateTo(
            FileUploadErrorPage(_, over10UploadResultErrors),
            controllers.nonsipp.memberdetails.upload.routes.FileUploadTooManyErrorsController.onPageLoad
          )
          .withName("go from file upload error page to too manu file upload errors page on file upload errors over 10")
      )

      act.like(
        normalmode
          .navigateTo(
            FileUploadErrorSummaryPage.apply,
            (srn, _) => controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn, false)
          )
          .withName("go from file upload error summary page to upload a file page")
      )

      act.like(
        normalmode
          .navigateTo(
            FileUploadTooManyErrorsPage.apply,
            (srn, _) => controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn, false)
          )
          .withName("go from too many file upload errors page to upload a file page")
      )
    }

    "CheckMode" - {

      act.like(
        checkmode
          .navigateTo(
            MemberDetailsPage(_, 1),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, 1, CheckMode)
          )
          .withName("go from member details page to check answers page")
      )

      act.like(
        checkmode
          .navigateTo(
            MemberDetailsNinoPage(_, 1),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, 1, CheckMode)
          )
          .withName("go from member details nino page to check your answers page")
      )

      act.like(
        checkmode
          .navigateTo(
            NoNINOPage(_, 1),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, 1, CheckMode)
          )
          .withName("go from no nino page to check your answers page")
      )

      "does member have nino page should go to" - {

        val userAnswersWithNino =
          (srn: Srn) => defaultUserAnswers.unsafeSet(MemberDetailsNinoPage(srn, 1), nino)
        val userAnswersWithNoNinoReason =
          (srn: Srn) => defaultUserAnswers.unsafeSet(NoNINOPage(srn, 1), noninoReason)

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, 1),
              Gen.const(true),
              routes.MemberDetailsNinoController.onPageLoad(_, 1, _)
            )
            .withName("nino page when yes selected and no data")
        )

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, 1),
              Gen.const(false),
              routes.NoNINOController.onPageLoad(_, 1, _)
            )
            .withName("no nino page when no selected and no data")
        )

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, 1),
              Gen.const(true),
              (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, 1, CheckMode),
              userAnswersWithNino
            )
            .withName("check answers page when yes selected and nino exists")
        )

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, 1),
              Gen.const(false),
              (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, 1, CheckMode),
              userAnswersWithNoNinoReason
            )
            .withName("check answers page when no selected and no nino reason exists")
        )

        act.like(
          checkmode
            .navigateTo(
              UploadMemberDetailsPage.apply,
              routes.CheckMemberDetailsFileController.onPageLoad
            )
            .withName("go from upload member details page to check member details file page")
        )
      }
    }
  }

}
