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

import pages.nonsipp.schemedesignatory._
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld._
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import org.scalatest.OptionValues
import generators.ModelGenerators.pensionSchemeIdGen
import models._
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.moneyborrowed._
import pages.nonsipp.bonds._
import pages.nonsipp.memberdetails._
import org.scalatest.freespec.AnyFreeSpec
import config.RefinedTypes._
import controllers.TestValues

class MemberCountUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  private val index: Max300 = refineMV(1)
  private val pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value
  private val userAnswersOverThresholdNumbers: UserAnswers = defaultUserAnswers
    .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)

  "hasMemberNumbersChangedToOver99" - {
    "should return false" - {
      "when default data" in {
        val result =
          MemberCountUtils.hasMemberNumbersChangedToOver99(defaultUserAnswers, srn, pensionSchemeId, isPrePop = false)
        result mustBe false
      }

      "when only SchemeMemberNumbers exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          defaultUserAnswers
            .unsafeSet(HowManyMembersPage(srn, psaId), SchemeMemberNumbers(1, 2, 3)),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe false
      }

      "when only SchemeMemberNumbers > 99 exist" in {
        val result =
          MemberCountUtils.hasMemberNumbersChangedToOver99(
            userAnswersOverThresholdNumbers,
            srn,
            pensionSchemeId,
            isPrePop = false
          )
        result mustBe false
      }
    }

    "should return true" - {
      "when SchemeMemberNumbers > 99 and member details exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(MemberDetailsPage(srn, index), memberDetails),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }
      "when SchemeMemberNumbers > 99 and loan details exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(LoansMadeOrOutstandingPage(srn), false),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and borrowings exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(MoneyBorrowedPage(srn), false),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and financial information exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(HowMuchCashPage(srn, NormalMode), moneyInPeriod)
            .unsafeSet(ValueOfAssetsPage(srn, NormalMode), moneyInPeriod)
            .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), money),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and shares exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(DidSchemeHoldAnySharesPage(srn), false),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and land or properties exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(LandOrPropertyHeldPage(srn), false),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and bonds exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and other assets exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(OtherAssetsHeldPage(srn), false),
          srn,
          pensionSchemeId,
          isPrePop = false
        )
        result mustBe true
      }

    }
  }
}
