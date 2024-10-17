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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import eu.timepit.refined.refineMV
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import utils.UserAnswersUtils.UserAnswersOps
import models.{NormalMode, UserAnswers}
import config.RefinedTypes._
import controllers.TestValues
import utils.nonsipp.ValidationUtils._
import pages.nonsipp.CheckReturnDatesPage
import org.scalatest.OptionValues

class ValidationUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  // Set test values
  private val name: String = "name"
  private val reason: String = "reason"
  private val index1of3: Max3 = refineMV(1)
  private val index1of5: Max5 = refineMV(1)
  private val index1of50: Max50 = refineMV(1)
  private val index1of300: Max300 = refineMV(1)
  private val index1of5000: Max5000 = refineMV(1)

  "TODO validateAllSections" - {

    "must be true when no necessary answers are missing" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      validateAllSections(userAnswers, srn) mustBe true
    }

    "must be false when some necessary answers are missing" in {
      val userAnswers = defaultUserAnswers

      validateAllSections(userAnswers, srn) mustBe false
    }
  }

  "validateBasicDetailsSection" - {
    "must be true for valid path A" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = validateBasicDetailsSection(userAnswers, srn)

      result mustBe true
    }

    "must be true for valid path B" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = validateBasicDetailsSection(userAnswers, srn)

      result mustBe true
    }

    "must be true for valid path C" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = validateBasicDetailsSection(userAnswers, srn)

      result mustBe true
    }

    "must be true for valid path D" in {
      val userAnswers: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = validateBasicDetailsSection(userAnswers, srn)

      result mustBe true
    }

    "must be false for invalid path - no answers" in {
      val userAnswersA: UserAnswers = defaultUserAnswers

      val result: Boolean = validateBasicDetailsSection(userAnswersA, srn)

      result mustBe false
    }

    "must be false for invalid path - single necessary answer missing" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), false)
        //.unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = validateBasicDetailsSection(userAnswersA, srn)

      result mustBe false
    }

    "must be false for invalid path - multiple necessary answers missing" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
      //.unsafeSet(CheckReturnDatesPage(srn), false)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), false)
        //.unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = validateBasicDetailsSection(userAnswersA, srn)

      result mustBe false
    }

    "must be false for invalid path - no necessary answers missing but invalid combination" in {
      val userAnswersA: UserAnswers = defaultUserAnswers
        .unsafeSet(CheckReturnDatesPage(srn), true)
        .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), dateRange)
        .unsafeSet(ActiveBankAccountPage(srn), true)
        .unsafeSet(WhyNoBankAccountPage(srn), reason)
        .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)

      val result: Boolean = validateBasicDetailsSection(userAnswersA, srn)

      result mustBe false
    }
  }

  "TODO validateLandOrPropertySection" - {
    val userAnswers = defaultUserAnswers

    validateLandOrPropertySection(userAnswers, srn) mustBe true
  }

  "TODO validateLandOrPropertyJourney" - {
    val userAnswers = defaultUserAnswers

    validateLandOrPropertyJourney(userAnswers, srn, index1of5000) mustBe true
  }

  "TODO validateLandOrPropertyDisposalJourney" - {
    val userAnswers = defaultUserAnswers

    validateLandOrPropertyDisposalJourney(userAnswers, srn, index1of5000, index1of50) mustBe true
  }
}
