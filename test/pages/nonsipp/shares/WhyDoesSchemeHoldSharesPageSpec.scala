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

package pages.nonsipp.shares

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import models.{IdentitySubject, IdentityType, SchemeHoldShare, UserAnswers}
import pages.behaviours.PageBehaviours
import pages.nonsipp.common
import utils.UserAnswersUtils.UserAnswersOps
import viewmodels.models.SectionCompleted

import java.time.LocalDate

class WhyDoesSchemeHoldSharesPageSpec extends PageBehaviours {

  "WhyDoesSchemeHoldSharesPage" - {

    val index = refineMV[OneTo5000](1)
    val srn = srnGen.sample.value
    val money = moneyGen.sample.value

    beRetrievable[SchemeHoldShare](WhyDoesSchemeHoldSharesPage(srn, index))

    beSettable[SchemeHoldShare](WhyDoesSchemeHoldSharesPage(srn, index))

    beRemovable[SchemeHoldShare](WhyDoesSchemeHoldSharesPage(srn, index))

    "cleanup" - {
      val localDate: LocalDate = LocalDate.of(1989, 10, 6)

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
          .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
          .unsafeSet(common.IdentityTypePage(srn, index, IdentitySubject.SharesSeller), IdentityType.Individual)
          .unsafeSet(TotalAssetValuePage(srn, index), money)
          .unsafeSet(SharesCompleted(srn, index), SectionCompleted)

      s"remove dependant values when current answer is different than the existing answer" in {
        val result =
          WhyDoesSchemeHoldSharesPage(srn, index).cleanup(Some(SchemeHoldShare.Transfer), userAnswers).toOption.value
        result.get(WhenDidSchemeAcquireSharesPage(srn, index)) mustBe None
        result.get(common.IdentityTypePage(srn, index, IdentitySubject.SharesSeller)) mustBe None
        result.get(TotalAssetValuePage(srn, index)) mustBe None
        result.get(SharesCompleted(srn, index)) mustBe None
      }

      s"retain dependant values when current answer is the same as the existing answer" in {
        val result =
          WhyDoesSchemeHoldSharesPage(srn, index).cleanup(Some(SchemeHoldShare.Acquisition), userAnswers).toOption.value
        result.get(WhenDidSchemeAcquireSharesPage(srn, index)) must not be None
        result.get(common.IdentityTypePage(srn, index, IdentitySubject.SharesSeller)) must not be None
        result.get(TotalAssetValuePage(srn, index)) must not be None
        result.get(SharesCompleted(srn, index)) must not be None
      }

      s"remove dependant values when we call Remove operation" in {
        val result =
          WhyDoesSchemeHoldSharesPage(srn, index).cleanup(None, userAnswers).toOption.value
        result.get(WhenDidSchemeAcquireSharesPage(srn, index)) mustBe None
        result.get(common.IdentityTypePage(srn, index, IdentitySubject.SharesSeller)) mustBe None
        result.get(TotalAssetValuePage(srn, index)) mustBe None
        result.get(SharesCompleted(srn, index)) mustBe None
      }
    }
  }
}
