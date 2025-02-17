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
import org.scalatest.matchers.must.Matchers
import utils.nonsipp.check.BondsCheckStatusUtils.{checkBondsRecord, checkBondsSection}
import org.scalatest.OptionValues
import models.{SchemeHoldBond, UserAnswers}
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec

class BondsCheckStatusUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  private val bondsHeldTrue = defaultUserAnswers.unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
  private val bondsHeldFalse = defaultUserAnswers.unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)

  private def addBondsBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(NameOfBondsPage(srn, index), name)
      .unsafeSet(CostOfBondsPage(srn, index), money)
      .unsafeSet(AreBondsUnregulatedPage(srn, index), true)

  // Branching on WhyDoesSchemeHoldBondsPage
  private def addBondsAcquisitionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)
      .unsafeSet(BondsFromConnectedPartyPage(srn, index), true)

  private def addBondsContributionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Contribution)
      .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)

  private def addBondsTransferAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Transfer)

  private def addBondsPrePopAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IncomeFromBondsPage(srn, index), money)

  "checkBondsSection" - {

    "must be true" - {

      "when unregulatedOrConnectedBondsHeld is Some(true) & 1 record is present, which needs checking" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsAcquisitionAnswers(
              index1of5000,
              bondsHeldTrue
            )
          )

        checkBondsSection(userAnswers, srn) mustBe true
      }

      "when unregulatedOrConnectedBondsHeld is Some(true) & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsContributionAnswers(
              index1of5000,
              addBondsBaseAnswers(
                index2of5000,
                addBondsTransferAnswers(
                  index2of5000,
                  addBondsPrePopAnswers(
                    index2of5000,
                    bondsHeldTrue
                  )
                )
              )
            )
          )

        checkBondsSection(userAnswers, srn) mustBe true
      }

      "when unregulatedOrConnectedBondsHeld is None & 1 record is present, which needs checking" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsAcquisitionAnswers(
              index1of5000,
              defaultUserAnswers
            )
          )

        checkBondsSection(userAnswers, srn) mustBe true
      }

      "when unregulatedOrConnectedBondsHeld is None & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsContributionAnswers(
              index1of5000,
              addBondsBaseAnswers(
                index2of5000,
                addBondsTransferAnswers(
                  index2of5000,
                  addBondsPrePopAnswers(
                    index2of5000,
                    defaultUserAnswers
                  )
                )
              )
            )
          )

        checkBondsSection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when unregulatedOrConnectedBondsHeld is Some(false)" in {
        val userAnswers = bondsHeldFalse

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when unregulatedOrConnectedBondsHeld is Some(true) & no records are present" in {
        val userAnswers = bondsHeldTrue

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when unregulatedOrConnectedBondsHeld is Some(true) & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsAcquisitionAnswers(
              index1of5000,
              addBondsPrePopAnswers(
                index1of5000,
                bondsHeldTrue
              )
            )
          )

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when unregulatedOrConnectedBondsHeld is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when unregulatedOrConnectedBondsHeld is None & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsContributionAnswers(
              index1of5000,
              addBondsPrePopAnswers(
                index1of5000,
                defaultUserAnswers
              )
            )
          )

        checkBondsSection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkBondsRecord" - {

    "must be true" - {

      "when pre-pop-cleared answer is missing & all other answers are present" - {

        "(Acquisition)" in {
          val userAnswers =
            addBondsBaseAnswers(
              index1of5000,
              addBondsAcquisitionAnswers(
                index1of5000,
                bondsHeldTrue
              )
            )

          checkBondsRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Contribution)" in {
          val userAnswers =
            addBondsBaseAnswers(
              index1of5000,
              addBondsContributionAnswers(
                index1of5000,
                bondsHeldTrue
              )
            )

          checkBondsRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Transfer)" in {
          val userAnswers =
            addBondsBaseAnswers(
              index1of5000,
              addBondsTransferAnswers(
                index1of5000,
                bondsHeldTrue
              )
            )

          checkBondsRecord(userAnswers, srn, index1of5000) mustBe true
        }
      }
    }

    "must be false" - {

      "when all answers are missing" in {
        val userAnswers = defaultUserAnswers

        checkBondsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when 1 other answer is missing" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsAcquisitionAnswers(
              index1of5000,
              bondsHeldTrue
            )
          ).unsafeRemove(WhyDoesSchemeHoldBondsPage(srn, index1of5000))

        checkBondsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when pre-pop-cleared answer is present" in {
        val userAnswers =
          addBondsBaseAnswers(
            index1of5000,
            addBondsTransferAnswers(
              index1of5000,
              addBondsPrePopAnswers(
                index1of5000,
                bondsHeldTrue
              )
            )
          )

        checkBondsRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }
  }

  private val schemeHadBondsTrue =
    defaultUserAnswers.unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)

  private val schemeHadBondsFalse =
    defaultUserAnswers.unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)

  private def addNonPrePopRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addBondsBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)

  private def addUncheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addBondsBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Contribution)
      .unsafeSet(BondPrePopulated(srn, index), false)

  private def addCheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addBondsBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Transfer)
      .unsafeSet(BondPrePopulated(srn, index), true)

  "checkBondsSectionPre-Pop" - {

    "must be true" - {

      "when schemeHadBonds is None & 1 record is present (unchecked)" in {
        val userAnswers = addUncheckedRecord(index1of5000, defaultUserAnswers)

        checkBondsSection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is Some(true) & 2 records are present (checked and unchecked)" in {
        val userAnswers = addCheckedRecord(index1of5000, addUncheckedRecord(index2of5000, schemeHadBondsTrue))

        checkBondsSection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is Some(true) & 2 records are present (unchecked and non-pre-pop)" in {
        val userAnswers = addUncheckedRecord(index1of5000, addNonPrePopRecord(index2of5000, schemeHadBondsTrue))

        checkBondsSection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when schemeHadLoans is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(false) & no records are present" in {
        val userAnswers = schemeHadBondsFalse

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & no records are present" in {
        val userAnswers = schemeHadBondsTrue

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 1 record is present (checked)" in {
        val userAnswers = addCheckedRecord(index1of5000, schemeHadBondsTrue)

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 1 record is present (non-pre-pop)" in {
        val userAnswers = addNonPrePopRecord(index1of5000, schemeHadBondsTrue)

        checkBondsSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 2 records are present (checked and non-pre-pop)" in {
        val userAnswers = addCheckedRecord(index1of5000, addNonPrePopRecord(index2of5000, schemeHadBondsTrue))

        checkBondsSection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkLoansRecord" - {

    "must be true" - {

      "when record is (unchecked)" in {
        val userAnswers = addUncheckedRecord(index1of5000, defaultUserAnswers)

        checkBondsRecord(userAnswers, srn, index1of5000) mustBe true
      }
    }

    "must be false" - {

      "when record is (checked)" in {
        val userAnswers = addCheckedRecord(index1of5000, defaultUserAnswers)

        checkBondsRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when record is (non-pre-pop)" in {
        val userAnswers = addNonPrePopRecord(index1of5000, defaultUserAnswers)

        checkBondsRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }

  }

}
