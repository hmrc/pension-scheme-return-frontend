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
import models.SchemeHoldLandProperty._
import config.RefinedTypes.{Max5000, OneTo5000}
import models.SchemeId.Srn
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineV
import models._
import pages.nonsipp.common._
import models.IdentitySubject._

object LandOrPropertyCheckStatusUtils {

  /**
   * This method determines whether or not the Land or Property section needs to be checked. A section needs to be
   * checked if 1 or more records in that section need to be checked.
   * @param userAnswers the answers provided by the user, from which we get each Land or Property record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @return true if any record requires checking, else false
   */
  def checkLandOrPropertySection(
    userAnswers: UserAnswers,
    srn: Srn
  ): Boolean = {
    val landOrPropertyHeld = userAnswers.get(LandOrPropertyHeldPage(srn))
    val journeysStartedList = userAnswers.get(LandPropertyInUKPages(srn)).getOrElse(Map.empty).keys.toList

    landOrPropertyHeld match {
      case Some(false) => false
      case _ =>
        journeysStartedList
          .map(
            index => {
              refineV[OneTo5000](index.toInt + 1).fold(
                _ => List.empty,
                refinedIndex => checkLandOrPropertyRecord(userAnswers, srn, refinedIndex)
              )
            }
          )
          .contains(true)
    }
  }

  /**
   * This method determines whether or not a Land or Property record needs to be checked. A record needs checking if
   * any of the pre-populated-then-cleared answers are missing & all of the other answers are present.
   * @param userAnswers the answers provided by the user, from which we get the Land or Property record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @param recordIndex the index of the record being checked
   * @return true if the record requires checking, else false
   */
  def checkLandOrPropertyRecord(
    userAnswers: UserAnswers,
    srn: Srn,
    recordIndex: Max5000
  ): Boolean = {
    val anyPrePopClearedAnswersMissing: Boolean = (
      userAnswers.get(IsLandOrPropertyResidentialPage(srn, recordIndex)),
      userAnswers.get(IsLandPropertyLeasedPage(srn, recordIndex)),
      userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, recordIndex)),
      userAnswers.get(IsLesseeConnectedPartyPage(srn, recordIndex)),
      userAnswers.get(LandOrPropertyTotalIncomePage(srn, recordIndex))
    ) match {
      case (Some(_), Some(true), Some(_), Some(_), Some(_)) => false
      case (Some(_), Some(false), None, None, Some(_)) => false
      case (_, _, _, _, _) => true
    }

    lazy val allOtherAnswersPresent: Boolean = (
      userAnswers.get(LandPropertyInUKPage(srn, recordIndex)),
      userAnswers.get(LandOrPropertyChosenAddressPage(srn, recordIndex)),
      userAnswers.get(LandRegistryTitleNumberPage(srn, recordIndex)),
      userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, recordIndex)),
      userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, recordIndex)), // if Acquisition || Contribution
      // IdentityType pages are handled by the identitySubjectAnswersPresent method
      userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, recordIndex)), // if Acquisition
      userAnswers.get(LandOrPropertyTotalCostPage(srn, recordIndex)),
      userAnswers.get(LandPropertyIndependentValuationPage(srn, recordIndex)) // if Acquisition || Contribution
    ) match {
      // Acquisition
      case (Some(_), Some(_), Some(_), Some(Acquisition), Some(_), Some(_), Some(_), Some(_)) =>
        userAnswers.get(IdentityTypePage(srn, recordIndex, LandOrPropertySeller)) match {
          case None => false
          case Some(identityType) =>
            identitySubjectAnswersPresent(userAnswers, srn, recordIndex, identityType)
        }
      // Contribution
      case (Some(_), Some(_), Some(_), Some(Contribution), Some(_), None, Some(_), Some(_)) =>
        true
      // Transfer
      case (Some(_), Some(_), Some(_), Some(Transfer), None, None, Some(_), None) =>
        true
      case (_, _, _, _, _, _, _, _) =>
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
          userAnswers.get(LandPropertyIndividualSellersNamePage(srn, recordIndex)),
          userAnswers.get(IndividualSellerNiPage(srn, recordIndex))
        ) match {
          case (Some(_), Some(_)) => true
          case (_, _) => false
        }
      case UKCompany =>
        (
          userAnswers.get(CompanySellerNamePage(srn, recordIndex)),
          userAnswers.get(CompanyRecipientCrnPage(srn, recordIndex, LandOrPropertySeller))
        ) match {
          case (Some(_), Some(_)) => true
          case (_, _) => false
        }
      case UKPartnership =>
        (
          userAnswers.get(PartnershipSellerNamePage(srn, recordIndex)),
          userAnswers.get(PartnershipRecipientUtrPage(srn, recordIndex, LandOrPropertySeller))
        ) match {
          case (Some(_), Some(_)) => true
          case (_, _) => false
        }
      case Other =>
        userAnswers.get(OtherRecipientDetailsPage(srn, recordIndex, LandOrPropertySeller)) match {
          case Some(_) => true
          case None => false
        }
    }
}
