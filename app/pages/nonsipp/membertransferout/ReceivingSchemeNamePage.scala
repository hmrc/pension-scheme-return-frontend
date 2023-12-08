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

package pages.nonsipp.membertransferout

import config.Refined.{Max300, Max5}
import models.SchemeId.Srn
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps

case class ReceivingSchemeNamePage(srn: Srn, index: Max300, transferIndex: Max5) extends QuestionPage[String] {

  override def path: JsPath =
    Paths.properties \ toString \ index.arrayIndex.toString \ transferIndex.arrayIndex.toString

  override def toString: String = "schemeName"
}


case class ReceivingSchemeNamePages(srn: Srn, index: Max300) extends QuestionPage[Map[String, String]] {
  override def path: JsPath =
    Paths.properties \ toString \ index.arrayIndex.toString

  override def toString: String = "schemeName"
}