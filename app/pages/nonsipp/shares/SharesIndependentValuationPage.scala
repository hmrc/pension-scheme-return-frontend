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

package pages.nonsipp.shares

import utils.RefinedUtils._
import pages.QuestionPage
import config.RefinedTypes._
import models.SchemeId.Srn
import play.api.libs.json.JsPath

case class SharesIndependentValuationPage(srn: Srn, index: Max5000) extends QuestionPage[Boolean] {
  override def path: JsPath = Paths.shareTransactions \ toString \ index.arrayIndex.toString
  override def toString: String = "supportedByIndepValuation"
}
