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

import org.scalatest.matchers.must.Matchers
import pages.nonsipp.otherassetsheld._
import models.IdentityType._
import utils.nonsipp.check.OtherAssetsCheckStatusUtils.{checkOtherAssetsRecord, checkOtherAssetsSection}
import org.scalatest.OptionValues
import models._
import pages.nonsipp.common._
import models.IdentitySubject._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec

class OtherAssetsCheckStatusUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  private val otherAssetsHeldTrueLegacy = defaultUserAnswers.unsafeSet(OtherAssetsHeldPage(srn), true)
  private val otherAssetsHeldFalseLegacy = defaultUserAnswers.unsafeSet(OtherAssetsHeldPage(srn), false)

  private def addOtherAssetsBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
      .unsafeSet(CostOfOtherAssetPage(srn, index), money)

  // Branching on WhyDoesSchemeHoldAssetsPage
  private def addOtherAssetsAcquisitionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
      .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
      .unsafeSet(IndependentValuationPage(srn, index), true)

  private def addOtherAssetsContributionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Contribution)
      .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
      .unsafeSet(IndependentValuationPage(srn, index), true)

  private def addOtherAssetsTransferAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Transfer)

  // Branching on IdentityTypePage
  private def addOtherAssetsIndividualAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, OtherAssetSeller), Individual)
      .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), name)
      .unsafeSet(OtherAssetIndividualSellerNINumberPage(srn, index), conditionalYesNoNino)

  private def addOtherAssetsUKCompanyAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, OtherAssetSeller), UKCompany)
      .unsafeSet(CompanyNameOfOtherAssetSellerPage(srn, index), name)
      .unsafeSet(CompanyRecipientCrnPage(srn, index, OtherAssetSeller), conditionalYesNoCrn)

  private def addOtherAssetsUKPartnershipAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, OtherAssetSeller), UKPartnership)
      .unsafeSet(PartnershipOtherAssetSellerNamePage(srn, index), name)
      .unsafeSet(PartnershipRecipientUtrPage(srn, index, OtherAssetSeller), conditionalYesNoUtr)

  private def addOtherAssetsOtherAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, OtherAssetSeller), Other)
      .unsafeSet(OtherRecipientDetailsPage(srn, index, OtherAssetSeller), otherRecipientDetails)

  private def addOtherAssetsPrePopAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), true)
      .unsafeSet(IncomeFromAssetPage(srn, index), money)

  "checkOtherAssetsSection" - {

    "must be true" - {

      "when otherAssetsHeld is Some(true) & 1 record is present, which needs checking" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsContributionAnswers(
              index1of5000,
              otherAssetsHeldTrueLegacy
            )
          )

        checkOtherAssetsSection(userAnswers, srn) mustBe true
      }

      "when otherAssetsHeld is Some(true) & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsTransferAnswers(
              index1of5000,
              addOtherAssetsBaseAnswers(
                index2of5000,
                addOtherAssetsContributionAnswers(
                  index2of5000,
                  addOtherAssetsPrePopAnswers(
                    index2of5000,
                    otherAssetsHeldTrueLegacy
                  )
                )
              )
            )
          )

        checkOtherAssetsSection(userAnswers, srn) mustBe true
      }

      "when otherAssetsHeld is None & 1 record is present, which needs checking" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsTransferAnswers(
              index1of5000,
              defaultUserAnswers
            )
          )

        checkOtherAssetsSection(userAnswers, srn) mustBe true
      }

      "when otherAssetsHeld is None & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsContributionAnswers(
              index1of5000,
              addOtherAssetsBaseAnswers(
                index2of5000,
                addOtherAssetsTransferAnswers(
                  index2of5000,
                  addOtherAssetsPrePopAnswers(
                    index2of5000,
                    defaultUserAnswers
                  )
                )
              )
            )
          )

        checkOtherAssetsSection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when otherAssetsHeld is Some(false)" in {
        val userAnswers = otherAssetsHeldFalseLegacy

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when otherAssetsHeld is Some(true) & no records are present" in {
        val userAnswers = otherAssetsHeldTrueLegacy

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when otherAssetsHeld is Some(true) & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsTransferAnswers(
              index1of5000,
              addOtherAssetsPrePopAnswers(
                index1of5000,
                otherAssetsHeldTrueLegacy
              )
            )
          )

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when otherAssetsHeld is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when otherAssetsHeld is None & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsContributionAnswers(
              index1of5000,
              addOtherAssetsPrePopAnswers(
                index1of5000,
                defaultUserAnswers
              )
            )
          )

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkOtherAssetsRecord" - {

    "must be true" - {

      "when all pre-pop-cleared answers are missing & all other answers are present" - {

        "(Acquisition & Individual)" in {
          val userAnswers =
            addOtherAssetsBaseAnswers(
              index1of5000,
              addOtherAssetsAcquisitionAnswers(
                index1of5000,
                addOtherAssetsIndividualAnswers(
                  index1of5000,
                  otherAssetsHeldTrueLegacy
                )
              )
            )

          checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & UKCompany)" in {
          val userAnswers =
            addOtherAssetsBaseAnswers(
              index1of5000,
              addOtherAssetsAcquisitionAnswers(
                index1of5000,
                addOtherAssetsUKCompanyAnswers(
                  index1of5000,
                  otherAssetsHeldTrueLegacy
                )
              )
            )

          checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & UKPartnership)" in {
          val userAnswers =
            addOtherAssetsBaseAnswers(
              index1of5000,
              addOtherAssetsAcquisitionAnswers(
                index1of5000,
                addOtherAssetsUKPartnershipAnswers(
                  index1of5000,
                  otherAssetsHeldTrueLegacy
                )
              )
            )

          checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & Other)" in {
          val userAnswers =
            addOtherAssetsBaseAnswers(
              index1of5000,
              addOtherAssetsAcquisitionAnswers(
                index1of5000,
                addOtherAssetsOtherAnswers(
                  index1of5000,
                  otherAssetsHeldTrueLegacy
                )
              )
            )

          checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Contribution)" in {
          val userAnswers =
            addOtherAssetsBaseAnswers(
              index1of5000,
              addOtherAssetsContributionAnswers(
                index1of5000,
                otherAssetsHeldTrueLegacy
              )
            )

          checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Transfer)" in {
          val userAnswers =
            addOtherAssetsBaseAnswers(
              index1of5000,
              addOtherAssetsTransferAnswers(
                index1of5000,
                otherAssetsHeldTrueLegacy
              )
            )

          checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
        }
      }

      "when some pre-pop-cleared answers are present & some are missing" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsContributionAnswers(
              index1of5000,
              otherAssetsHeldTrueLegacy
            )
          ).unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index1of5000), true)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
      }
    }

    "must be false" - {

      "when all answers are missing" in {
        val userAnswers = defaultUserAnswers

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when 1 other answer is missing" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsAcquisitionAnswers(
              index1of5000,
              addOtherAssetsIndividualAnswers(
                index1of5000,
                otherAssetsHeldTrueLegacy
              )
            )
          ).unsafeRemove(IdentityTypePage(srn, index1of5000, OtherAssetSeller))

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when all pre-pop-cleared answers are present" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsTransferAnswers(
              index1of5000,
              addOtherAssetsPrePopAnswers(
                index1of5000,
                otherAssetsHeldTrueLegacy
              )
            )
          )

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Individual answers are missing" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsAcquisitionAnswers(
              index1of5000,
              otherAssetsHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, OtherAssetSeller), Individual)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKCompany answers are missing" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsAcquisitionAnswers(
              index1of5000,
              otherAssetsHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, OtherAssetSeller), UKCompany)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKPartnership answers are missing" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsAcquisitionAnswers(
              index1of5000,
              otherAssetsHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, OtherAssetSeller), UKPartnership)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Other answer is missing" in {
        val userAnswers =
          addOtherAssetsBaseAnswers(
            index1of5000,
            addOtherAssetsAcquisitionAnswers(
              index1of5000,
              otherAssetsHeldTrueLegacy
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, OtherAssetSeller), Other)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }
  }

  private val otherAssetsHeldTrue = defaultUserAnswers.unsafeSet(OtherAssetsHeldPage(srn), true)
  private val otherAssetsHeldFalse = defaultUserAnswers.unsafeSet(OtherAssetsHeldPage(srn), false)

  private def addNonPrePopRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addOtherAssetsBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Acquisition)
      .unsafeSet(IdentityTypePage(srn, index, OtherAssetSeller), Individual)

  private def addUncheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addOtherAssetsBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Contribution)
      .unsafeSet(IdentityTypePage(srn, index, OtherAssetSeller), UKCompany)
      .unsafeSet(OtherAssetsPrePopulated(srn, index), false)

  private def addCheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addOtherAssetsBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Transfer)
      .unsafeSet(IdentityTypePage(srn, index, OtherAssetSeller), UKPartnership)
      .unsafeSet(OtherAssetsPrePopulated(srn, index), true)

  "checkOtherAssetsSectionPre-Pop" - {

    "must be true" - {

      "when schemeHadBonds is None & 1 record is present (unchecked)" in {
        val userAnswers = addUncheckedRecord(index1of5000, defaultUserAnswers)

        checkOtherAssetsSection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is Some(true) & 2 records are present (checked and unchecked)" in {
        val userAnswers = addCheckedRecord(index1of5000, addUncheckedRecord(index2of5000, otherAssetsHeldTrue))

        checkOtherAssetsSection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is Some(true) & 2 records are present (unchecked and non-pre-pop)" in {
        val userAnswers = addUncheckedRecord(index1of5000, addNonPrePopRecord(index2of5000, otherAssetsHeldTrue))

        checkOtherAssetsSection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when schemeHadLoans is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(false) & no records are present" in {
        val userAnswers = otherAssetsHeldFalse

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & no records are present" in {
        val userAnswers = otherAssetsHeldTrue

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 1 record is present (checked)" in {
        val userAnswers = addCheckedRecord(index1of5000, otherAssetsHeldTrue)

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 1 record is present (non-pre-pop)" in {
        val userAnswers = addNonPrePopRecord(index1of5000, otherAssetsHeldTrue)

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 2 records are present (checked and non-pre-pop)" in {
        val userAnswers = addCheckedRecord(index1of5000, addNonPrePopRecord(index2of5000, otherAssetsHeldTrue))

        checkOtherAssetsSection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkLoansRecord" - {

    "must be true" - {

      "when record is (unchecked)" in {
        val userAnswers = addUncheckedRecord(index1of5000, defaultUserAnswers)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe true
      }
    }

    "must be false" - {

      "when record is (checked)" in {
        val userAnswers = addCheckedRecord(index1of5000, defaultUserAnswers)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when record is (non-pre-pop)" in {
        val userAnswers = addNonPrePopRecord(index1of5000, defaultUserAnswers)

        checkOtherAssetsRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }

  }

}
