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

import config.Refined.{Max300, Max50}
import eu.timepit.refined.refineMV
import models.{IdentityType, NormalMode}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.employercontributions._
import pages.nonsipp.memberpayments.EmployerContributionsPage
import utils.BaseSpec

class EmployerContributionsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

  "EmployerContributionsNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          EmployerContributionsPage,
          Gen.const(true),
          controllers.nonsipp.memberpayments.routes.UnallocatedEmployerContributionsController.onPageLoad
        )
        .withName("go from employer contribution page to unallocated employer contributions page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          EmployerContributionsPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from employer contribution page to task list page when no selected")
    )

  }

  "EmployerNamePage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          EmployerNamePage,
          (srn, index: Max300, secondaryIndex: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.EmployerTypeOfBusinessController
              .onPageLoad(srn, index, secondaryIndex, NormalMode)
        )
        .withName("go from employer name page to employer type of business page")
    )
  }

  "EmployerTypeOfBusinessPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleDataAndIndex(
          index,
          secondaryIndex,
          EmployerTypeOfBusinessPage,
          Gen.const(IdentityType.UKCompany),
          (srn, memberIndex: Max300, index: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from employer type of business page to unauthorised")
    )

    act.like(
      normalmode
        .navigateToWithDoubleDataAndIndex(
          index,
          secondaryIndex,
          EmployerTypeOfBusinessPage,
          Gen.const(IdentityType.UKPartnership),
          (srn, memberIndex: Max300, index: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from employer type of business page to unauthorised page")
    )

    act.like(
      normalmode
        .navigateToWithDoubleDataAndIndex(
          index,
          secondaryIndex,
          EmployerTypeOfBusinessPage,
          Gen.const(IdentityType.Other),
          (srn, memberIndex: Max300, index: Max50, _) =>
            controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
              .onPageLoad(srn, memberIndex, index, NormalMode)
        )
        .withName("go from employer type of business page to unauthorised controller page")
    )
  }

  "OtherEmployeeDescriptionPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          secondaryIndex,
          OtherEmployeeDescriptionPage,
          (srn, index: Max300, secondaryIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from OtherEmployeeDescriptionPage to ??? page")
    )

  }
}
