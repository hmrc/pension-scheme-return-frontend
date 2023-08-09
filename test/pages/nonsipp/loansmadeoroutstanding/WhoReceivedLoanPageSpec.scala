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

package pages.nonsipp.loansmadeoroutstanding

import config.Refined.OneTo9999999
import eu.timepit.refined.refineMV
import models.{ReceivedLoanType, UserAnswers}
import pages.behaviours.PageBehaviours
import pages.nonsipp.common.{WhoReceivedLoanPage, WhoReceivedLoans}
import utils.UserAnswersUtils.UserAnswersOps

class WhoReceivedLoanPageSpec extends PageBehaviours {
  "WhoReceivedLoanPage" - {
    val index = refineMV[OneTo9999999](1)

    val srn = srnGen.sample.value

    beRetrievable[ReceivedLoanType](WhoReceivedLoanPage(srn, index))

    beSettable[ReceivedLoanType](WhoReceivedLoanPage(srn, index))

    beRemovable[ReceivedLoanType](WhoReceivedLoanPage(srn, index))

    "must be able to set and retrieve multiple" in {
      val ua = UserAnswers("test")
        .unsafeSet(WhoReceivedLoanPage(srn, refineMV[OneTo9999999](1)), ReceivedLoanType.Individual)
        .unsafeSet(WhoReceivedLoanPage(srn, refineMV[OneTo9999999](2)), ReceivedLoanType.UKCompany)
        .unsafeSet(WhoReceivedLoanPage(srn, refineMV[OneTo9999999](3)), ReceivedLoanType.UKPartnership)

      ua.map(WhoReceivedLoans(srn)).values.toList mustBe List(
        ReceivedLoanType.Individual,
        ReceivedLoanType.UKCompany,
        ReceivedLoanType.UKPartnership
      )
    }
  }
}
