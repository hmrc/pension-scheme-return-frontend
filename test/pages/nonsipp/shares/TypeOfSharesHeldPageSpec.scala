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

package pages.nonsipp.shares

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.{SchemeHoldShare, TypeOfShares, UserAnswers}
import pages.behaviours.PageBehaviours

class TypeOfSharesHeldPageSpec extends PageBehaviours {

  "DidSchemeHoldAnySharesPage" - {

    val srn = srnGen.sample.value
    val localDate = localDateGen.sample.value
    val index = refineMV[OneTo5000](1)

    beRetrievable[TypeOfShares](TypeOfSharesHeldPage(srn, index))
    beSettable[TypeOfShares](TypeOfSharesHeldPage(srn, index))
    beRemovable[TypeOfShares](TypeOfSharesHeldPage(srn, index))

    "cleanup" - {
      val userAnswers =
        UserAnswers("id")
          .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer)
          .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
          .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)

      "do not remove check return dates value when TypeOfSharesHeldPage remains the same" in {
        val updatedAnswers =
          userAnswers.set(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer).toOption.value
        updatedAnswers.get(WhyDoesSchemeHoldSharesPage(srn, index)) mustBe Some(SchemeHoldShare.Acquisition)
        updatedAnswers.get(WhenDidSchemeAcquireSharesPage(srn, index)) mustBe Some(localDate)
      }
    }
  }
}
