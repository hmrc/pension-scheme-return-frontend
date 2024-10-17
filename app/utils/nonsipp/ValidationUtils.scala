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

package utils.nonsipp

import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import config.RefinedTypes.{Max50, Max5000}
import models.SchemeId.Srn
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.CheckReturnDatesPage
import models.UserAnswers

object ValidationUtils {

  /**
   * This method determines whether or not a return is missing any necessary answers by calling the validation
   * method for each section in turn.
   * @param userAnswers The answers provided by the user which are checked for validity
   * @param srn The Scheme Reference Number, used for the .get calls in the section validation methods
   * @return true if all answers in all sections are valid, false if any necessary answers in any section are missing
   */
  def validateAllSections(userAnswers: UserAnswers, srn: Srn): Boolean =
    validateBasicDetailsSection(userAnswers, srn) &&
      //...
      validateLandOrPropertySection(userAnswers, srn)
  //...

  /**
   * This method determines whether or not the Basic Details section is missing any necessary answers.
   * @param userAnswers The answers provided by the user which are checked for validity
   * @param srn The Scheme Reference Number, used for the .get calls
   * @return true if all answers in this section are valid, false if any necessary answers are missing
   */
  def validateBasicDetailsSection(userAnswers: UserAnswers, srn: Srn): Boolean = {
    val checkReturnDates = userAnswers.get(CheckReturnDatesPage(srn))
    val accountingPeriods = userAnswers.get(AccountingPeriods(srn))
    val activeBankAccount = userAnswers.get(ActiveBankAccountPage(srn))
    val whyNoBankAccount = userAnswers.get(WhyNoBankAccountPage(srn))
    val howManyMembers = userAnswers.get(HowManyMembersPage.bySrn(srn))

    (checkReturnDates, accountingPeriods, activeBankAccount, whyNoBankAccount, howManyMembers) match {
      case (Some(true), _, Some(true), None, Some(_)) => true
      case (Some(true), _, Some(false), Some(_), Some(_)) => true
      case (Some(false), Some(dateRangeList), Some(true), None, Some(_)) if dateRangeList.nonEmpty => true
      case (Some(false), Some(dateRangeList), Some(false), Some(_), Some(_)) if dateRangeList.nonEmpty => true
      case (_, _, _, _, _) => false
    }
  }

  def validateLandOrPropertySection(
    userAnswers: UserAnswers,
    srn: Srn
  ): Boolean =
    true

  def validateLandOrPropertyJourney(
    userAnswers: UserAnswers,
    srn: Srn,
    sectionIndex: Max5000
  ): Boolean =
    true

  def validateLandOrPropertyDisposalJourney(
    userAnswers: UserAnswers,
    srn: Srn,
    sectionIndex: Max5000,
    disposalIndex: Max50
  ): Boolean =
    true
}
