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
import models.IdentityType._
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import models._
import pages.nonsipp.common._
import models.IdentitySubject.LandOrPropertySeller
import viewmodels.models.SectionCompleted
import config.RefinedTypes._
import controllers.ControllerBaseSpec
import utils.nonsipp.ValidationUtils._
import pages.nonsipp.CheckReturnDatesPage
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino

class ValidationUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  // Set test values
  private val name: String = "name"
  private val reason: String = "reason"
  private val index1of3: Max3 = refineMV(1)
  private val index1of5000: Max5000 = refineMV(1)
  private val index2of5000: Max5000 = refineMV(2)
  private val index3of5000: Max5000 = refineMV(3)
  private val conditionalYesNino: ConditionalYesNo[String, Nino] = ConditionalYesNo.yes(nino)
  private val conditionalNoNino: ConditionalYesNo[String, Nino] = ConditionalYesNo.no(reason)
  private val conditionalYesCrn: ConditionalYesNo[String, Crn] = ConditionalYesNo.yes(crn)
  private val conditionalNoCrn: ConditionalYesNo[String, Crn] = ConditionalYesNo.no(reason)
  private val conditionalYesUtr: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)
  private val conditionalNoUtr: ConditionalYesNo[String, Utr] = ConditionalYesNo.no(reason)

  private val baseLandOrPropertyAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyHeldPage(srn), true)
    .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.no[String, String](reason))
    //.unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // X--
    //.unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    //.unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKCompany) // -X-
    //.unsafeSet(CompanySellerNamePage(srn, index1of5000), name)
    //.unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, LandOrPropertySeller), conditionalYesCrn)
    //.unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
    //.unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index1of5000), false)
    //.unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true) // --X
    //.unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    //.unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index1of5000), money)
    .unsafeSet(LandOrPropertyCompleted(srn, index1of5000), SectionCompleted)

  // AAA = Acquisition, Individual, Leased
  private val landOrPropertyAnswersAAA = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), Individual) // -A-
    .unsafeSet(LandPropertyIndividualSellersNamePage(srn, index1of5000), name)
    .unsafeSet(IndividualSellerNiPage(srn, index1of5000), conditionalYesNino)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true) // --A
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), true)

  // AAB = Acquisition, Individual, Not Leased
  private val landOrPropertyAnswersAAB = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), Individual) // -A-
    .unsafeSet(LandPropertyIndividualSellersNamePage(srn, index1of5000), name)
    .unsafeSet(IndividualSellerNiPage(srn, index1of5000), conditionalNoNino)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false) // --B

  // ABA = Acquisition, UKCompany, Leased
  private val landOrPropertyAnswersABA = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKCompany) // -B-
    .unsafeSet(CompanySellerNamePage(srn, index1of5000), name)
    .unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, LandOrPropertySeller), conditionalYesCrn)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true) // --A
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), false)

  // ABB = Acquisition, UKCompany, Not Leased
  private val landOrPropertyAnswersABB = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKCompany) // -B-
    .unsafeSet(CompanySellerNamePage(srn, index1of5000), name)
    .unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, LandOrPropertySeller), conditionalNoCrn)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false) // --B

  // ACA = Acquisition, UKPartnership, Leased
  private val landOrPropertyAnswersACA = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKPartnership) // -C-
    .unsafeSet(PartnershipSellerNamePage(srn, index1of5000), name)
    .unsafeSet(PartnershipRecipientUtrPage(srn, index1of5000, LandOrPropertySeller), conditionalYesUtr)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true) // --A
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), true)

  // ACB = Acquisition, UKPartnership, Not Leased
  private val landOrPropertyAnswersACB = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKPartnership) // -C-
    .unsafeSet(PartnershipSellerNamePage(srn, index1of5000), name)
    .unsafeSet(PartnershipRecipientUtrPage(srn, index1of5000, LandOrPropertySeller), conditionalNoUtr)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false) // --B

  // ADA = Acquisition, Other, Leased
  private val landOrPropertyAnswersADA = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), Other) // -D-
    .unsafeSet(OtherRecipientDetailsPage(srn, index1of5000, LandOrPropertySeller), otherRecipientDetails)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true) // --A
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), false)

  // ADB = Acquisition, Other, Not Leased
  private val landOrPropertyAnswersADB = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition) // A--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), Other) // -D-
    .unsafeSet(OtherRecipientDetailsPage(srn, index1of5000, LandOrPropertySeller), otherRecipientDetails)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false) // --B

  // BxA = Contribution, (IdentityType N/A), Leased
  private val landOrPropertyAnswersBxA = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Contribution) // B--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true) // --A
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), true)

  // BxB = Contribution, (IdentityType N/A), Not Leased
  private val landOrPropertyAnswersBxB = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Contribution) // B--
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false) // --B

  // CxA = Transfer, (IdentityType N/A), Leased
  private val landOrPropertyAnswersCxA = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer) // C--
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true) // --A
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), false)

  // CxB = Transfer, (IdentityType N/A), Not Leased
  private val landOrPropertyAnswersCxB = baseLandOrPropertyAnswers
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer) // C--
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false) // --B

  "answersMissingAllSections" - {

    "must be false when no answers are missing" in {
      val userAnswers = landOrPropertyAnswersCxB
        .unsafeSet(CheckReturnDatesPage(srn), true)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      answersMissingAllSections(userAnswers, srn) mustBe false
    }

    "must be true when some answers are missing" in {
      val userAnswers = defaultUserAnswers

      answersMissingAllSections(userAnswers, srn) mustBe true
    }
  }

  "answersMissingBasicDetailsSection" - {
    "must be false for valid path AA" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswers, srn)

      result mustBe false
    }

    "must be false for valid path AB" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswers, srn)

      result mustBe false
    }

    "must be false for valid path BA" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswers, srn)

      result mustBe false
    }

    "must be false for valid path BB" in {
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

    "must be true for invalid path - single answer missing" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswersA, srn)

      result mustBe true
    }

    "must be true for invalid path - multiple answers missing" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
      //.unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = answersMissingBasicDetailsSection(userAnswersA, srn)

      result mustBe true
    }

    "must be true for invalid path - no answers missing but invalid combination" in {
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

  "answersMissingLandOrPropertySection" - {

    "must be false when LandOrPropertyHeldPage is Some(true) and all answers are present" in {
      val userAnswers = landOrPropertyAnswersBxA

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be true when LandOrPropertyHeldPage is Some(true) and some answers are missing" in {
      val userAnswers = landOrPropertyAnswersABB
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

  "answersMissingLandOrPropertyJourney" - {

    "must be false when all answers present for path AAA: Acquisition, Individual, Leased" in {
      val userAnswers = landOrPropertyAnswersAAA

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path AAB: Acquisition, Individual, Not Leased" in {
      val userAnswers = landOrPropertyAnswersAAB

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path ABA: Acquisition, UKCompany, Leased" in {
      val userAnswers = landOrPropertyAnswersABA

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path ABB: Acquisition, UKCompany, Not Leased" in {
      val userAnswers = landOrPropertyAnswersABB

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path ACA: Acquisition, UKPartnership, Leased" in {
      val userAnswers = landOrPropertyAnswersACA

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path ACB: Acquisition, UKPartnership, Not Leased" in {
      val userAnswers = landOrPropertyAnswersACB

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path ADA: Acquisition, Other, Leased" in {
      val userAnswers = landOrPropertyAnswersADA

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path ADB: Acquisition, Other, Not Leased" in {
      val userAnswers = landOrPropertyAnswersADB

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path BxA: Contribution, (IdentityType N/A), Leased" in {
      val userAnswers = landOrPropertyAnswersBxA

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path BxB: Contribution, (IdentityType N/A), Not Leased" in {
      val userAnswers = landOrPropertyAnswersBxB

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path CxA: Transfer, (IdentityType N/A), Leased" in {
      val userAnswers = landOrPropertyAnswersCxA

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be false when all answers present for path CxB: Transfer, (IdentityType N/A), Not Leased" in {
      val userAnswers = landOrPropertyAnswersCxB

      answersMissingLandOrPropertySection(userAnswers, srn) mustBe false
    }

    "must be true when some answers are missing" in {
      val userAnswers = landOrPropertyAnswersACA
        .unsafeRemove(PartnershipRecipientUtrPage(srn, index1of5000, LandOrPropertySeller))

      answersMissingLandOrPropertyJourney(userAnswers, srn, index1of5000) mustBe true
    }
  }
}
