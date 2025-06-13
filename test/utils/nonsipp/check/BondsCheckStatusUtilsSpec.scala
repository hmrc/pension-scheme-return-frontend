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
import utils.nonsipp.check.BondsCheckStatusUtils.checkBondsRecord
import org.scalatest.OptionValues
import models.{SchemeHoldBond, UserAnswers}
import config.RefinedTypes.Max5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class BondsCheckStatusUtilsSpec extends ControllerBaseSpec with ControllerBehaviours with Matchers with OptionValues {

  private def addBondsBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(NameOfBondsPage(srn, index), name)
      .unsafeSet(CostOfBondsPage(srn, index), money)
      .unsafeSet(AreBondsUnregulatedPage(srn, index), true)

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

  "checkBondsRecord" - {

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
