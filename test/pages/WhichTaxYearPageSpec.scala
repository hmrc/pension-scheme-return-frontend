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

package pages

import models.{DateRange, UserAnswers}
import pages.behaviours.PageBehaviours
import pages.nonsipp.{CheckReturnDatesPage, WhichTaxYearPage}
import utils.UserAnswersUtils.UserAnswersOps

class WhichTaxYearPageSpec extends PageBehaviours {

  val srn = srnGen.sample.value

  "WhichTaxYearPage" - {

    beRetrievable[DateRange](WhichTaxYearPage(srn))

    beSettable[DateRange](WhichTaxYearPage(srn))

    beRemovable[DateRange](WhichTaxYearPage(srn))

    "cleanup" - {

      val dateRange1 = dateRangeGen.sample.value
      val dateRange2 = dateRangeGen.retryUntil(_ != dateRange1).sample.value
      val checkDates = boolean.sample.value
      val userAnswers =
        UserAnswers("id")
          .unsafeSet(WhichTaxYearPage(srn), dateRange1)
          .unsafeSet(CheckReturnDatesPage(srn), checkDates)

      "remove check return dates value when tax year changes" - {
        val updatedAnswers = userAnswers.set(WhichTaxYearPage(srn), dateRange2).toOption.value
        updatedAnswers.get(CheckReturnDatesPage(srn)) mustBe None
      }

      "do not remove check return dates value when tax year remains the same" - {
        val updatedAnswers = userAnswers.set(WhichTaxYearPage(srn), dateRange1).toOption.value
        updatedAnswers.get(CheckReturnDatesPage(srn)) mustBe Some(checkDates)
      }
    }
  }
}
