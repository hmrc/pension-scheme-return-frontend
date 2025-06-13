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

package pages.nonsipp.otherassetsdisposal

import models.HowDisposed._
import controllers.TestValues
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import models.{HowDisposed, IdentityType}
import viewmodels.models.SectionJourneyStatus
import pages.behaviours.PageBehaviours

class HowWasAssetDisposedOfPageSpec extends PageBehaviours with TestValues {

  private val assetIndexOne = 1
  private val assetIndexTwo = 2
  private val indexOne = 1
  private val indexTwo = 2

  "HowWasAssetDisposedOfPage" - {

    val srn = srnGen.sample.value
    val assetIndex = 1
    val disposalIndex = 1

    beRetrievable[HowDisposed](HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex))

    beSettable[HowDisposed](HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex))

    beRemovable[HowDisposed](HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex))

    "cleanup other fields when removing the last disposal of all assets" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(OtherAssetsDisposalPage(srn), true)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexOne), HowDisposed.Sold)
        .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndexOne, indexOne), IdentityType.Individual)
        .unsafeSet(IndividualNameOfAssetBuyerPage(srn, assetIndexOne, indexOne), individualName)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexOne), true)
        .unsafeSet(WhenWasAssetSoldPage(srn, assetIndexOne, indexOne), localDate)
        .unsafeSet(AssetSaleIndependentValuationPage(srn, assetIndexOne, indexOne), true)
        .unsafeSet(TotalConsiderationSaleAssetPage(srn, assetIndexOne, indexOne), money)
        .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndexOne, indexOne), SectionJourneyStatus.Completed)

      val result = userAnswers.remove(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexOne)).success.value

      result.get(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexOne)) must be(empty)
      result.get(TypeOfAssetBuyerPage(srn, assetIndexOne, indexOne)) must be(empty)
      result.get(IndividualNameOfAssetBuyerPage(srn, assetIndexOne, indexOne)) must be(empty)

      result.get(WhenWasAssetSoldPage(srn, assetIndexOne, disposalIndex)) must be(empty)
      result.get(AssetSaleIndependentValuationPage(srn, assetIndexOne, disposalIndex)) must be(empty)
      result.get(TotalConsiderationSaleAssetPage(srn, assetIndexOne, disposalIndex)) must be(empty)

      result.get(OtherAssetsDisposalProgress(srn, assetIndexOne, indexOne)) must be(empty)
      result.get(OtherAssetsDisposalPage(srn)) must be(empty)
    }

    "cleanup other fields when removing a disposal but there are disposals for the same assets" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(OtherAssetsDisposalPage(srn), true)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexOne), HowDisposed.Sold)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexOne), true)
        .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndexOne, indexOne), SectionJourneyStatus.Completed)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexTwo), HowDisposed.Sold)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexTwo), true)
        .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndexOne, indexTwo), SectionJourneyStatus.Completed)

      val result = userAnswers.remove(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexOne)).success.value

      result.get(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexOne)) must be(empty)
      result.get(OtherAssetsDisposalProgress(srn, assetIndexOne, indexOne)) must be(empty)
      result.get(OtherAssetsDisposalPage(srn)) must not be empty
    }

    "cleanup other fields when removing a disposal but there are disposals for other assets" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(OtherAssetsDisposalPage(srn), true)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexOne), HowDisposed.Sold)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexOne), true)
        .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndexOne, indexOne), SectionJourneyStatus.Completed)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexTwo), HowDisposed.Sold)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexTwo), true)
        .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndexOne, indexTwo), SectionJourneyStatus.Completed)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexTwo, indexOne), HowDisposed.Sold)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndexTwo, indexOne), true)
        .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndexTwo, indexOne), SectionJourneyStatus.Completed)

      val result = userAnswers.remove(HowWasAssetDisposedOfPage(srn, assetIndexOne, indexOne)).success.value

      result.get(AnyPartAssetStillHeldPage(srn, assetIndexOne, indexOne)) must be(empty)
      result.get(OtherAssetsDisposalProgress(srn, assetIndexOne, indexOne)) must be(empty)
      result.get(OtherAssetsDisposalPage(srn)) must not be empty
    }
  }
}
