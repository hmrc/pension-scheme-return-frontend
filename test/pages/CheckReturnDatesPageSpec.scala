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

import eu.timepit.refined.refineMV
import pages.nonsipp.accountingperiod.{AccountingPeriodPage, AccountingPeriods}
import utils.UserAnswersUtils.UserAnswersOps
import pages.nonsipp.CheckReturnDatesPage
import models.{NormalMode, UserAnswers}
import pages.behaviours.PageBehaviours

class CheckReturnDatesPageSpec extends PageBehaviours {

  val srn = srnGen.sample.value

  "CheckReturnDatesPage" - {

    beRetrievable[Boolean](CheckReturnDatesPage(srn))

    beSettable[Boolean](CheckReturnDatesPage(srn))

    beRemovable[Boolean](CheckReturnDatesPage(srn))

    "cleanup" - {

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(AccountingPeriodPage(srn, refineMV(1), NormalMode), dateRangeGen.sample.value)
          .unsafeSet(AccountingPeriodPage(srn, refineMV(2), NormalMode), dateRangeGen.sample.value)
          .unsafeSet(AccountingPeriodPage(srn, refineMV(3), NormalMode), dateRangeGen.sample.value)

      List(Some(true), None).foreach { answer =>
        s"remove accounting periods when answer is $answer" in {

          val result = CheckReturnDatesPage(srn).cleanup(answer, userAnswers).toOption.value

          result.get(AccountingPeriods(srn)) mustBe None
        }
      }

      "retain accounting periods when answer is Some(false)" in {

        val expected = userAnswers.get(AccountingPeriods(srn))

        val result = CheckReturnDatesPage(srn).cleanup(Some(false), userAnswers).toOption.value

        result.get(AccountingPeriods(srn)) mustBe expected
      }
    }
  }
}
