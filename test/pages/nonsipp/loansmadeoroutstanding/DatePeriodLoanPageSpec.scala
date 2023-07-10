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
import models.Money
import pages.behaviours.PageBehaviours

import java.time.LocalDate

class DatePeriodLoanPageSpec extends PageBehaviours {

  "DatePeriodLoanPage" - {
    val srn = srnGen.sample.value
    val index = refineMV[OneTo9999999](1)

    beRetrievable[(LocalDate, Money, Int)](DatePeriodLoanPage(srn, index))

    beSettable[(LocalDate, Money, Int)](DatePeriodLoanPage(srn, index))

    beRemovable[(LocalDate, Money, Int)](DatePeriodLoanPage(srn, index))
  }
}
