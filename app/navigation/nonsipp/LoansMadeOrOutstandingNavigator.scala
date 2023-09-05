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

import config.Refined.OneTo5000
import eu.timepit.refined.{refineMV, refineV}
import models.CheckOrChange.Check
import models.ConditionalYesNo._
import models.{CheckOrChange, IdentitySubject, IdentityType, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage, IdentityTypes, OtherRecipientDetailsPage}
import pages.nonsipp.loansmadeoroutstanding._
import play.api.mvc.Call

object LoansMadeOrOutstandingNavigator extends JourneyNavigator {

  // scalastyle:off
  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ LoansMadeOrOutstandingPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.loansmadeoroutstanding.routes.WhatYouWillNeedLoansController.onPageLoad(srn)
      } else {
        controllers.nonsipp.sharesinsponsoringemployer.routes.DidSchemeHoldSharesInSponsoringEmployerController
          .onPageLoad(srn, NormalMode)
      }

    case WhatYouWillNeedLoansPage(srn) =>
      // if final loans page was completed, route user to loans list page
      userAnswers.get(OutstandingArrearsOnLoanPage(srn, refineMV(1))) match {
        case Some(_) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController.onPageLoad(srn, page = 1, NormalMode)
        case None =>
          controllers.nonsipp.common.routes.IdentityTypeController
            .onPageLoad(srn, refineMV(1), NormalMode, IdentitySubject.LoanRecipient)
      }
    case IdentityTypePage(srn, index, IdentitySubject.LoanRecipient) =>
      userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient)) match {
        case Some(IdentityType.Other) =>
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.Individual) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKCompany) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientNameController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKPartnership) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientNameController
            .onPageLoad(srn, index, NormalMode)
      }

    case IndividualRecipientNamePage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNinoController
        .onPageLoad(srn, index, NormalMode)

    case IndividualRecipientNinoPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.IsIndividualRecipientConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case CompanyRecipientNamePage(srn, index) =>
      controllers.nonsipp.common.routes.CompanyRecipientCrnController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.LoanRecipient)

    case CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case PartnershipRecipientNamePage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientUtrController
        .onPageLoad(srn, index, NormalMode)

    case OtherRecipientDetailsPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case PartnershipRecipientUtrPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController
        .onPageLoad(srn, index, NormalMode)

    case IsIndividualRecipientConnectedPartyPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.DatePeriodLoanController
        .onPageLoad(srn, index, NormalMode)

    case RecipientSponsoringEmployerConnectedPartyPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.DatePeriodLoanController
        .onPageLoad(srn, index, NormalMode)

    case DatePeriodLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.AmountOfTheLoanController
        .onPageLoad(srn, index, NormalMode)

    case AmountOfTheLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.AreRepaymentsInstalmentsController
        .onPageLoad(srn, index, NormalMode)

    case AreRepaymentsInstalmentsPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.InterestOnLoanController.onPageLoad(srn, index, NormalMode)

    case InterestOnLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.SecurityGivenForLoanController
        .onPageLoad(srn, index, NormalMode)

    case SecurityGivenForLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.OutstandingArrearsOnLoanController
        .onPageLoad(srn, index, NormalMode)

    case OutstandingArrearsOnLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, CheckOrChange.Check)

    case LoansCYAPage(srn) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController.onPageLoad(srn, page = 1, NormalMode)

    case LoansListPage(srn, addLoan @ true) =>
      refineV[OneTo5000](userAnswers.map(IdentityTypes(srn, IdentitySubject.LoanRecipient)).size + 1) match {
        case Left(_) => controllers.routes.JourneyRecoveryController.onPageLoad()
        case Right(nextIndex) =>
          controllers.nonsipp.common.routes.IdentityTypeController
            .onPageLoad(srn, nextIndex, NormalMode, IdentitySubject.LoanRecipient)
      }

    case LoansListPage(srn, addLoan @ false) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case RemoveLoanPage(srn, index) =>
      if (userAnswers.map(IdentityTypes(srn, IdentitySubject.LoanRecipient)).isEmpty) {
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController.onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController.onPageLoad(srn, page = 1, NormalMode)
      }
  }
  // scalastyle:on

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ IdentityTypePage(srn, index, IdentitySubject.LoanRecipient) =>
      userAnswers.get(page) match {
        case Some(IdentityType.Individual) if userAnswers.get(IndividualRecipientNamePage(srn, index)).isEmpty =>
          controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKCompany) if userAnswers.get(CompanyRecipientNamePage(srn, index)).isEmpty =>
          controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientNameController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.UKPartnership) if userAnswers.get(PartnershipRecipientNamePage(srn, index)).isEmpty =>
          controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientNameController
            .onPageLoad(srn, index, NormalMode)
        case Some(IdentityType.Other) if userAnswers.get(OtherRecipientDetailsPage(srn, index)).isEmpty =>
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index, NormalMode)
        case Some(_) =>
          controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
            .onPageLoad(srn, index, Check)
        case None =>
          controllers.nonsipp.loansmadeoroutstanding.routes.WhatYouWillNeedLoansController.onPageLoad(srn)
      }

    case IndividualRecipientNamePage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case CompanyRecipientNamePage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case PartnershipRecipientNamePage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case OtherRecipientDetailsPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case IndividualRecipientNinoPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case PartnershipRecipientUtrPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case IsIndividualRecipientConnectedPartyPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case RecipientSponsoringEmployerConnectedPartyPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case DatePeriodLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case AmountOfTheLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case AreRepaymentsInstalmentsPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case InterestOnLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case SecurityGivenForLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)

    case OutstandingArrearsOnLoanPage(srn, index) =>
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
        .onPageLoad(srn, index, Check)
  }
}
