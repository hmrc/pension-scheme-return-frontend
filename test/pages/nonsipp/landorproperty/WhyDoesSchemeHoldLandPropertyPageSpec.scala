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

package pages.nonsipp.landorproperty

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models._
import pages.nonsipp.common.IdentityTypePage
import models.IdentitySubject.writes
import pages.behaviours.PageBehaviours

import java.time.LocalDate

class WhyDoesSchemeHoldLandPropertyPageSpec extends PageBehaviours {

  "WhyDoesSchemeHoldLandPropertyPage" - {

    val index = refineMV[OneTo5000](1)
    val srn = srnGen.sample.value
    val subject = IdentitySubject.LandOrPropertySeller

    beRetrievable[SchemeHoldLandProperty](WhyDoesSchemeHoldLandPropertyPage(srn, index))

    beSettable[SchemeHoldLandProperty](WhyDoesSchemeHoldLandPropertyPage(srn, index))

    beRemovable[SchemeHoldLandProperty](WhyDoesSchemeHoldLandPropertyPage(srn, index))

    "cleanup" - {
      val localDate: LocalDate = LocalDate.of(1989, 10, 6)
      val userAnswers =
        UserAnswers("id")
          .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index), localDate)
          .unsafeSet(LandPropertyIndependentValuationPage(srn, index), true)
          .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.UKCompany)

      s"remove dependant values when current answer is Transfer and existing answer is Acquisition" in {
        val answer =
          userAnswers.unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Acquisition)

        val result = WhyDoesSchemeHoldLandPropertyPage(srn, index)
          .cleanup(Some(SchemeHoldLandProperty.Transfer), answer)
          .toOption
          .value

        result.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, index)) mustBe None
        result.get(LandPropertyIndependentValuationPage(srn, index)) mustBe None
        result.get(IdentityTypePage(srn, index, subject)) mustBe None
      }

      List(SchemeHoldLandProperty.Acquisition, SchemeHoldLandProperty.Contribution, SchemeHoldLandProperty.Transfer)
        .foreach { answer =>
          s"retain dependant values when current answer is the same as existing answer: $answer" in {
            val savedAnswer = userAnswers
              .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), answer)

            val result = WhyDoesSchemeHoldLandPropertyPage(srn, index)
              .cleanup(Some(answer), savedAnswer)
              .toOption
              .value

            result.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, index)) must not be None
            result.get(LandPropertyIndependentValuationPage(srn, index)) must not be None
            result.get(IdentityTypePage(srn, index, subject)) must not be None
          }
        }
    }
  }
}
