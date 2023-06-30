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
import models.{NormalMode, ReceivedLoanType, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.whoreceivedloan.WhoReceivedLoanPage
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
    case WhoReceivedLoanPage(srn) =>
      userAnswers.get(WhoReceivedLoanPage(srn)) match {
        case Some(ReceivedLoanType.Other) =>
          controllers.nonsipp.otherrecipientdetails.routes.OtherRecipientDetailsController.onPageLoad(srn, NormalMode)
        case Some(ReceivedLoanType.Individual) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController
            .onPageLoad(srn, NormalMode)
        case Some(ReceivedLoanType.UKCompany) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientNameController
            .onPageLoad(srn, NormalMode)
        case Some(ReceivedLoanType.UKPartnership) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientNameController
            .onPageLoad(srn, NormalMode)
      }

    case page @ CompanyRecipientNamePage(srn) =>
      routes.UnauthorisedController.onPageLoad()

    case IndividualRecipientNamePage(srn) =>
      controllers.routes.IndividualRecipientNinoController.onPageLoad(srn, NormalMode)

    case IndividualRecipientNinoPage(srn) =>
      controllers.nonsipp.routes.IsMemberOrConnectedPartyController.onPageLoad(srn, NormalMode)

    case PartnershipRecipientNamePage(srn) =>
      controllers.routes.UnauthorisedController.onPageLoad()
  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
}
