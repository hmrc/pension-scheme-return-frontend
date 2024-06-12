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

import utils.BaseSpec
import config.Refined.Max300
import org.scalacheck.Gen
import navigation.{Navigator, NavigatorBehaviours}
import models.NormalMode
import pages.nonsipp.memberpensionpayments._
import eu.timepit.refined.refineMV

class PensionPaymentsReceivedNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max300.Refined](1)

  "PensionPaymentsReceivedNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          PensionPaymentsReceivedPage,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
              .onPageLoad(srn, page = 1, NormalMode)
        )
        .withName("go from pension payments received page to member pension payments list page on yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          PensionPaymentsReceivedPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from pension payments received page to task list page son no selected")
    )
  }

  "MemberPensionPaymentsListPage" - {
    act.like(
      normalmode
        .navigateTo(
          MemberPensionPaymentsListPage,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from  member pension payments list page to task list page")
    )
  }

  "TotalAmountPensionPaymentsPage" - {

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          TotalAmountPensionPaymentsPage.apply,
          (srn, index: Max300, _) =>
            controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from total amount pension payments page to member pension payments CYA page")
    )
  }

  "MemberPensionPaymentsCYAPage" - {
    act.like(
      normalmode
        .navigateTo(
          MemberPensionPaymentsCYAPage,
          (srn, _) =>
            controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
              .onPageLoad(srn, page = 1, NormalMode)
        )
        .withName("go from member pension payments CYA page to member pension payments list page")
    )
  }

  "RemovePensionPaymentsPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          RemovePensionPaymentsPage,
          (srn, _: Max300, _) =>
            controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from remove pension payments page to member pension payments list page")
    )
  }

}
