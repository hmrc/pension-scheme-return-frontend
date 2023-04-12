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

import controllers.nonsipp.memberdetails.routes
import eu.timepit.refined.refineMV
import models.{CheckOrChange, ManualOrUpload}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.memberdetails.{
  MemberDetailsNinoPage,
  MemberDetailsPage,
  PensionSchemeMembersPage,
  RemoveMemberDetailsPage
}
import utils.BaseSpec

class MemberDetailsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new Navigator

  "MemberDetailsNavigator" - {

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
        .navigateToWithData(
          PensionSchemeMembersPage,
          Gen.const(ManualOrUpload.Manual),
          (srn, _) => routes.MemberDetailsController.onPageLoad(srn, refineMV(1))
        )
        .withName("go from manual or upload to details page when manual is chosen")
    )

    act.like(
      normalmode
        .navigateToWithData(
          PensionSchemeMembersPage,
          Gen.const(ManualOrUpload.Upload),
          (_, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from manual or upload to unauthorised page when upload is chosen")
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
          MemberDetailsNinoPage(_, refineMV(1)),
          (srn, _) => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), CheckOrChange.Check)
        )
        .withName("go from nino page to check answers page")
    )
  }

}
