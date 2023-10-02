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

package utils.nonsipp

import models.SchemeId.Srn
import models.{IdentitySubject, NormalMode, PensionSchemeId, UserAnswers}
import pages.nonsipp.common.IdentityTypes
import pages.nonsipp.loansmadeoroutstanding.{LoansMadeOrOutstandingPage, OutstandingArrearsOnLoanPages}
import pages.nonsipp.memberdetails.{MemberDetailsNinoPages, MembersDetailsPages, NoNinoPages}
import pages.nonsipp.schemedesignatory.{FeesCommissionsWagesSalariesPage, HowManyMembersPage, HowMuchCashPage}
import viewmodels.models.TaskListStatus.{Completed, InProgress, NotStarted}
import models.ConditionalYesNo._

object TasklistStatusUtils {
  def getBasicSchemeDetailsTaskListStatus(
    srn: Srn,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId,
    activeBankAccount: Option[Boolean],
    whyNoBankAccountPage: Option[String]
  ) =
    (userAnswers.get(HowManyMembersPage(srn, pensionSchemeId)), activeBankAccount, whyNoBankAccountPage) match {
      case (None, _, _) => InProgress
      case (Some(_), Some(true), _) => Completed
      case (Some(_), Some(false), Some(_)) => Completed
      case (Some(_), Some(false), None) => InProgress
    }

  def getFinancialDetailsTaskListStatus(userAnswers: UserAnswers, srn: Srn) = {
    val totalSalaries = userAnswers.get(FeesCommissionsWagesSalariesPage(srn, NormalMode))
    val howMuchCash = userAnswers.get(HowMuchCashPage(srn, NormalMode))
    (howMuchCash, totalSalaries) match {
      case (Some(_), Some(_)) => Completed
      case (None, _) => NotStarted
      case (Some(_), None) => InProgress
    }
  }

  def getMembersTaskListStatus(userAnswers: UserAnswers, srn: Srn) = {
    val membersDetailsPages = userAnswers.get(MembersDetailsPages(srn))
    val ninoPages = userAnswers.get(MemberDetailsNinoPages(srn))
    val noNinoPages = userAnswers.get(NoNinoPages(srn))
    (membersDetailsPages, ninoPages, noNinoPages) match {
      case (None, _, _) => NotStarted
      case (Some(_), None, None) => InProgress
      case (Some(memberDetails), ninos, noNinos) =>
        if (memberDetails.isEmpty) {
          NotStarted
        } else {
          val countMemberDetails = memberDetails.size
          val countNinos = ninos.getOrElse(List.empty).size
          val countNoninos = noNinos.getOrElse(List.empty).size
          if (countMemberDetails > countNinos + countNoninos) {
            InProgress
          } else {
            Completed
          }
        }
    }
  }

  def getLoansTaskListStatus(userAnswers: UserAnswers, srn: Srn) = {
    val loansMadeOrOutstandingPage = userAnswers.get(LoansMadeOrOutstandingPage(srn))
    val whoReceivedTheLoanPages = userAnswers.get(IdentityTypes(srn, IdentitySubject.LoanRecipient))
    val outstandingArrearsOnLoanPages = userAnswers.get(OutstandingArrearsOnLoanPages(srn))
    (loansMadeOrOutstandingPage, whoReceivedTheLoanPages, outstandingArrearsOnLoanPages) match {
      case (None, _, _) => NotStarted
      case (Some(loansMadeOrOutstanding), whoReceivedTheLoan, outstandingArrearsOnLoan) =>
        if (!loansMadeOrOutstanding) {
          Completed
        } else {
          val countLoanTransactions = whoReceivedTheLoanPages.getOrElse(List.empty).size
          val countLastPage = outstandingArrearsOnLoanPages.getOrElse(List.empty).size
          if (countLoanTransactions + countLastPage == 0) {
            InProgress
          } else if (countLoanTransactions > countLastPage) {
            InProgress
          } else {
            Completed
          }
        }
    }
  }

}
