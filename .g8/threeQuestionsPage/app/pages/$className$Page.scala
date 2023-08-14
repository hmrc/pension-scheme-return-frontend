package pages

import play.api.libs.json.JsPath
import models.SchemeId.Srn
import models._
$if(!index.empty)$
import config.Refined.$index$
import utils.RefinedUtils._
import eu.timepit.refined.refineMV
$endif$

$if(index.empty)$
case class $className;format="cap"$Page(srn: Srn) extends QuestionPage[($field1Type$, $field2Type$, $field3Type$)] {
$else$
case class $className;format="cap"$Page(srn: Srn, index: $index$) extends QuestionPage[($field1Type$, $field2Type$, $field3Type$)] {
$endif$

  $if(index.empty)$
  override def path: JsPath = JsPath \ toString
  $else$
  override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString
  $endif$

  override def toString: String = "$className;format="decap"$"
}
