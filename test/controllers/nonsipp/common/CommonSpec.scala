/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.nonsipp.common

import pages.nonsipp.otherassetsheld.OtherAssetsProgress
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import models.{IdentitySubject, UserAnswers}
import pages.nonsipp.loansmadeoroutstanding.LoansProgress
import viewmodels.models.SectionJourneyStatus
import pages.nonsipp.shares.SharesProgress
import play.api.mvc.Call
import utils.IntUtils.given
import pages.nonsipp.landorproperty.LandOrPropertyProgress

class CommonSpec extends ControllerBaseSpec with ControllerBehaviours {

  // Test data setup
  private val index = 1
  private val userAnswers = emptyUserAnswers
  private val nonCyaPageCall = Call("GET", "/some/random/page")

  // CYA calls for each specific journey to test the "Completed" status
  private val cyaOtherAssetsCall = Call("GET", s"/pension-scheme-return/${srn.value}/check-answers-other-assets")
  private val cyaLoansCall = Call("GET", s"/pension-scheme-return/${srn.value}/check-answers-loans")
  private val cyaSharesCall = Call("GET", s"/pension-scheme-return/${srn.value}/check-answers-shares")

  "saveProgress" - {

    "when subject is OtherAssetSeller" - {
      val subject = IdentitySubject.OtherAssetSeller

      "must delegate to otherassetsheld.saveProgress and set status to InProgress" in {
        val result = saveProgress(srn, index, userAnswers, nonCyaPageCall, subject)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(OtherAssetsProgress(srn, index)) mustBe Some(
          SectionJourneyStatus.InProgress(nonCyaPageCall)
        )
      }

      "must delegate to otherassetsheld.saveProgress and set status to Completed" in {
        val result = saveProgress(srn, index, userAnswers, cyaOtherAssetsCall, subject)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(OtherAssetsProgress(srn, index)) mustBe Some(SectionJourneyStatus.Completed)
      }
    }

    "when subject is LoanRecipient" - {
      val subject = IdentitySubject.LoanRecipient

      "must delegate to loansmadeoroutstanding.saveProgress and set status to InProgress" in {
        val result = saveProgress(srn, index, userAnswers, nonCyaPageCall, subject)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(LoansProgress(srn, index)) mustBe Some(SectionJourneyStatus.InProgress(nonCyaPageCall))
      }

      "must delegate to loansmadeoroutstanding.saveProgress and set status to Completed" in {
        val result = saveProgress(srn, index, userAnswers, cyaLoansCall, subject)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(LoansProgress(srn, index)) mustBe Some(SectionJourneyStatus.Completed)
      }
    }

    "when subject is LandOrPropertySeller" - {
      val subject = IdentitySubject.LandOrPropertySeller

      "must delegate to landorproperty.saveProgress and set status to InProgress" in {
        val result = saveProgress(srn, index, userAnswers, nonCyaPageCall, subject)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(LandOrPropertyProgress(srn, index)) mustBe Some(
          SectionJourneyStatus.InProgress(nonCyaPageCall)
        )
      }
    }

    "when subject is SharesSeller" - {
      val subject = IdentitySubject.SharesSeller

      "must delegate to shares.saveProgress and set status to InProgress" in {
        val result = saveProgress(srn, index, userAnswers, nonCyaPageCall, subject)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(SharesProgress(srn, index)) mustBe Some(SectionJourneyStatus.InProgress(nonCyaPageCall))
      }

      "must delegate to shares.saveProgress and set status to Completed" in {
        val result = saveProgress(srn, index, userAnswers, cyaSharesCall, subject)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(SharesProgress(srn, index)) mustBe Some(SectionJourneyStatus.Completed)
      }

      "must respect the alwaysCompleted flag" in {
        val result = saveProgress(srn, index, userAnswers, nonCyaPageCall, subject, alwaysCompleted = true)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(SharesProgress(srn, index)) mustBe Some(SectionJourneyStatus.Completed)
      }
    }

    "when subject is not handled" - {
      "must return the original user answers unchanged" in {
        // Use a subject that is not in the match case
        val unhandledSubject = IdentitySubject.Unknown
        val result: UserAnswers = saveProgress(srn, index, userAnswers, nonCyaPageCall, unhandledSubject).futureValue

        result mustBe userAnswers
        // Verify no progress pages have been set
        result.get(SharesProgress(srn, index)) mustBe None
        result.get(LoansProgress(srn, index)) mustBe None
        result.get(OtherAssetsProgress(srn, index)) mustBe None
        result.get(LandOrPropertyProgress(srn, index)) mustBe None
      }
    }
  }
}
