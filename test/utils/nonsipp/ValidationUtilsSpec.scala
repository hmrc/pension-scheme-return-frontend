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
import org.scalatest.matchers.must.Matchers
import models.IdentityType.UKCompany
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import models._
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage}
import models.IdentitySubject.LandOrPropertySeller
import config.RefinedTypes._
import controllers.ControllerBaseSpec
import utils.nonsipp.ValidationUtils._
import pages.nonsipp.CheckReturnDatesPage
import org.scalatest.OptionValues

class ValidationUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  // Set test values
  private val name: String = "name"
  private val reason: String = "reason"
  private val index1of3: Max3 = refineMV(1)
  private val index1of5000: Max5000 = refineMV(1)
  private val index2of5000: Max5000 = refineMV(2)
  private val index3of5000: Max5000 = refineMV(3)

  // (S6) Land or Property
  private val validLandOrPropertyAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyHeldPage(srn), true)
    .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
    .unsafeSet(LandOrPropertyPostcodeLookupPage(srn, index1of5000), postcodeLookup)
    .unsafeSet(AddressLookupResultsPage(srn, index1of5000), List(address, address, address))
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.no[String, String](reason))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition)
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKCompany)
    .unsafeSet(CompanySellerNamePage(srn, index1of5000), name)
    .unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, LandOrPropertySeller), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true)
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index1of5000), money)

  "answersMissingAllSections" - { //todo

    "must be false when no necessary answers are missing" in {
//      val userAnswers = defaultUserAnswers
//      // (S1) Basic Details
//        .unsafeSet(CheckReturnDatesPage(srn), true)
//        .unsafeSet(ActiveBankAccountPage(srn), true)
//        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
//        // (S?) Land or Property
//        .unsafeSet(LandOrPropertyHeldPage(srn), true)
//        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
//        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)

      val userAnswers = validLandOrPropertyAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      answersMissingAllSections(userAnswers, srn) mustBe false
    }

    "must be true when some necessary answers are missing" in {
      val userAnswers = defaultUserAnswers

      answersMissingAllSections(userAnswers, srn) mustBe true
    }
  }

  "answersMissingBasicDetailsSection" - {
    "must be false for valid path A" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswers, srn)

      result mustBe false
    }

    "must be false for valid path B" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswers, srn)

      result mustBe false
    }

    "must be false for valid path C" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswers, srn)

      result mustBe false
    }

    "must be false for valid path D" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswers, srn)

      result mustBe false
    }

    "must be true for invalid path - no answers" in {
      val userAnswersA: UserAnswers = defaultUserAnswers

      val result: Boolean = answersMissingBasicDetailsSection(userAnswersA, srn)

      result mustBe true
    }

    "must be true for invalid path - single necessary answer missing" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswersA, srn)

      result mustBe true
    }

    "must be true for invalid path - multiple necessary answers missing" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
      //.unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswersA, srn)

      result mustBe true
    }

    "must be true for invalid path - no necessary answers missing but invalid combination" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswersA, srn)

      result mustBe true
    }
  }

  "answersMissingLandOrPropertySection" - { //todo

    "must be false when LandOrPropertyHeldPage is Some(true) and no necessary answers are missing" in {
//      val userAnswers = defaultUserAnswers
//        .unsafeSet(LandOrPropertyHeldPage(srn), true)
//        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
//        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
////        .unsafeSet(LandPropertyInUKPage(srn, index2of5000), false)
////        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index2of5000), address)
//        .unsafeSet(LandPropertyInUKPage(srn, index3of5000), false)
//        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index3of5000), address)

      val userAnswers = validLandOrPropertyAnswers

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be true when LandOrPropertyHeldPage is Some(true) and some necessary answers are missing" in {
//      val userAnswers = defaultUserAnswers
//        .unsafeSet(LandOrPropertyHeldPage(srn), true)
//        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
//        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
//        .unsafeSet(LandPropertyInUKPage(srn, index2of5000), false)
////        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index2of5000), address)
//        .unsafeSet(LandPropertyInUKPage(srn, index3of5000), false)
//        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index3of5000), address)

      val userAnswers = validLandOrPropertyAnswers
        .unsafeRemove(IdentityTypePage(srn, index1of5000, LandOrPropertySeller))

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe true
    }

    "must be false when LandOrPropertyHeldPage is Some(false)" in {
      val userAnswers = defaultUserAnswers.unsafeSet(LandOrPropertyHeldPage(srn), false)

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when LandOrPropertyHeldPage is None" in {
      val userAnswers = defaultUserAnswers

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }
  }

  "answersMissingLandOrPropertyJourney" - { //todo

    "must be false when no necessary answers are missing" in {
      val userAnswers = validLandOrPropertyAnswers

      answersMissingLandOrPropertyJourney(userAnswers, srn, index1of5000) mustBe false
    }

    "must be true when some necessary answers are missing" in {
      val userAnswers = validLandOrPropertyAnswers
        .unsafeRemove(IdentityTypePage(srn, index1of5000, LandOrPropertySeller))

      answersMissingLandOrPropertyJourney(userAnswers, srn, index1of5000) mustBe true
    }
  }
}
