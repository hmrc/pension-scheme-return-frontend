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

import controllers.routes
import models.{NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.loansmadeoroutstanding.{LoansMadeOrOutstandingPage, WhatYouWillNeedLoansPage}
import play.api.mvc.Call

object LoanMadeOrOutstandingNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ LoansMadeOrOutstandingPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.loansmadeoroutstanding.routes.WhatYouWillNeedLoansController.onPageLoad(srn)
      } else {
        controllers.nonsipp.sharesinsponsoringemployer.routes.DidSchemeHoldSharesInSponsoringEmployerController
          .onPageLoad(srn, NormalMode)
      }

    case WhatYouWillNeedLoansPage(srn) =>
      controllers.nonsipp.whoreceivedloan.routes.WhoReceivedLoanController.onPageLoad(srn)
  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
}
