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
import models.IdentityType._
import models.SchemeHoldLandProperty._
import config.RefinedTypes.{Max5000, OneTo5000}
import models.SchemeId.Srn
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineV
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.CheckReturnDatesPage
import models.{IdentitySubject, IdentityType, UserAnswers}
import pages.nonsipp.common._
import models.IdentitySubject._

object ValidationUtils {

  /**
   * This method determines whether or not a return is missing any necessary answers across all sections.
   * @param userAnswers the answers provided by the user which are checked for validity
   * @param srn the Scheme Reference Number, used for the .get calls in the section validation methods
   * @return true if any necessary answers in any section are missing, else false
   */
  def answersMissingAllSections(userAnswers: UserAnswers, srn: Srn): Boolean =
    answersMissingBasicDetailsSection(userAnswers, srn) ||
      answersMissingLandOrPropertySection(userAnswers, srn)

  /**
   * This method determines whether or not the Basic Details section is missing any necessary answers.
   * @param userAnswers the answers provided by the user which are checked for validity
   * @param srn the Scheme Reference Number, used for the .get calls
   * @return true if any necessary answers are missing, else false
   */
  def answersMissingBasicDetailsSection(userAnswers: UserAnswers, srn: Srn): Boolean = {
    val checkReturnDates = userAnswers.get(CheckReturnDatesPage(srn))
    val accountingPeriods = userAnswers.get(AccountingPeriods(srn))
    val activeBankAccount = userAnswers.get(ActiveBankAccountPage(srn))
    val whyNoBankAccount = userAnswers.get(WhyNoBankAccountPage(srn))
    val howManyMembers = userAnswers.get(HowManyMembersPage.bySrn(srn))

    (checkReturnDates, accountingPeriods, activeBankAccount, whyNoBankAccount, howManyMembers) match {
      case (Some(true), _, Some(true), None, Some(_)) => false
      case (Some(true), _, Some(false), Some(_), Some(_)) => false
      case (Some(false), Some(dateRangeList), Some(true), None, Some(_)) if dateRangeList.nonEmpty => false
      case (Some(false), Some(dateRangeList), Some(false), Some(_), Some(_)) if dateRangeList.nonEmpty => false
      case (_, _, _, _, _) => true
    }
  }

  /**
   * This method determines whether or not the Land or Property section is missing any necessary answers.
   * @param userAnswers the answers provided by the user which are checked for validity
   * @param srn the Scheme Reference Number, used for the .get calls
   * @return true if any necessary answers are missing, else false
   */
  def answersMissingLandOrPropertySection(
    userAnswers: UserAnswers,
    srn: Srn
  ): Boolean = {
    val landOrPropertyHeld = userAnswers.get(LandOrPropertyHeldPage(srn))
    val journeysStartedList = userAnswers.get(LandPropertyInUKPages(srn)).getOrElse(Map.empty).keys.toList

    landOrPropertyHeld match {
      case None => false
      case Some(false) => false
      case Some(true) =>
        journeysStartedList
          .map(
            index => {
              refineV[OneTo5000](index.toInt + 1).fold(
                _ => List.empty,
                refinedIndex => answersMissingLandOrPropertyJourney(userAnswers, srn, refinedIndex)
              )
            }
          )
          .contains(true)
    }
  }

  /**
   * This method determines whether or not a Land or Property journey is missing any necessary answers.
   * @param userAnswers the answers provided by the user which are checked for validity
   * @param srn the Scheme Reference Number, used for the .get calls
   * @param sectionIndex the index of the journey being checked
   * @return true if any necessary answers are missing, else false
   */
  def answersMissingLandOrPropertyJourney(
    userAnswers: UserAnswers,
    srn: Srn,
    sectionIndex: Max5000
  ): Boolean = {
    val initialAnswers = (
      userAnswers.get(LandPropertyInUKPage(srn, sectionIndex)),
      userAnswers.get(LandOrPropertyChosenAddressPage(srn, sectionIndex)),
      userAnswers.get(LandRegistryTitleNumberPage(srn, sectionIndex)),
      userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, sectionIndex)),
      userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, sectionIndex)),
      // IdentityType pages are handled by the answersMissingIdentityQuestions method
      userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, sectionIndex)),
      userAnswers.get(LandOrPropertyTotalCostPage(srn, sectionIndex)),
      userAnswers.get(LandPropertyIndependentValuationPage(srn, sectionIndex)),
      userAnswers.get(IsLandOrPropertyResidentialPage(srn, sectionIndex))
    ) match {
      // Acquisition
      case (Some(_), Some(_), Some(_), Some(Acquisition), Some(_), Some(_), Some(_), Some(_), Some(_)) =>
        userAnswers.get(IdentityTypePage(srn, sectionIndex, LandOrPropertySeller)) match {
          case None => true
          case Some(identityType) =>
            answersMissingIdentityQuestions(userAnswers, srn, sectionIndex, identityType, LandOrPropertySeller)
        }
      // Contribution
      case (Some(_), Some(_), Some(_), Some(Contribution), Some(_), None, Some(_), Some(_), Some(_)) =>
        false
      // Transfer
      case (Some(_), Some(_), Some(_), Some(Transfer), None, None, Some(_), None, Some(_)) =>
        false
      case (_, _, _, _, _, _, _, _, _) =>
        true
    }
    initialAnswers || (
      (
        userAnswers.get(IsLandPropertyLeasedPage(srn, sectionIndex)),
        userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, sectionIndex)),
        userAnswers.get(IsLesseeConnectedPartyPage(srn, sectionIndex)),
        userAnswers.get(LandOrPropertyTotalIncomePage(srn, sectionIndex))
      ) match {
        case (Some(true), Some(_), Some(_), Some(_)) => false
        case (Some(false), None, None, Some(_)) => false
        case (_, _, _, _) => true
      }
    )
  }

  /**
   * This method determines whether or not any necessary answers are missing for a given IdentityType & IdentitySubject.
   * @param userAnswers the answers provided by the user which are checked for validity
   * @param srn the Scheme Reference Number, used for the .get calls
   * @param sectionIndex the index of the journey being checked
   * @param identityType relates to the section this is called from: Loans, Land or Property, Shares, or Other Assets
   * @param identitySubject relates to the seller involved: Individual, UKCompany, UKPartnership, or Other
   * @return true if any necessary answers are missing, else false
   */
  private def answersMissingIdentityQuestions(
    userAnswers: UserAnswers,
    srn: Srn,
    sectionIndex: Max5000,
    identityType: IdentityType,
    identitySubject: IdentitySubject
  ): Boolean =
    identitySubject match {
      case LoanRecipient =>
        true
      case LandOrPropertySeller =>
        identityType match {
          case Individual =>
            (
              userAnswers.get(LandPropertyIndividualSellersNamePage(srn, sectionIndex)),
              userAnswers.get(IndividualSellerNiPage(srn, sectionIndex))
            ) match {
              case (Some(_), Some(_)) => false
              case (_, _) => true
            }
          case UKCompany =>
            (
              userAnswers.get(CompanySellerNamePage(srn, sectionIndex)),
              userAnswers.get(CompanyRecipientCrnPage(srn, sectionIndex, identitySubject))
            ) match {
              case (Some(_), Some(_)) => false
              case (_, _) => true
            }
          case UKPartnership =>
            (
              userAnswers.get(PartnershipSellerNamePage(srn, sectionIndex)),
              userAnswers.get(PartnershipRecipientUtrPage(srn, sectionIndex, LandOrPropertySeller))
            ) match {
              case (Some(_), Some(_)) => false
              case (_, _) => true
            }
          case Other =>
            userAnswers.get(OtherRecipientDetailsPage(srn, sectionIndex, LandOrPropertySeller)) match {
              case Some(_) => false
              case None => true
            }
        }
      case SharesSeller =>
        true
      case OtherAssetSeller =>
        true
      case Unknown =>
        true
    }
}
