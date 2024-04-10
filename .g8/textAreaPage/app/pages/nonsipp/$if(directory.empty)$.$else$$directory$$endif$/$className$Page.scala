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

$! Generic !$
$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

$if(!index.empty)$
import config.Refined._
import utils.RefinedUtils._
$endif$

import play.api.libs.json.JsPath
import models.SchemeId.Srn
import pages.QuestionPage
$! Generic end !$

$! Generic (change QuestionPage type) !$
$if(index.empty)$
case class $className;format="cap"$Page(srn: Srn) extends QuestionPage[String] {
$else$
$if(secondaryIndex.empty)$
case class $className;format="cap"$Page(srn: Srn, index: $index$) extends QuestionPage[String] {
$else$
case class $className;format="cap"$Page(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$) extends QuestionPage[String] {
$endif$
$endif$

  $if(index.empty)$
  override def path: JsPath = JsPath \ toString
  $else$
  $if(secondaryIndex.empty)$
  override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString
  $else$
  override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString \ secondaryIndex.arrayIndex.toString
  $endif$
  $endif$

  override def toString: String = "$className;format="decap"$"
}
$! Generic end !$
