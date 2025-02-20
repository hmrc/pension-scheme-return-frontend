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

import models.IdentityType._
import pages.nonsipp.landorproperty._
import utils.nonsipp.check.LandOrPropertyCheckStatusUtils.{checkLandOrPropertyRecord, checkLandOrPropertySection}
import org.scalatest.OptionValues
import models._
import pages.nonsipp.common._
import models.IdentitySubject._
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec

class LandOrPropertyCheckStatusUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  private val conditionalYesNoLRTN: ConditionalYesNo[String, String] = ConditionalYesNo.yes("landRegistryTitleNumber")

  private val landOrPropertyHeldTrueLegacy = defaultUserAnswers.unsafeSet(LandOrPropertyHeldPage(srn), true)
  private val landOrPropertyHeldFalseLegacy = defaultUserAnswers.unsafeSet(LandOrPropertyHeldPage(srn), false)

  private def addLOPBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(LandPropertyInUKPage(srn, index), true)
      .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
      .unsafeSet(LandRegistryTitleNumberPage(srn, index), conditionalYesNoLRTN)
      .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)

  // Branching on WhyDoesSchemeHoldLandPropertyPage
  private def addLOPAcquisitionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Acquisition)
      .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index), localDate)
      .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index), true)
      .unsafeSet(LandPropertyIndependentValuationPage(srn, index), true)

  private def addLOPContributionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Contribution)
      .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index), localDate)
      .unsafeSet(LandPropertyIndependentValuationPage(srn, index), true)

  private def addLOPTransferAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Transfer)

  // Branching on IdentityTypePage
  private def addLOPIndividualAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LandOrPropertySeller), Individual)
      .unsafeSet(LandPropertyIndividualSellersNamePage(srn, index), name)
      .unsafeSet(IndividualSellerNiPage(srn, index), conditionalYesNoNino)

  private def addLOPUKCompanyAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LandOrPropertySeller), UKCompany)
      .unsafeSet(CompanySellerNamePage(srn, index), name)
      .unsafeSet(CompanyRecipientCrnPage(srn, index, LandOrPropertySeller), conditionalYesNoCrn)

  private def addLOPUKPartnershipAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LandOrPropertySeller), UKPartnership)
      .unsafeSet(PartnershipSellerNamePage(srn, index), name)
      .unsafeSet(PartnershipRecipientUtrPage(srn, index, LandOrPropertySeller), conditionalYesNoUtr)

  private def addLOPOtherAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LandOrPropertySeller), Other)
      .unsafeSet(OtherRecipientDetailsPage(srn, index, LandOrPropertySeller), otherRecipientDetails)

  private def addLOPPrePopAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IsLandOrPropertyResidentialPage(srn, index), true)
      .unsafeSet(IsLandPropertyLeasedPage(srn, index), true)
      .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index), (leaseName, money, localDate))
      .unsafeSet(IsLesseeConnectedPartyPage(srn, index), true)
      .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)

  "checkLandOrPropertySection" - {

    "must be true" - {

      "when landOrPropertyHeld is Some(true) & 1 record is present, which needs checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              landOrPropertyHeldTrueLegacy
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }

      "when landOrPropertyHeld is Some(true) & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPTransferAnswers(
              index1of5000,
              addLOPBaseAnswers(
                index2of5000,
                addLOPContributionAnswers(
                  index2of5000,
                  addLOPPrePopAnswers(
                    index2of5000,
                    landOrPropertyHeldTrueLegacy
                  )
                )
              )
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }

      "when landOrPropertyHeld is None & 1 record is present, which needs checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPTransferAnswers(
              index1of5000,
              defaultUserAnswers
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }

      "when landOrPropertyHeld is None & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              addLOPBaseAnswers(
                index2of5000,
                addLOPTransferAnswers(
                  index2of5000,
                  addLOPPrePopAnswers(
                    index2of5000,
                    defaultUserAnswers
                  )
                )
              )
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when landOrPropertyHeld is Some(false)" in {
        val userAnswers = landOrPropertyHeldFalseLegacy

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when landOrPropertyHeld is Some(true) & no records are present" in {
        val userAnswers = landOrPropertyHeldTrueLegacy

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when landOrPropertyHeld is Some(true) & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPTransferAnswers(
              index1of5000,
              addLOPPrePopAnswers(
                index1of5000,
                landOrPropertyHeldTrueLegacy
              )
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when landOrPropertyHeld is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when landOrPropertyHeld is None & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              addLOPPrePopAnswers(
                index1of5000,
                defaultUserAnswers
              )
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkLandOrPropertyRecord" - {

    "must be true" - {

      "when all pre-pop-cleared answers are missing & all other answers are present" - {

        "(Acquisition & Individual)" in {
          val userAnswers =
            addLOPBaseAnswers(
              index1of5000,
              addLOPAcquisitionAnswers(
                index1of5000,
                addLOPIndividualAnswers(
                  index1of5000,
                  landOrPropertyHeldTrueLegacy
                )
              )
            )

          checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & UKCompany)" in {
          val userAnswers =
            addLOPBaseAnswers(
              index1of5000,
              addLOPAcquisitionAnswers(
                index1of5000,
                addLOPUKCompanyAnswers(
                  index1of5000,
                  landOrPropertyHeldTrueLegacy
                )
              )
            )

          checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & UKPartnership)" in {
          val userAnswers =
            addLOPBaseAnswers(
              index1of5000,
              addLOPAcquisitionAnswers(
                index1of5000,
                addLOPUKPartnershipAnswers(
                  index1of5000,
                  landOrPropertyHeldTrueLegacy
                )
              )
            )

          checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & Other)" in {
          val userAnswers =
            addLOPBaseAnswers(
              index1of5000,
              addLOPAcquisitionAnswers(
                index1of5000,
                addLOPOtherAnswers(
                  index1of5000,
                  landOrPropertyHeldTrueLegacy
                )
              )
            )

          checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Contribution)" in {
          val userAnswers =
            addLOPBaseAnswers(
              index1of5000,
              addLOPContributionAnswers(
                index1of5000,
                landOrPropertyHeldTrueLegacy
              )
            )

          checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Transfer)" in {
          val userAnswers =
            addLOPBaseAnswers(
              index1of5000,
              addLOPTransferAnswers(
                index1of5000,
                landOrPropertyHeldTrueLegacy
              )
            )

          checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
        }
      }

      "when some pre-pop-cleared answers are present & some are missing" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              landOrPropertyHeldTrueLegacy
            )
          ).unsafeSet(IsLandOrPropertyResidentialPage(srn, index1of5000), true)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }
    }

    "must be false" - {

      "when all answers are missing" in {
        val userAnswers = defaultUserAnswers

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when 1 other answer is missing" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              addLOPIndividualAnswers(
                index1of5000,
                landOrPropertyHeldTrueLegacy
              )
            )
          ).unsafeRemove(IdentityTypePage(srn, index1of5000, LandOrPropertySeller))

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when all pre-pop-cleared answers are present (leased)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPTransferAnswers(
              index1of5000,
              addLOPPrePopAnswers(
                index1of5000,
                landOrPropertyHeldTrueLegacy
              )
            )
          )

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when all pre-pop-cleared answers are present (not leased)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              addLOPPrePopAnswers(
                index1of5000,
                landOrPropertyHeldTrueLegacy
              )
            )
          ).unsafeSet(IsLandOrPropertyResidentialPage(srn, index1of5000), true)
            .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false)
            .unsafeSet(LandOrPropertyTotalIncomePage(srn, index1of5000), money)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Individual answers are missing" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              landOrPropertyHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), Individual)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKCompany answers are missing" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              landOrPropertyHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKCompany)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKPartnership answers are missing" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              landOrPropertyHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKPartnership)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Other answer is missing" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              landOrPropertyHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), Other)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }
  }

  private val landOrPropertyHeldTrue = defaultUserAnswers.unsafeSet(LandOrPropertyHeldPage(srn), true)
  private val landOrPropertyHeldFalse = defaultUserAnswers.unsafeSet(LandOrPropertyHeldPage(srn), false)

  private def addNonPrePopRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addLOPBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Acquisition)
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), Individual)

  private def addUncheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addLOPBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Contribution)
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), UKCompany)
      .unsafeSet(LandOrPropertyPrePopulated(srn, index), false)

  private def addCheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addLOPBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Transfer)
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), UKPartnership)
      .unsafeSet(LandOrPropertyPrePopulated(srn, index), true)

  "checkBondsSectionPre-Pop" - {

    "must be true" - {

      "when schemeHadBonds is None & 1 record is present (unchecked)" in {
        val userAnswers = addUncheckedRecord(index1of5000, defaultUserAnswers)

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is Some(true) & 2 records are present (checked and unchecked)" in {
        val userAnswers = addCheckedRecord(index1of5000, addUncheckedRecord(index2of5000, landOrPropertyHeldTrue))

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is Some(true) & 2 records are present (unchecked and non-pre-pop)" in {
        val userAnswers = addUncheckedRecord(index1of5000, addNonPrePopRecord(index2of5000, landOrPropertyHeldTrue))

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when schemeHadLoans is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(false) & no records are present" in {
        val userAnswers = landOrPropertyHeldFalse

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & no records are present" in {
        val userAnswers = landOrPropertyHeldTrue

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 1 record is present (checked)" in {
        val userAnswers = addCheckedRecord(index1of5000, landOrPropertyHeldTrue)

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 1 record is present (non-pre-pop)" in {
        val userAnswers = addNonPrePopRecord(index1of5000, landOrPropertyHeldTrue)

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 2 records are present (checked and non-pre-pop)" in {
        val userAnswers = addCheckedRecord(index1of5000, addNonPrePopRecord(index2of5000, landOrPropertyHeldTrue))

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkLoansRecord" - {

    "must be true" - {

      "when record is (unchecked)" in {
        val userAnswers = addUncheckedRecord(index1of5000, defaultUserAnswers)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }
    }

    "must be false" - {

      "when record is (checked)" in {
        val userAnswers = addCheckedRecord(index1of5000, defaultUserAnswers)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when record is (non-pre-pop)" in {
        val userAnswers = addNonPrePopRecord(index1of5000, defaultUserAnswers)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }

  }

}
