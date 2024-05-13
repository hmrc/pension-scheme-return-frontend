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

package pages.nonsipp.receivetransfer

import utils.RefinedUtils._
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import config.Refined._
import pages.QuestionPage

import java.time.LocalDate

case class WhenWasTransferReceivedPage(srn: Srn, index: Max300, secondaryIndex: Max5) extends QuestionPage[LocalDate] {

  override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString \ secondaryIndex.arrayIndex.toString

  override def toString: String = "dateOfTransfer"
}

case class AllWhenWasTransferMadePages(srn: Srn) extends QuestionPage[Map[String, Map[String, LocalDate]]] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "dateOfTransfer"
}
