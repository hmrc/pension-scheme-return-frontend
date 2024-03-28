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

package pages.nonsipp.bonds

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.{SchemeHoldBond, UserAnswers}
import pages.behaviours.PageBehaviours

import java.time.LocalDate

class WhyDoesSchemeHoldBondsPageSpec extends PageBehaviours {

  "WhyDoesSchemeHoldBondsPage" - {

    val index = refineMV[OneTo5000](1)
    val srn = srnGen.sample.value

    beRetrievable[SchemeHoldBond](WhyDoesSchemeHoldBondsPage(srn, index))

    beSettable[SchemeHoldBond](WhyDoesSchemeHoldBondsPage(srn, index))

    beRemovable[SchemeHoldBond](WhyDoesSchemeHoldBondsPage(srn, index))

    "cleanup" - {
      val localDate: LocalDate = LocalDate.of(1989, 10, 6)

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
          .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)

      s"remove dependant values when current answer is different than the existing answer" in {
        val result =
          WhyDoesSchemeHoldBondsPage(srn, index).cleanup(Some(SchemeHoldBond.Transfer), userAnswers).toOption.value
        result.get(WhenDidSchemeAcquireBondsPage(srn, index)) mustBe None
      }

      s"retain dependant values when current answer is the same as the existing answer" in {
        val result =
          WhyDoesSchemeHoldBondsPage(srn, index).cleanup(Some(SchemeHoldBond.Acquisition), userAnswers).toOption.value
        result.get(WhenDidSchemeAcquireBondsPage(srn, index)) must not be None
      }
    }
  }
}
