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

import models.SchemeId.Srn
import utils.IntUtils.given
import pages.nonsipp.accountingperiod.{AccountingPeriodPage, AccountingPeriods}
import models.{DateRange, NormalMode}
import pages.behaviours.PageBehaviours

class AccountingPeriodPageSpec extends PageBehaviours {

  val srn: Srn = srnGen.sample.value

  "AccountingPeriodPage" - {

    beRetrievable[DateRange](AccountingPeriodPage(srn, 1, NormalMode))

    beSettable[DateRange](AccountingPeriodPage(srn, 1, NormalMode))

    beRemovable[DateRange](AccountingPeriodPage(srn, 1, NormalMode))
  }

  "AccountingPeriods" - {

    beRetrievable[DateRange]
      .list(
        getter = AccountingPeriods(srn),
        setter = AccountingPeriodPage(srn, 1, NormalMode)
      )

    beRemovable[DateRange]
      .list(
        getter = AccountingPeriods(srn),
        setter = AccountingPeriodPage(srn, 1, NormalMode),
        remover = AccountingPeriods(srn)
      )
  }
}
