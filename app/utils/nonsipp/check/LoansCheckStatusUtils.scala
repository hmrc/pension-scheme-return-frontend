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

package utils.nonsipp.check

import models.ConditionalYesNo._
import models.IdentityType._
import config.RefinedTypes.{Max5000, OneTo5000}
import models.SchemeId.Srn
import eu.timepit.refined.refineV
import models._
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject._

object LoansCheckStatusUtils {

  /**
   * This method determines whether or not the Loans section needs to be checked. A section needs to be checked if 1 or
   * more records in that section need to be checked.
   * @param userAnswers the answers provided by the user, from which we get each Loans record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @return true if any record requires checking, else false
   */
  def checkLoansSection(
    userAnswers: UserAnswers,
    srn: Srn
  ): Boolean = {
    val schemeHadLoans = userAnswers.get(LoansMadeOrOutstandingPage(srn))
    val journeysStartedList =
      userAnswers.get(IdentityTypes(srn, IdentitySubject.LoanRecipient)).getOrElse(Map.empty).keys.toList

    schemeHadLoans match {
      case Some(false) => false
      case _ =>
        journeysStartedList
          .map(
            index => {
              refineV[OneTo5000](index.toInt + 1).fold(
                _ => List.empty,
                refinedIndex => checkLoansRecord(userAnswers, srn, refinedIndex)
              )
            }
          )
          .contains(true)
    }
  }

  /**
   * This method determines whether or not a Loans record needs to be checked. A record needs checking if any of the
   * pre-populated-then-cleared answers are missing & all of the other answers are present.
   * @param userAnswers the answers provided by the user, from which we get the Loans record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @param recordIndex the index of the record being checked
   * @return true if the record requires checking, else false
   */
  def checkLoansRecord(
    userAnswers: UserAnswers,
    srn: Srn,
    recordIndex: Max5000
  ): Boolean = {
    val anyPrePopClearedAnswersMissing: Boolean = (
      userAnswers.get(AmountOfTheLoanPage(srn, recordIndex)),
      userAnswers.get(InterestOnLoanPage(srn, recordIndex)),
      userAnswers.get(ArrearsPrevYears(srn, recordIndex)),
      userAnswers.get(OutstandingArrearsOnLoanPage(srn, recordIndex))
    ) match {
      case (Some(amountOfTheLoan), Some(interestOnLoan), Some(_), Some(_)) =>
        amountOfTheLoan.hasMissingAnswers || interestOnLoan.hasMissingAnswer
      case (_, _, _, _) => true
    }

    lazy val allOtherAnswersPresent: Boolean = (
      userAnswers.get(IdentityTypePage(srn, recordIndex, LoanRecipient)),
      userAnswers.get(DatePeriodLoanPage(srn, recordIndex)),
      userAnswers.get(AmountOfTheLoanPage(srn, recordIndex)),
      userAnswers.get(AreRepaymentsInstalmentsPage(srn, recordIndex)),
      userAnswers.get(InterestOnLoanPage(srn, recordIndex)),
      userAnswers.get(SecurityGivenForLoanPage(srn, recordIndex))
    ) match {
      case (Some(identityType), Some(_), Some(amountOfTheLoan), Some(_), Some(interestOnLoan), Some(_)) =>
        identitySubjectAnswersPresent(userAnswers, srn, recordIndex, identityType)
      case (_, _, _, _, _, _) =>
        false
    }

    anyPrePopClearedAnswersMissing && allOtherAnswersPresent
  }

  /**
   * This method determines whether or not all answers are present for a given IdentityType.
   * @param userAnswers the answers provided by the user
   * @param srn the Scheme Reference Number, used for the .get calls
   * @param recordIndex the index of the record being checked
   * @param identityType relates to the seller involved: Individual, UKCompany, UKPartnership, or Other
   * @return true if all answers are present, else false
   */
  private def identitySubjectAnswersPresent(
    userAnswers: UserAnswers,
    srn: Srn,
    recordIndex: Max5000,
    identityType: IdentityType
  ): Boolean =
    identityType match {
      case Individual =>
        (
          userAnswers.get(IndividualRecipientNamePage(srn, recordIndex)),
          userAnswers.get(IndividualRecipientNinoPage(srn, recordIndex)),
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, recordIndex))
        ) match {
          case (Some(_), Some(_), Some(_)) => true
          case (_, _, _) => false
        }
      case UKCompany =>
        (
          userAnswers.get(CompanyRecipientNamePage(srn, recordIndex)),
          userAnswers.get(CompanyRecipientCrnPage(srn, recordIndex, LoanRecipient)),
          userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, recordIndex))
        ) match {
          case (Some(_), Some(_), Some(_)) => true
          case (_, _, _) => false
        }
      case UKPartnership =>
        (
          userAnswers.get(PartnershipRecipientNamePage(srn, recordIndex)),
          userAnswers.get(PartnershipRecipientUtrPage(srn, recordIndex, LoanRecipient)),
          userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, recordIndex))
        ) match {
          case (Some(_), Some(_), Some(_)) => true
          case (_, _, _) => false
        }
      case Other =>
        (
          userAnswers.get(OtherRecipientDetailsPage(srn, recordIndex, LoanRecipient)),
          userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, recordIndex))
        ) match {
          case (Some(_), Some(_)) => true
          case (_, _) => false
        }
    }
}
