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
import models.{Money, UserAnswers}
import pages.behaviours.PageBehaviours

import java.time.LocalDate

class IsLandPropertyLeasedPageSpec extends PageBehaviours {
  private val srn = srnGen.sample.value

  "IsLandPropertyLeasedPage" - {

    val index = refineMV[OneTo5000](1)

    beRetrievable[Boolean](IsLandPropertyLeasedPage(srn, index))

    beSettable[Boolean](IsLandPropertyLeasedPage(srn, index))

    beRemovable[Boolean](IsLandPropertyLeasedPage(srn, index))

    "cleanup" - {
      val leaseName = "testLeaseName"
      val money: Money = Money(123456)
      val localDate: LocalDate = LocalDate.of(1989, 10, 6)

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index), (leaseName, money, localDate))
          .unsafeSet(IsLesseeConnectedPartyPage(srn, index), true)

      s"remove dependant values when current answer is Some(false) and existing answer is true" in {
        val answer = userAnswers.unsafeSet(IsLandPropertyLeasedPage(srn, index), true)

        val result = IsLandPropertyLeasedPage(srn, index).cleanup(Some(false), answer).toOption.value

        result.get(LandOrPropertyLeaseDetailsPage(srn, index)) mustBe None
        result.get(IsLesseeConnectedPartyPage(srn, index)) mustBe None
      }

      s"retain dependant values when current answer is Some(true) and existing answer is true" in {
        val answer = userAnswers.unsafeSet(IsLandPropertyLeasedPage(srn, index), true)

        val result = IsLandPropertyLeasedPage(srn, index).cleanup(Some(true), answer).toOption.value

        result.get(LandOrPropertyLeaseDetailsPage(srn, index)) must not be None
        result.get(IsLesseeConnectedPartyPage(srn, index)) must not be None
      }

      s"retain dependant values when current answer is Some(true) and existing answer is None" in {

        val result = IsLandPropertyLeasedPage(srn, index).cleanup(Some(true), userAnswers).toOption.value

        result.get(LandOrPropertyLeaseDetailsPage(srn, index)) must not be None
        result.get(IsLesseeConnectedPartyPage(srn, index)) must not be None
      }

      List(Some(true), Some(false)).foreach { currentAnswer =>
        s"retain dependant values when current answer is $currentAnswer and existing answer is false" in {

          val answer = userAnswers.unsafeSet(IsLandPropertyLeasedPage(srn, index), false)

          val result = IsLandPropertyLeasedPage(srn, index).cleanup(currentAnswer, answer).toOption.value

          result.get(LandOrPropertyLeaseDetailsPage(srn, index)) must not be None
          result.get(IsLesseeConnectedPartyPage(srn, index)) must not be None
        }
      }

      List(Some(true), Some(false), None).foreach { existingAnswer =>
        s"remove dependant values when current answer is None and existing answer is $existingAnswer" in {

          val answer = userAnswers.unsafeSet(IsLandPropertyLeasedPage(srn, index), true)

          val result = IsLandPropertyLeasedPage(srn, index).cleanup(None, answer).toOption.value

          result.get(LandOrPropertyLeaseDetailsPage(srn, index)) mustBe None
          result.get(IsLesseeConnectedPartyPage(srn, index)) mustBe None
        }
      }
    }
  }
}
