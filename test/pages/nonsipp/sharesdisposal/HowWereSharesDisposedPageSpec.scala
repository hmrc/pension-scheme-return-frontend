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

package pages.nonsipp.sharesdisposal

import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.HowSharesDisposed.HowSharesDisposed
import models.{HowSharesDisposed, IdentityType, PointOfEntry}
import viewmodels.models.SectionJourneyStatus
import pages.behaviours.PageBehaviours
import config.RefinedTypes.{Max50, Max5000}
import controllers.TestValues

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
      // This test covers case (true, true) in the HowWereSharesDisposedPage.pages method
      val userAnswers = defaultUserAnswers
        .unsafeSet(SharesDisposalPage(srn), true)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(HowManySharesRedeemedPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, shareIndexOne, indexOne), money)
        .unsafeSet(HowManyDisposalSharesPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(WhenWereSharesSoldPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(HowManySharesSoldPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(TotalConsiderationSharesSoldPage(srn, shareIndexOne, indexOne), money)
        .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndexOne, indexOne), IdentityType.Individual)
        .unsafeSet(SharesIndividualBuyerNamePage(srn, shareIndexOne, indexOne), buyerName)
        .unsafeSet(IsBuyerConnectedPartyPage(srn, shareIndexOne, indexOne), true)
        .unsafeSet(IndependentValuationPage(srn, shareIndexOne, indexOne), true)
        .unsafeSet(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexOne), PointOfEntry.NoPointOfEntry)
        .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, indexOne), SectionJourneyStatus.Completed)

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
      result.get(IsBuyerConnectedPartyPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(IndependentValuationPage(srn, shareIndexOne, indexOne)) must be(empty)

      result.get(HowManyDisposalSharesPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(SharesDisposalProgress(srn, shareIndexOne, indexOne)) must be(empty)
    }

    "cleanup other fields when removing a disposal but there are disposals of the same share" in {
      // This test covers case (true, false) in the HowWereSharesDisposedPage.pages method
      val userAnswers = defaultUserAnswers
        .unsafeSet(SharesDisposalPage(srn), true)
        // Disposal 1
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(HowManyDisposalSharesPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexOne), PointOfEntry.NoPointOfEntry)
        .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, indexOne), SectionJourneyStatus.Completed)
        // Disposal 2
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexTwo), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexTwo), localDate)
        .unsafeSet(HowManyDisposalSharesPage(srn, shareIndexOne, indexTwo), 1)
        .unsafeSet(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexTwo), PointOfEntry.NoPointOfEntry)
        .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, indexTwo), SectionJourneyStatus.Completed)

      val result = userAnswers.remove(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)).success.value

      result.get(SharesDisposalPage(srn)) must not be empty
      // Disposal 1
      result.get(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(HowManyDisposalSharesPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(SharesDisposalProgress(srn, shareIndexOne, indexOne)) must be(empty)
      // Disposal 2
      result.get(HowWereSharesDisposedPage(srn, shareIndexOne, indexTwo)) must not be empty
      result.get(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexTwo)) must not be empty
      result.get(HowManyDisposalSharesPage(srn, shareIndexOne, indexTwo)) must not be empty
      result.get(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexTwo)) must not be empty
      result.get(SharesDisposalProgress(srn, shareIndexOne, indexTwo)) must not be empty
    }

    "cleanup other fields when removing a disposal but there are disposals for other shares" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(SharesDisposalPage(srn), true)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, indexOne), SectionJourneyStatus.Completed)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexTwo, indexOne), HowSharesDisposed.Sold)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexTwo, indexOne), localDate)
        .unsafeSet(SharesDisposalProgress(srn, shareIndexTwo, indexOne), SectionJourneyStatus.Completed)

      val result = userAnswers.remove(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)).success.value

      result.get(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne)) must be(empty)
      result.get(HowWereSharesDisposedPage(srn, shareIndexTwo, indexOne)) must not be empty
      result.get(WhenWereSharesRedeemedPage(srn, shareIndexTwo, indexOne)) must not be empty
      result.get(SharesDisposalPage(srn)) must not be empty
      result.get(SharesDisposalProgress(srn, shareIndexTwo, indexOne)) must not be empty
    }

    "cleanup relevant fields when changing the answer on this page in CheckMode" in {
      // This test covers case (_, _) in the HowWereSharesDisposedPage.pages method
      val userAnswers = defaultUserAnswers
        .unsafeSet(SharesDisposalPage(srn), true)
        .unsafeSet(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Redeemed)
        .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne), localDate)
        .unsafeSet(HowManySharesRedeemedPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, shareIndexOne, indexOne), money)
        .unsafeSet(HowManyDisposalSharesPage(srn, shareIndexOne, indexOne), 1)
        .unsafeSet(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexOne), PointOfEntry.NoPointOfEntry)
        .unsafeSet(SharesDisposalProgress(srn, shareIndexOne, indexOne), SectionJourneyStatus.Completed)

      userAnswers.set(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne), HowSharesDisposed.Transferred)

      userAnswers.get(SharesDisposalPage(srn)) must not be empty
      userAnswers.get(HowWereSharesDisposedPage(srn, shareIndexOne, indexOne)) must not be empty
      userAnswers.get(HowManyDisposalSharesPage(srn, shareIndexOne, indexOne)) must not be empty
      userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndexOne, indexOne)) must not be empty

      userAnswers.get(WhenWereSharesRedeemedPage(srn, shareIndexOne, indexOne)) must not be empty
      userAnswers.get(HowManySharesRedeemedPage(srn, shareIndexOne, indexOne)) must not be empty
      userAnswers.get(TotalConsiderationSharesRedeemedPage(srn, shareIndexOne, indexOne)) must not be empty
      userAnswers.get(SharesDisposalProgress(srn, shareIndexOne, indexOne)) must not be empty
    }
  }
}
