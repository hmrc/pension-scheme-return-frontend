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

package pages.nonsipp.accountingperiod

import config.Refined.Max3
import models.DateRange
import models.SchemeId.Srn
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.{Gettable, Removable}
import utils.RefinedUtils.RefinedIntOps

case class AccountingPeriodPage(srn: Srn, index: Max3) extends QuestionPage[DateRange] {

  override def path: JsPath = JsPath \ toString \ index.arrayIndex

  override def toString: String = "accountingPeriods"
}

case class AccountingPeriods(srn: Srn) extends Gettable[List[DateRange]] with Removable[List[DateRange]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "accountingPeriods"
}
