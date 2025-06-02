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

import play.api.mvc.Call
import pages.Page
import utils.IntUtils.toInt
import pages.nonsipp.memberpensionpayments._
import navigation.JourneyNavigator
import models.{NormalMode, UserAnswers}

object PensionPaymentsReceivedNavigator extends JourneyNavigator {

  val normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ PensionPaymentsReceivedPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
          .onPageLoad(srn, page = 1, NormalMode)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case MemberPensionPaymentsListPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case TotalAmountPensionPaymentsPage(srn, index) =>
      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
        .onPageLoad(srn, index, NormalMode)

    case MemberPensionPaymentsCYAPage(srn) =>
      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
        .onPageLoad(srn, page = 1, NormalMode)

    case RemovePensionPaymentsPage(srn, _) =>
      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
        .onPageLoad(srn, page = 1, NormalMode)

  }

  val checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ =>
    userAnswers => {

      case page @ PensionPaymentsReceivedPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
            .onPageLoad(srn, page = 1, NormalMode)
        } else {
          controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        }
      case TotalAmountPensionPaymentsPage(srn, index) =>
        controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
          .onPageLoad(srn, index, NormalMode)

      case MemberPensionPaymentsCYAPage(srn) =>
        controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
          .onPageLoad(srn, page = 1, NormalMode)
    }

}
