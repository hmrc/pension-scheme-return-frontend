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

import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import models.{PensionCommencementLumpSum, UserAnswers}
import pages.behaviours.PageBehaviours

class PensionCommencementLumpSumPageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "PensionCommencementLumpSumPage" - {

    beRetrievable[Boolean](PensionCommencementLumpSumPage(srn))

    beSettable[Boolean](PensionCommencementLumpSumPage(srn))

    beRemovable[Boolean](PensionCommencementLumpSumPage(srn))

    "cleanup" - {

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(PensionCommencementLumpSumPage(srn), true)
          .unsafeSet(
            PensionCommencementLumpSumAmountPage(srn, 1),
            PensionCommencementLumpSum(moneyGen.sample.value, moneyGen.sample.value)
          )
          .unsafeSet(
            PensionCommencementLumpSumAmountPage(srn, 2),
            PensionCommencementLumpSum(moneyGen.sample.value, moneyGen.sample.value)
          )

      List(Some(true), None).foreach { answer =>
        s"retain pcls amount values when answer is $answer" in {

          val result = PensionCommencementLumpSumPage(srn).cleanup(answer, userAnswers).toOption.value

          result.get(PensionCommencementLumpSumAmountPage(srn, 1)) must not be None
          result.get(PensionCommencementLumpSumAmountPage(srn, 2)) must not be None
        }
      }

      "remove all pcl amount values when answer is Some(false)" in {

        val result = PensionCommencementLumpSumPage(srn).cleanup(Some(false), userAnswers).toOption.value

        result.get(PensionCommencementLumpSumAmountPage(srn, 1)) mustBe None
        result.get(PensionCommencementLumpSumAmountPage(srn, 2)) mustBe None
      }
    }
  }
}
