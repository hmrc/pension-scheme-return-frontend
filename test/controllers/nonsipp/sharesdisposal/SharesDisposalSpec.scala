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

package controllers.nonsipp.sharesdisposal

import play.api.mvc.Call
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.SharesDisposalProgress
import viewmodels.models.SectionJourneyStatus

class SharesDisposalSpec extends ControllerBaseSpec with ControllerBehaviours {

  // Test data setup
  private val index = 1
  private val secondaryIndex = 1
  private val userAnswers = emptyUserAnswers

  // URLs for testing CallOps
  private val cyaUrl = s"/pension-scheme-return/${srn.value}/check-answers-share-disposal/"
  private val changeUrl = s"/pension-scheme-return/${srn.value}/change-share-disposal/"
  private val nonCyaUrl = s"/pension-scheme-return/${srn.value}/some-other-share-disposal-page"
  private val urlWithIndices = s"/pension-scheme-return/${srn.value}/check-answers-share-disposal/1/1"
  private val urlMissingTrailingSlash = s"/pension-scheme-return/${srn.value}/check-answers-share-disposal"

  // Call objects for testing
  private val cyaPageCall = Call("GET", cyaUrl)
  private val changePageCall = Call("GET", changeUrl)
  private val nonCyaPageCall = Call("GET", nonCyaUrl)
  private val urlWithIndicesCall = Call("GET", urlWithIndices)
  private val urlMissingTrailingSlashCall = Call("GET", urlMissingTrailingSlash)

  "CallOps" - {

    "isCyaPage" - {

      "must return true for a standard shares disposal CYA page URL" in {
        cyaPageCall.isCyaPage mustBe true
      }

      "must return true for a change shares disposal page URL" in {
        changePageCall.isCyaPage mustBe true
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
        val result = saveProgress(srn, index, secondaryIndex, userAnswers, cyaPageCall)
        val updatedUserAnswers = result.futureValue
        updatedUserAnswers.get(SharesDisposalProgress(srn, index, secondaryIndex)) mustBe Some(
          SectionJourneyStatus.Completed
        )
      }
    }

    "when the next page is not a CYA page" - {
      "and alwaysCompleted is false (default)" - {
        "must set the progress to InProgress" in {
          val result = saveProgress(srn, index, secondaryIndex, userAnswers, nonCyaPageCall)
          val updatedUserAnswers = result.futureValue
          updatedUserAnswers.get(SharesDisposalProgress(srn, index, secondaryIndex)) mustBe Some(
            SectionJourneyStatus.InProgress(nonCyaPageCall)
          )
        }
      }

      "and alwaysCompleted is true" - {
        "must set the progress to Completed" in {
          val result = saveProgress(srn, index, secondaryIndex, userAnswers, nonCyaPageCall, alwaysCompleted = true)
          val updatedUserAnswers = result.futureValue
          updatedUserAnswers.get(SharesDisposalProgress(srn, index, secondaryIndex)) mustBe Some(
            SectionJourneyStatus.Completed
          )
        }
      }
    }
  }
}
