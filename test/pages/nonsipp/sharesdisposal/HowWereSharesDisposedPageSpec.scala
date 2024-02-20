/*
 * Copyright 2023 HM Revenue & Customs
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

package pages.nonsipp.sharesdisposal

import config.Refined.{Max50, Max5000}
import controllers.TestValues
import eu.timepit.refined.refineMV
import models.{HowSharesDisposed, IdentityType}
import models.HowSharesDisposed.HowSharesDisposed
import pages.behaviours.PageBehaviours
import utils.UserAnswersUtils.UserAnswersOps
import viewmodels.models.SectionCompleted

class HowWereSharesDisposedPageSpec extends PageBehaviours with TestValues {
  private val shareIndexOne = refineMV[Max5000.Refined](1)
  private val shareIndexTwo = refineMV[Max5000.Refined](2)
  private val indexOne = refineMV[Max50.Refined](1)
  private val indexTwo = refineMV[Max50.Refined](2)

  "HowWereSharesDisposedPage" - {
    val srn = srnGen.sample.value
    val shareIndex = refineMV[Max5000.Refined](1)
    val disposalIndex = refineMV[Max50.Refined](1)

    beRetrievable[HowSharesDisposed](HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))

    beSettable[HowSharesDisposed](HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))

    beRemovable[HowSharesDisposed](HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))

    "cleanup other fields when removing the last disposal of all shares" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(SharesDisposalPage(srn), true)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(HowManySharesRedeemedPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, shareIndexOne, indexOne), money)
        .unsafeSet(HowManySharesPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(WhenWereSharesSoldPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(HowManySharesSoldPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(TotalConsiderationSharesSoldPage(srn, shareIndexOne, indexOne), money)
        .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndexOne, indexOne), IdentityType.Individual)
        .unsafeSet(SharesIndividualBuyerNamePage(srn, shareIndexOne, indexOne), buyerName)
        .unsafeSet(SharesDisposalBuyerConnectedPartyPage(srn, shareIndexOne, indexOne), true)
        .unsafeSet(IndependentValuationPage(srn, shareIndexOne, indexOne), true)
        .unsafeSet(SharesDisposalCompletedPage(srn, shareIndexOne, indexOne), SectionCompleted)

      val result = userAnswers.remove(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)).success.value

      result.get(SharesDisposalPage(srn)) must be(empty)

      result.get(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)) must be(empty)

      result.get(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(HowManySharesRedeemedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(TotalConsiderationSharesRedeemedPage(srn, shareIndexOne, indexOne)) must be(empty)

      result.get(WhenWereSharesSoldPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(HowManySharesSoldPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(WhoWereTheSharesSoldToPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(TotalConsiderationSharesSoldPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(SharesIndividualBuyerNamePage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(SharesDisposalBuyerConnectedPartyPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(IndependentValuationPage(srn, shareIndexOne, indexOne)) must be(empty)

      result.get(HowManySharesPage(srn, shareIndexOne, indexOne)) must be(empty)

    }

    "cleanup other fields when removing a disposal but there are disposals of the same share" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(SharesDisposalPage(srn), true)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(SharesDisposalCompletedPage(srn, shareIndexOne, indexOne), SectionCompleted)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexTwo), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexTwo), localDate)
        .unsafeSet(SharesDisposalCompletedPage(srn, shareIndexOne, indexTwo), SectionCompleted)

      val result = userAnswers.remove(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)).success.value

      result.get(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(SharesDisposalPage(srn)) must not be (empty)
    }

    "cleanup other fields when removing a disposal but there are disposals for other shares" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(SharesDisposalPage(srn), true)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(SharesDisposalCompletedPage(srn, shareIndexOne, indexOne), SectionCompleted)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexTwo, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexTwo, indexOne), localDate)
        .unsafeSet(SharesDisposalCompletedPage(srn, shareIndexTwo, indexOne), SectionCompleted)

      val result = userAnswers.remove(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)).success.value

      result.get(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(HowWereSharesDisposedPage(srn, shareIndexTwo, indexOne)) must not be (empty)
      result.get(WhenWereSharesRedeemedPage(srn, shareIndexTwo, indexOne)) must not be (empty)
      result.get(SharesDisposalPage(srn)) must not be (empty)
    }
  }
}
