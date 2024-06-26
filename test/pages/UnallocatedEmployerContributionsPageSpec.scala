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

package pages

import utils.UserAnswersUtils.UserAnswersOps
import models.UserAnswers
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import pages.behaviours.PageBehaviours

class UnallocatedEmployerContributionsPageSpec extends PageBehaviours {

  "UnallocatedEmployerContributionsPage" - {

    val srn = srnGen.sample.value

    beRetrievable[Boolean](UnallocatedEmployerContributionsPage(srn))

    beSettable[Boolean](UnallocatedEmployerContributionsPage(srn))

    beRemovable[Boolean](UnallocatedEmployerContributionsPage(srn))

    "cleanup" - {

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
          .unsafeSet(UnallocatedEmployerAmountPage(srn), moneyGen.sample.value)

      List(Some(true), None).foreach { answer =>
        s"retain unallocated amount value when answer is $answer" in {

          val result = UnallocatedEmployerContributionsPage(srn).cleanup(answer, userAnswers).toOption.value

          result.get(UnallocatedEmployerAmountPage(srn)) must not be None
        }
      }

      "remove unallocated amount value when answer is Some(false)" in {

        val result = UnallocatedEmployerContributionsPage(srn).cleanup(Some(false), userAnswers).toOption.value

        result.get(UnallocatedEmployerAmountPage(srn)) mustBe None
      }
    }

  }
}
