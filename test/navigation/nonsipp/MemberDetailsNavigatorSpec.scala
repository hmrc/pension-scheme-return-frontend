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

import config.Refined.{Max300, OneTo300}
import controllers.nonsipp.memberdetails.routes
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineMV
import generators.IndexGen
import models.ManualOrUpload.{Manual, Upload}
import models.SchemeId.Srn
import models.{CheckOrChange, ConditionalYesNo, ManualOrUpload, NormalMode, UploadFormatError}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberdetails.upload.{FileUploadErrorPage, FileUploadSuccessPage}
import pages.{CheckingMemberDetailsFilePage, FileUploadErrorSummaryPage, FileUploadTooManyErrorsPage}
import uk.gov.hmrc.domain.Nino
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class MemberDetailsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  private val index = refineMV[Max300.Refined](1)

  val navigator: Navigator = new NonSippNavigator

  "MemberDetailsNavigator" - {

    "NormalMode" - {

      act.like(
        normalmode
          .navigateToWithData(
            PensionSchemeMembersPage,
            Gen.const(ManualOrUpload.Manual),
            routes.MemberDetailsController.onPageLoad(_, refineMV(1), _)
          )
          .withName("go from manual or upload to details page when manual is chosen")
      )

      act.like(
        normalmode
          .navigateToWithData(
            PensionSchemeMembersPage,
            Gen.const(ManualOrUpload.Upload),
            (srn, _) => controllers.nonsipp.memberdetails.routes.HowToUploadController.onPageLoad(srn)
          )
          .withName("go from manual or upload to how to upload page when upload is chosen")
      )

      act.like(
        normalmode
          .navigateTo(
            MemberDetailsPage(_, refineMV(1)),
            routes.DoesSchemeMemberHaveNINOController.onPageLoad(_, refineMV(1), _)
          )
          .withName("go from details to have a nino page")
      )

      act.like(
        normalmode
          .navigateTo(
            MemberDetailsPage(_, refineMV(1)),
            routes.DoesSchemeMemberHaveNINOController.onPageLoad(_, refineMV(1), _)
          )
          .withName("go from member details page to does member have nino page")
      )

//      act.like(
//        normalmode
//          .navigateTo(
//            MemberDetailsNinoPage(_, refineMV(1)),
//            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
//          )
//          .withName("go from nino page to check answers page")
//      )

//      act.like(
//        normalmode
//          .navigateTo(
//            NoNINOPage(_, refineMV(1)),
//            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
//          )
//          .withName("go from no nino page to scheme member details answers page")
//      )

      act.like(
        normalmode
          .navigateTo(
            SchemeMemberDetailsAnswersPage,
            (srn, _) => routes.SchemeMembersListController.onPageLoad(srn, page = 1, Manual)
          )
          .withName("go from scheme member details answers page to scheme members list Page")
      )

      act.like(
        normalmode
          .navigateFromListPage(
            SchemeMembersListPage(_, addMember = true, Manual),
            MemberDetailsPage,
            nameDobGen,
            IndexGen[OneTo300](min = 1, max = 300),
            (srn, _: Refined[Int, OneTo300], _) => routes.PensionSchemeMembersController.onPageLoad(srn),
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
            MemberDetailsPage,
            nameDobGen,
            IndexGen[OneTo300](min = 1, max = 300),
            (srn, _: Refined[Int, OneTo300], _) => routes.PensionSchemeMembersController.onPageLoad(srn),
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
            RemoveMemberDetailsPage,
            (srn, _) => routes.SchemeMembersListController.onPageLoad(srn, page = 1, Manual)
          )
          .withName("go from remove page to list page")
      )

      act.like(
        normalmode
          .navigateTo(
            HowToUploadPage,
            (srn, _) => controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)
          )
          .withName("go from how to upload page  to upload member details page")
      )

      act.like(
        normalmode
          .navigateTo(
            UploadMemberDetailsPage,
            routes.CheckMemberDetailsFileController.onPageLoad
          )
          .withName("go from upload member details page to check member details file page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckMemberDetailsFilePage,
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
            FileUploadSuccessPage,
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
            FileUploadErrorSummaryPage,
            (srn, _) => controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)
          )
          .withName("go from file upload error summary page to upload a file page")
      )

      act.like(
        normalmode
          .navigateTo(
            FileUploadTooManyErrorsPage,
            (srn, _) => controllers.nonsipp.memberdetails.routes.UploadMemberDetailsController.onPageLoad(srn)
          )
          .withName("go from too many file upload errors page to upload a file page")
      )
    }

    "CheckMode" - {

      act.like(
        checkmode
          .navigateTo(
            MemberDetailsPage(_, refineMV(1)),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
          )
          .withName("go from member details page to check answers page")
      )

      act.like(
        checkmode
          .navigateTo(
            MemberDetailsNinoPage(_, refineMV(1)),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
          )
          .withName("go from member details nino page to check your answers page")
      )

      act.like(
        checkmode
          .navigateTo(
            NoNINOPage(_, refineMV(1)),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
          )
          .withName("go from no nino page to check your answers page")
      )

      "does member have nino page should go to" - {

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, index),
              Gen.const(ConditionalYesNo.yes[String, Nino](nino)),
              (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
            )
            .withName("go from does member have ni page to scheme members CYA page")
        )

        act.like(
          checkmode
            .navigateTo(
              UploadMemberDetailsPage,
              routes.CheckMemberDetailsFileController.onPageLoad
            )
            .withName("go from upload member details page to check member details file page")
        )
      }
    }
  }

}
