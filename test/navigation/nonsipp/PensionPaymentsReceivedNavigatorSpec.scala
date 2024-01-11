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

import config.Refined.Max300
import eu.timepit.refined.refineMV
import models.NormalMode
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.memberpensionpayments._
import utils.BaseSpec

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
            controllers.nonsipp.memberpensionpayments.routes.WhatYouWillNeedPensionPaymentsController.onPageLoad(srn)
        )
        .withName("go from pension payments received page to what you will need pension payments page on yes selected")
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

  "WhatYouWillNeedPensionPaymentsPage" - {

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedPensionPaymentsPage,
          (srn, _) =>
            controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
              .onPageLoad(srn, page = 1, NormalMode)
        )
        .withName("go from what you will need pension payments page to member pension payments list page")
    )

  }

  "TotalAmountPensionPaymentsPage" - {

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          TotalAmountPensionPaymentsPage,
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

}
