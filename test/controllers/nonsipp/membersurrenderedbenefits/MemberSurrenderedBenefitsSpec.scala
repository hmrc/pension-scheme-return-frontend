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

package controllers.nonsipp.membersurrenderedbenefits

import play.api.mvc.Call
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import utils.IntUtils.given
import pages.nonsipp.membersurrenderedbenefits.MemberSurrenderedBenefitsProgress
import viewmodels.models.SectionJourneyStatus

class MemberSurrenderedBenefitsSpec extends ControllerBaseSpec with ControllerBehaviours {

  // Test data setup
  private val index = 1
  private val userAnswers = emptyUserAnswers

  // URLs for testing CallOps
  private val cyaUrl = s"/pension-scheme-return/${srn.value}/check-answers-surrender-benefits/"
  private val changeCyaUrl = s"/pension-scheme-return/${srn.value}/change-check-answers-surrender-benefits/"
  private val nonCyaUrl = s"/pension-scheme-return/${srn.value}/some-other-surrender-page"
  private val urlWithIndices = s"/pension-scheme-return/${srn.value}/check-answers-surrender-benefits/1/1"
  private val urlMissingTrailingSlash = s"/pension-scheme-return/${srn.value}/check-answers-surrender-benefits"

  // Call objects for testing
  private val cyaPageCall = Call("GET", cyaUrl)
  private val changeCyaPageCall = Call("GET", changeCyaUrl)
  private val nonCyaPageCall = Call("GET", nonCyaUrl)
  private val urlWithIndicesCall = Call("GET", urlWithIndices)
  private val urlMissingTrailingSlashCall = Call("GET", urlMissingTrailingSlash)

  "CallOps" - {

    "isCyaPage" - {

      "must return true for a standard surrendered benefits CYA page URL" in {
        cyaPageCall.isCyaPage mustBe true
      }

      "must return true for a change surrendered benefits CYA page URL" in {
        changeCyaPageCall.isCyaPage mustBe true
      }

      "must return true for a CYA page URL with query parameters" in {
        val callWithQuery = Call("GET", s"$cyaUrl?foo=bar")
        callWithQuery.isCyaPage mustBe true
      }

      "must return false for a non-CYA page URL" in {
        nonCyaPageCall.isCyaPage mustBe false
      }

      "must return false for a URL that is missing the required trailing slash" in {
        urlMissingTrailingSlashCall.isCyaPage mustBe false
      }

      "must return true for a URL with trailing indices because the negative lookahead is ineffective" in {
        urlWithIndicesCall.isCyaPage mustBe true
      }

      "must return false for a completely different URL structure" in {
        val completelyDifferentCall = Call("GET", "/foo/bar")
        completelyDifferentCall.isCyaPage mustBe false
      }
    }
  }

  "saveProgress" - {

    "when the next page is a CYA page" - {
      "must set the progress to Completed" in {
        val result = saveProgress(srn, index, userAnswers, cyaPageCall)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(MemberSurrenderedBenefitsProgress(srn, index)) mustBe Some(
          SectionJourneyStatus.Completed
        )
      }
    }

    "when the next page is not a CYA page" - {
      "and alwaysCompleted is false (default)" - {
        "must set the progress to InProgress" in {
          val result = saveProgress(srn, index, userAnswers, nonCyaPageCall)
          val updatedUserAnswers = result.futureValue
          updatedUserAnswers.get(MemberSurrenderedBenefitsProgress(srn, index)) mustBe Some(
            SectionJourneyStatus.InProgress(nonCyaPageCall)
          )
        }
      }

      "and alwaysCompleted is true" - {
        "must set the progress to Completed" in {
          val result = saveProgress(srn, index, userAnswers, nonCyaPageCall, alwaysCompleted = true)
          val updatedUserAnswers = result.futureValue
          updatedUserAnswers.get(MemberSurrenderedBenefitsProgress(srn, index)) mustBe Some(
            SectionJourneyStatus.Completed
          )
        }
      }
    }
  }
}
