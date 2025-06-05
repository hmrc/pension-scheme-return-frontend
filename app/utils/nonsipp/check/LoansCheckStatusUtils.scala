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
   * @param userAnswers
   *   the answers provided by the user, from which we get each Loans record
   * @param srn
   *   the Scheme Reference Number, used for the .get calls
   * @return
   *   true if any record requires checking, else false
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
        journeysStartedList.exists(index => {
          refineV[OneTo5000](index.toInt + 1).fold(
            _ => false,
            refinedIndex => checkLoansRecord(userAnswers, srn, refinedIndex)
          )
        })
    }
  }

  /**
   * This method determines whether or not a Loans record needs to be checked. A record only needs to be checked if its
   * LoanPrePopulated field is false.
   * @param userAnswers
   *   the answers provided by the user, from which we get the Loans record
   * @param srn
   *   the Scheme Reference Number, used for the .get calls
   * @param recordIndex
   *   the index of the record being checked
   * @return
   *   true if the record requires checking, else false
   */
  def checkLoansRecord(
    userAnswers: UserAnswers,
    srn: Srn,
    recordIndex: Max5000
  ): Boolean =
    userAnswers.get(LoanPrePopulated(srn, recordIndex)) match {
      case Some(checked) => !checked
      case None => false // non-pre-pop
    }
}
