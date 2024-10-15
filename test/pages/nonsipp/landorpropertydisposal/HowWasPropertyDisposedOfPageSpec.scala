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

package pages.nonsipp.landorpropertydisposal

import models.HowDisposed.HowDisposed
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.{HowDisposed, IdentityType}
import viewmodels.models.SectionCompleted
import pages.behaviours.PageBehaviours
import config.RefinedTypes.{Max50, Max5000}
import controllers.TestValues

class HowWasPropertyDisposedOfPageSpec extends PageBehaviours with TestValues {
  private val landOrPropertyIndexOne = refineMV[Max5000.Refined](1)
  private val landOrPropertyIndexTwo = refineMV[Max5000.Refined](2)
  private val indexOne = refineMV[Max50.Refined](1)
  private val indexTwo = refineMV[Max50.Refined](2)

  "HowWasPropertyDisposedOfPage" - {

    val index = refineMV[Max5000.Refined](1)
    val disposalIndex = refineMV[Max50.Refined](1)
    val srnSample = srnGen.sample.value

    beRetrievable[HowDisposed](HowWasPropertyDisposedOfPage(srnSample, index, disposalIndex))

    beSettable[HowDisposed](HowWasPropertyDisposedOfPage(srnSample, index, disposalIndex))

    beRemovable[HowDisposed](HowWasPropertyDisposedOfPage(srnSample, index, disposalIndex))

    "cleanup other fields when removing the last disposal of all properties" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexOne), HowDisposed.Sold)
        .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndexOne, indexOne), IdentityType.Individual)
        .unsafeSet(LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndexOne, indexOne), individualName)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexOne), true)
        .unsafeSet(RemoveLandPropertyDisposalPage(srn, landOrPropertyIndexOne, disposalIndex), true)
        .unsafeSet(WhenWasPropertySoldPage(srn, landOrPropertyIndexOne, disposalIndex), localDate)
        .unsafeSet(DisposalIndependentValuationPage(srn, landOrPropertyIndexOne, disposalIndex), true)
        .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndexOne, disposalIndex), money)
        .unsafeSet(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexOne), SectionCompleted)

      val result = userAnswers.remove(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexOne)).success.value

      result.get(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)
      result.get(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)
      result.get(LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)

      result.get(RemoveLandPropertyDisposalPage(srn, landOrPropertyIndexOne, disposalIndex)) must be(empty)
      result.get(WhenWasPropertySoldPage(srn, landOrPropertyIndexOne, disposalIndex)) must be(empty)
      result.get(DisposalIndependentValuationPage(srn, landOrPropertyIndexOne, disposalIndex)) must be(empty)
      result.get(TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndexOne, disposalIndex)) must be(empty)
      result.get(RemoveLandPropertyDisposalPage(srn, landOrPropertyIndexOne, disposalIndex)) must be(empty)

      result.get(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)
      result.get(LandOrPropertyDisposalPage(srn)) must be(empty)

    }

    "cleanup other fields when removing a disposal but there are disposals for the same property" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexOne), HowDisposed.Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexOne), true)
        .unsafeSet(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexOne), SectionCompleted)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexTwo), HowDisposed.Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexTwo), true)
        .unsafeSet(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexTwo), SectionCompleted)

      val result = userAnswers.remove(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexOne)).success.value

      result.get(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)
      result.get(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)
      result.get(LandOrPropertyDisposalPage(srn)) must not be empty
    }

    "cleanup other fields when removing a disposal but there are disposals for other properties" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexOne), HowDisposed.Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexOne), true)
        .unsafeSet(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexOne), SectionCompleted)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexTwo), HowDisposed.Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexTwo), true)
        .unsafeSet(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexTwo), SectionCompleted)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexTwo, indexOne), HowDisposed.Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexTwo, indexOne), true)
        .unsafeSet(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexTwo, indexOne), SectionCompleted)

      val result = userAnswers.remove(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndexOne, indexOne)).success.value

      result.get(LandOrPropertyStillHeldPage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)
      result.get(LandPropertyDisposalCompletedPage(srn, landOrPropertyIndexOne, indexOne)) must be(empty)
      result.get(LandOrPropertyDisposalPage(srn)) must not be empty
    }
  }

}
