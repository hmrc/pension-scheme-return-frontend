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

$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

import play.api.libs.json.JsPath
import models.SchemeId.Srn
import models.Money
import pages.QuestionPage
$if(!index.empty)$
import config.Refined.$index$
import utils.RefinedUtils._
import eu.timepit.refined.refineMV
$endif$

$if(index.empty)$
case class $className;format="cap"$Page(srn: Srn) extends QuestionPage[Money] {
$else$
case class $className;format="cap"$Page(srn: Srn, index: $index$) extends QuestionPage[Money] {
$endif$

  $if(index.empty)$
  override def path: JsPath = JsPath \ toString
  $else$
    override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString
  $endif$
  
  override def toString: String = "$className;format="decap"$"
}
