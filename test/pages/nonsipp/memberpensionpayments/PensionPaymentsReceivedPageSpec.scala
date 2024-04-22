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

package pages.nonsipp.memberpensionpayments

import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.UserAnswers
import pages.behaviours.PageBehaviours

class PensionPaymentsReceivedPageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "PensionPaymentsReceivedPage" - {

    beRetrievable[Boolean](PensionPaymentsReceivedPage(srn))

    beSettable[Boolean](PensionPaymentsReceivedPage(srn))

    beRemovable[Boolean](PensionPaymentsReceivedPage(srn))

    "cleanup" - {

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(PensionPaymentsReceivedPage(srn), true)
          .unsafeSet(TotalAmountPensionPaymentsPage(srn, refineMV(1)), moneyGen.sample.value)
          .unsafeSet(TotalAmountPensionPaymentsPage(srn, refineMV(2)), moneyGen.sample.value)

      List(Some(true), None).foreach { answer =>
        s"retain pension payment amount values when answer is $answer" in {

          val result = PensionPaymentsReceivedPage(srn).cleanup(answer, userAnswers).toOption.value

          result.get(TotalAmountPensionPaymentsPage(srn, refineMV(1))) must not be None
          result.get(TotalAmountPensionPaymentsPage(srn, refineMV(2))) must not be None
        }
      }

      "remove all pcl amount values when answer is Some(false)" in {

        val result = PensionPaymentsReceivedPage(srn).cleanup(Some(false), userAnswers).toOption.value

        result.get(TotalAmountPensionPaymentsPage(srn, refineMV(1))) mustBe None
        result.get(TotalAmountPensionPaymentsPage(srn, refineMV(2))) mustBe None
      }
    }
  }
}
