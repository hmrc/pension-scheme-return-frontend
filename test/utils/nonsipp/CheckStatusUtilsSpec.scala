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

import org.scalatest.matchers.must.Matchers
import models.IdentityType._
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import utils.nonsipp.CheckStatusUtils._
import models._
import pages.nonsipp.common._
import models.IdentitySubject.LandOrPropertySeller
import config.RefinedTypes._
import controllers.ControllerBaseSpec
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino

class CheckStatusUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  // Test values
  private val name: String = "name"
  private val reason: String = "reason"
  private val index1of5000: Max5000 = refineMV(1)
  private val index2of5000: Max5000 = refineMV(2)
  private val conditionalYesNino: ConditionalYesNo[String, Nino] = ConditionalYesNo.yes(nino)
  private val conditionalYesCrn: ConditionalYesNo[String, Crn] = ConditionalYesNo.yes(crn)
  private val conditionalYesUtr: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)

  // Land or Property
  private val landOrPropertyHeldTrue = defaultUserAnswers.unsafeSet(LandOrPropertyHeldPage(srn), true)
  private val landOrPropertyHeldFalse = defaultUserAnswers.unsafeSet(LandOrPropertyHeldPage(srn), false)

  private def addLOPBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(LandPropertyInUKPage(srn, index), true)
      .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
      .unsafeSet(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo.no[String, String](reason))
      .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)

  // LOP: Branching on WhyDoesSchemeHoldLandPropertyPage
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

  // LOP: Branching on IdentityTypePage
  private def addLOPIndividualAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LandOrPropertySeller), Individual)
      .unsafeSet(LandPropertyIndividualSellersNamePage(srn, index), name)
      .unsafeSet(IndividualSellerNiPage(srn, index), conditionalYesNino)

  private def addLOPUKCompanyAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LandOrPropertySeller), UKCompany)
      .unsafeSet(CompanySellerNamePage(srn, index), name)
      .unsafeSet(CompanyRecipientCrnPage(srn, index, LandOrPropertySeller), conditionalYesCrn)

  private def addLOPUKPartnershipAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LandOrPropertySeller), UKPartnership)
      .unsafeSet(PartnershipSellerNamePage(srn, index), name)
      .unsafeSet(PartnershipRecipientUtrPage(srn, index, LandOrPropertySeller), conditionalYesUtr)

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

      "when 1 record is present and needs checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              landOrPropertyHeldTrue
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }

      "when 2 records are present and 1 needs checking" in {
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
                    landOrPropertyHeldTrue
                  )
                )
              )
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when landOrPropertyHeld is None" in {
        val userAnswers = defaultUserAnswers

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when landOrPropertyHeld is Some(false)" in {
        val userAnswers = landOrPropertyHeldFalse

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when landOrPropertyHeld is Some(true) and no records are present" in {
        val userAnswers = landOrPropertyHeldTrue

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }

      "when landOrPropertyHeld is Some(true) and 1 record is present which doesn't need checking" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPTransferAnswers(
              index1of5000,
              addLOPPrePopAnswers(
                index1of5000,
                landOrPropertyHeldTrue
              )
            )
          )

        checkLandOrPropertySection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkLandOrPropertyRecord" - {

    "must be true" - {

      "for valid pre-pop answers (Acquisition & Individual)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              addLOPIndividualAnswers(
                index1of5000,
                landOrPropertyHeldTrue
              )
            )
          )

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "for valid pre-pop answers (Acquisition & UKCompany)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              addLOPUKCompanyAnswers(
                index1of5000,
                landOrPropertyHeldTrue
              )
            )
          )

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "for valid pre-pop answers (Acquisition & UKPartnership)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              addLOPUKPartnershipAnswers(
                index1of5000,
                landOrPropertyHeldTrue
              )
            )
          )

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "for valid pre-pop answers (Acquisition & Other)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPAcquisitionAnswers(
              index1of5000,
              addLOPOtherAnswers(
                index1of5000,
                landOrPropertyHeldTrue
              )
            )
          )

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "for valid pre-pop answers (Contribution)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              landOrPropertyHeldTrue
            )
          )

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "for valid pre-pop answers (Transfer)" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPTransferAnswers(
              index1of5000,
              landOrPropertyHeldTrue
            )
          )

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "when some pre-pop-cleared answers are present and some are missing" in {
        val userAnswers =
          addLOPBaseAnswers(
            index1of5000,
            addLOPContributionAnswers(
              index1of5000,
              landOrPropertyHeldTrue
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
                landOrPropertyHeldTrue
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
                landOrPropertyHeldTrue
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
                landOrPropertyHeldTrue
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
              landOrPropertyHeldTrue
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
              landOrPropertyHeldTrue
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
              landOrPropertyHeldTrue
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
              landOrPropertyHeldTrue
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), Other)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }
  }
}
