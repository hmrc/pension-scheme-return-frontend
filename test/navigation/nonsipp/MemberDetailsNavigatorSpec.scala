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
import eu.timepit.refined.refineMV
import generators.IndexGen
import models.SchemeId.Srn
import models.{CheckOrChange, ManualOrUpload, NormalMode}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.CheckingMemberDetailsFilePage
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberdetails.upload.{FileUploadErrorPage, FileUploadSuccessPage}
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class MemberDetailsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

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

      act.like(
        normalmode
          .navigateToWithData(
            DoesMemberHaveNinoPage(_, refineMV(1)),
            Gen.const(true),
            routes.MemberDetailsNinoController.onPageLoad(_, refineMV(1), _)
          )
          .withName("go from does member have nino Page to member details nino page when yes selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            DoesMemberHaveNinoPage(_, refineMV(1)),
            Gen.const(false),
            routes.NoNINOController.onPageLoad(_, refineMV(1), _)
          )
          .withName("go from does member have nino Page to no nino page when no selected")
      )

      act.like(
        normalmode
          .navigateTo(
            MemberDetailsNinoPage(_, refineMV(1)),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
          )
          .withName("go from nino page to check answers page")
      )

      act.like(
        normalmode
          .navigateTo(
            NoNINOPage(_, refineMV(1)),
            (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
          )
          .withName("go from no nino page to scheme member details answers page")
      )

      act.like(
        normalmode
          .navigateTo(
            SchemeMemberDetailsAnswersPage,
            (srn, _) => routes.SchemeMembersListController.onPageLoad(srn, page = 1)
          )
          .withName("go from scheme member details answers page to scheme members list Page")
      )

      act.like(
        normalmode
          .navigateFromListPage(
            SchemeMembersListPage(_, addMember = true),
            MemberDetailsPage,
            nameDobGen,
            IndexGen[OneTo99](min = 1, max = 99),
            routes.MemberDetailsController.onPageLoad,
            employercontributions.routes.EmployerContributionsController.onPageLoad
          )
          .withName("go from scheme members list page to member details page when yes selected")
      )

      act.like(
        normalmode
          .navigateTo(
            SchemeMembersListPage(_, addMember = false),
            employercontributions.routes.EmployerContributionsController.onPageLoad
          )
          .withName("go from scheme members list page to employer contributions page when no selected")
      )

      act.like(
        normalmode
          .navigateTo(
            RemoveMemberDetailsPage,
            (srn, _) => routes.SchemeMembersListController.onPageLoad(srn, page = 1)
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
            (srn, _) => controllers.nonsipp.memberdetails.routes.SchemeMembersListController.onPageLoad(srn, 1)
          )
          .withName("go from file upload success page to scheme members list page")
      )
    }

    act.like(
      normalmode
        .navigateTo(
          FileUploadErrorPage,
          (_, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from file upload error page  to unauthorised page")
    )

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

        val userAnswersWithNino =
          (srn: Srn) => defaultUserAnswers.unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), ninoGen.sample.value)
        val userAnswersWithNoNinoReason =
          (srn: Srn) => defaultUserAnswers.unsafeSet(NoNINOPage(srn, refineMV(1)), nonEmptyAlphaString.sample.value)

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, refineMV(1)),
              Gen.const(true),
              routes.MemberDetailsNinoController.onPageLoad(_, refineMV(1), _)
            )
            .withName("nino page when yes selected and no data")
        )

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, refineMV(1)),
              Gen.const(false),
              routes.NoNINOController.onPageLoad(_, refineMV(1), _)
            )
            .withName("no nino page when no selected and no data")
        )

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, refineMV(1)),
              Gen.const(true),
              (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check),
              userAnswersWithNino
            )
            .withName("check answers page when yes selected and nino exists")
        )

        act.like(
          checkmode
            .navigateToWithData(
              DoesMemberHaveNinoPage(_, refineMV(1)),
              Gen.const(false),
              (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check),
              userAnswersWithNoNinoReason
            )
            .withName("check answers page when no selected and no nino reason exists")
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
