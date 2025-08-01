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

package pages.nonsipp.bondsdisposal

import controllers.TestValues
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import viewmodels.models.SectionJourneyStatus.Completed
import pages.behaviours.PageBehaviours
import models.PointOfEntry._
import models.HowDisposed._

class HowWereBondsDisposedOfPageSpec extends PageBehaviours with TestValues {

  "HowWereBondsDisposedOfPage" - {

    val srn = srnGen.sample.value
    val bondIndexOne = 1
    val bondIndexTwo = 2
    val disposalIndexOne = 1
    val disposalIndexTwo = 2

    beRetrievable[HowDisposed](HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne))

    beSettable[HowDisposed](HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne))

    beRemovable[HowDisposed](HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne))

    "cleanup other fields when removing the last disposal of all bonds" in {
      // This test covers case (true, true) in the HowWereBondsDisposedOfPage.pages method
      val userAnswers = defaultUserAnswers
        .unsafeSet(BondsDisposalPage(srn), true)
        .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), Sold)
        .unsafeSet(WhenWereBondsSoldPage(srn, bondIndexOne, disposalIndexOne), localDate)
        .unsafeSet(TotalConsiderationSaleBondsPage(srn, bondIndexOne, disposalIndexOne), money)
        .unsafeSet(BuyerNamePage(srn, bondIndexOne, disposalIndexOne), buyerName)
        .unsafeSet(IsBuyerConnectedPartyPage(srn, bondIndexOne, disposalIndexOne), true)
        .unsafeSet(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne), bondsStillHeld)
        .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne), NoPointOfEntry)
        .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne), Completed)

      val result = userAnswers.remove(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne)).success.value

      result.get(BondsDisposalPage(srn)) must be(empty)
      result.get(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(WhenWereBondsSoldPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(TotalConsiderationSaleBondsPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BuyerNamePage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(IsBuyerConnectedPartyPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne)) must be(empty)
    }

    "cleanup other fields when removing a disposal but there are disposals of the same bond" in {
      // This test covers case (true, false) in the HowWereBondsDisposedOfPage.pages method
      val userAnswers = defaultUserAnswers
        .unsafeSet(BondsDisposalPage(srn), true)
        // Disposal 1
        .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), Transferred)
        .unsafeSet(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne), bondsStillHeld)
        .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne), NoPointOfEntry)
        .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne), Completed)
        // Disposal 2
        .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexTwo), Other(otherDetails))
        .unsafeSet(BondsStillHeldPage(srn, bondIndexOne, disposalIndexTwo), bondsStillHeld)
        .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexTwo), NoPointOfEntry)
        .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexTwo), Completed)

      val result = userAnswers.remove(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne)).success.value

      result.get(BondsDisposalPage(srn)) must be(Some(true))
      // Disposal 1 - cleaned up
      result.get(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      // Disposal 2 - retained
      result.get(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexTwo)) must be(Some(Other(otherDetails)))
      result.get(BondsStillHeldPage(srn, bondIndexOne, disposalIndexTwo)) must be(Some(bondsStillHeld))
      result.get(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexTwo)) must be(Some(NoPointOfEntry))
      result.get(BondsDisposalProgress(srn, bondIndexOne, disposalIndexTwo)) must be(Some(Completed))
    }

    "cleanup other fields when removing a disposal but there are disposals for other bonds" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(BondsDisposalPage(srn), true)
        // Disposal for Bond 1
        .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), Transferred)
        .unsafeSet(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne), bondsStillHeld)
        .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne), NoPointOfEntry)
        .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne), Completed)
        // Disposal for Bond 2
        .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexTwo, disposalIndexOne), Other(otherDetails))
        .unsafeSet(BondsStillHeldPage(srn, bondIndexTwo, disposalIndexOne), bondsStillHeld)
        .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndexTwo, disposalIndexOne), NoPointOfEntry)
        .unsafeSet(BondsDisposalProgress(srn, bondIndexTwo, disposalIndexOne), Completed)

      val result = userAnswers.remove(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne)).success.value

      result.get(BondsDisposalPage(srn)) must be(Some(true))
      // Disposal for Bond 1
      result.get(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      // Disposal for Bond 2
      result.get(HowWereBondsDisposedOfPage(srn, bondIndexTwo, disposalIndexOne)) must be(Some(Other(otherDetails)))
      result.get(BondsStillHeldPage(srn, bondIndexTwo, disposalIndexOne)) must be(Some(bondsStillHeld))
      result.get(BondsDisposalCYAPointOfEntry(srn, bondIndexTwo, disposalIndexOne)) must be(Some(NoPointOfEntry))
      result.get(BondsDisposalProgress(srn, bondIndexTwo, disposalIndexOne)) must be(Some(Completed))
    }

    "cleanup relevant fields when changing the answer on this page in CheckMode" in {
      // This test covers case (_, _) in the HowWereBondsDisposedOfPage.pages method
      val userAnswers = defaultUserAnswers
        .unsafeSet(BondsDisposalPage(srn), true)
        .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), Sold)
        .unsafeSet(WhenWereBondsSoldPage(srn, bondIndexOne, disposalIndexOne), localDate)
        .unsafeSet(TotalConsiderationSaleBondsPage(srn, bondIndexOne, disposalIndexOne), money)
        .unsafeSet(BuyerNamePage(srn, bondIndexOne, disposalIndexOne), buyerName)
        .unsafeSet(IsBuyerConnectedPartyPage(srn, bondIndexOne, disposalIndexOne), true)
        .unsafeSet(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne), bondsStillHeld)
        .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne), NoPointOfEntry)
        .unsafeSet(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne), Completed)

      val result = userAnswers
        .set(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne), Transferred)
        .success
        .value

      // Cleaned up fields
      result.get(WhenWereBondsSoldPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(TotalConsiderationSaleBondsPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(BuyerNamePage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      result.get(IsBuyerConnectedPartyPage(srn, bondIndexOne, disposalIndexOne)) must be(empty)
      // Retained fields
      result.get(BondsDisposalPage(srn)) must be(Some(true))
      result.get(HowWereBondsDisposedOfPage(srn, bondIndexOne, disposalIndexOne)) must be(Some(Transferred))
      result.get(BondsStillHeldPage(srn, bondIndexOne, disposalIndexOne)) must be(Some(bondsStillHeld))
      result.get(BondsDisposalCYAPointOfEntry(srn, bondIndexOne, disposalIndexOne)) must be(Some(NoPointOfEntry))
      result.get(BondsDisposalProgress(srn, bondIndexOne, disposalIndexOne)) must be(Some(Completed))
    }
  }
}
