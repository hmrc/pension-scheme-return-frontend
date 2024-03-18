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

package pages.nonsipp.shares

import utils.RefinedUtils.RefinedIntOps
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.TypeOfShares
import config.Refined.Max5000
import pages.QuestionPage

case class TypeOfSharesHeldPage(srn: Srn, index: Max5000) extends QuestionPage[TypeOfShares] {

  override def path: JsPath =
    Paths.shareTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "typeOfSharesHeld"
}

case class TypeOfSharesHeldPages(srn: Srn) extends QuestionPage[Map[String, TypeOfShares]] {
  override def path: JsPath =
    Paths.shareTransactions \ toString
  override def toString: String = "typeOfSharesHeld"
}
