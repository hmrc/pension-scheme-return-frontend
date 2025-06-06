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

package pages.nonsipp.accountingperiod

import utils.RefinedUtils.RefinedIntOps
import queries.{Gettable, Removable, Settable}
import pages.QuestionPage
import config.RefinedTypes.Max3
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{DateRange, Mode}

case class AccountingPeriodPage(srn: Srn, index: Max3, mode: Mode) extends QuestionPage[DateRange] {

  override def path: JsPath = Paths.accountingPeriodDetails \ toString \ index.arrayIndex

  override def toString: String = "accountingPeriods"
}

case class AccountingPeriods(srn: Srn)
    extends Gettable[List[DateRange]]
    with Settable[List[DateRange]]
    with Removable[List[DateRange]] {

  override def path: JsPath = Paths.accountingPeriodDetails \ toString

  override def toString: String = "accountingPeriods"
}
