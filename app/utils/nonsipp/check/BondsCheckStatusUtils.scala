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

import pages.nonsipp.bonds._
import models.SchemeId.Srn
import eu.timepit.refined.refineV
import models.UserAnswers
import config.RefinedTypes.{Max5000, OneTo5000}
import models.SchemeHoldBond._

object BondsCheckStatusUtils {

  /**
   * This method determines whether or not the Bonds section needs to be checked. A section needs to be checked if 1 or
   * more records in that section need to be checked.
   * @param userAnswers the answers provided by the user, from which we get each Bonds record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @return true if any record requires checking, else false
   */
  def checkBondsSection(
    userAnswers: UserAnswers,
    srn: Srn
  ): Boolean = {
    val bondsHeld = userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn))
    val journeysStartedList = userAnswers.get(NameOfBondsPages(srn)).getOrElse(Map.empty).keys.toList

    bondsHeld match {
      case Some(false) => false
      case _ =>
        journeysStartedList
          .map(
            index => {
              refineV[OneTo5000](index.toInt + 1).fold(
                _ => List.empty,
                refinedIndex => checkBondsRecord(userAnswers, srn, refinedIndex)
              )
            }
          )
          .contains(true)
    }
  }

  /**
   * This method determines whether or not a Bonds record needs to be checked. A record needs checking if any of the
   * pre-populated-then-cleared answers are missing & all of the other answers are present.
   * @param userAnswers the answers provided by the user, from which we get the Bonds record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @param recordIndex the index of the record being checked
   * @return true if the record requires checking, else false
   */
  def checkBondsRecord(
    userAnswers: UserAnswers,
    srn: Srn,
    recordIndex: Max5000
  ): Boolean = {
    val anyPrePopClearedAnswersMissing: Boolean = userAnswers.get(IncomeFromBondsPage(srn, recordIndex)).isEmpty

    lazy val allOtherAnswersPresent: Boolean = (
      userAnswers.get(NameOfBondsPage(srn, recordIndex)),
      userAnswers.get(WhyDoesSchemeHoldBondsPage(srn, recordIndex)),
      userAnswers.get(WhenDidSchemeAcquireBondsPage(srn, recordIndex)), // if Acquisition || Contribution
      userAnswers.get(CostOfBondsPage(srn, recordIndex)),
      userAnswers.get(BondsFromConnectedPartyPage(srn, recordIndex)), // if Acquisition
      userAnswers.get(AreBondsUnregulatedPage(srn, recordIndex))
    ) match {
      // Acquisition
      case (Some(_), Some(Acquisition), Some(_), Some(_), Some(_), Some(_)) =>
        true
      // Contribution
      case (Some(_), Some(Contribution), Some(_), Some(_), None, Some(_)) =>
        true
      // Transfer
      case (Some(_), Some(Transfer), None, Some(_), None, Some(_)) =>
        true
      case (_, _, _, _, _, _) =>
        false
    }

    anyPrePopClearedAnswersMissing && allOtherAnswersPresent
  }
}
