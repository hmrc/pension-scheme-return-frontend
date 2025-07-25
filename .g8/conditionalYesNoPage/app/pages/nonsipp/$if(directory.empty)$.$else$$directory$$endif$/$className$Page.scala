$if(directory.empty)$
package pages.nonsipp
$else$
package pages.nonsipp.$directory$
$endif$

import play.api.libs.json.JsPath
import models.SchemeId.Srn
import models._
import pages.QuestionPage
$if(!index.empty)$
import config.RefinedTypes.$index$
import utils.RefinedUtils._
import utils.IntUtils.given
$endif$

$if(index.empty)$
case class $className;format="cap"$Page(srn: Srn) extends QuestionPage[ConditionalYesNo[String, String]] {
$else$
case class $className;format="cap"$Page(srn: Srn, index: $index$) extends QuestionPage[ConditionalYesNo[String, String]] {
$endif$

  $if(index.empty)$
  override def path: JsPath = JsPath \ toString
  $else$
  override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString
  $endif$

  override def toString: String = "$className;format="decap"$"
}