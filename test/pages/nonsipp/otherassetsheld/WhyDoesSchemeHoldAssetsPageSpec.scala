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

package pages.nonsipp.otherassetsheld

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import models.SchemeHoldAsset
import pages.behaviours.PageBehaviours

class WhyDoesSchemeHoldAssetsPageSpec extends PageBehaviours {

  "WhyDoesSchemeHoldAssetsPage" - {

    val index = refineMV[OneTo5000](1)
    val srn = srnGen.sample.value

    beRetrievable[SchemeHoldAsset](WhyDoesSchemeHoldAssetsPage(srn, index))

    beSettable[SchemeHoldAsset](WhyDoesSchemeHoldAssetsPage(srn, index))

    beRemovable[SchemeHoldAsset](WhyDoesSchemeHoldAssetsPage(srn, index))

    /*
    TODO Uncomment and adjust cleanup for subsequent pages when created
    "cleanup" - {
      val localDate: LocalDate = LocalDate.of(1989, 10, 6)

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Acquisition)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)

      s"remove dependant values when current answer is different than the existing answer" in {
        val result =
          WhyDoesSchemeHoldAssetsPage(srn, index).cleanup(Some(SchemeHoldAsset.Transfer), userAnswers).toOption.value
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) mustBe None
      }

      s"retain dependant values when current answer is the same as the existing answer" in {
        val result =
          WhyDoesSchemeHoldAssetsPage(srn, index).cleanup(Some(SchemeHoldAsset.Acquisition), userAnswers).toOption.value
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must not be None
      }
    }*/
  }
}
