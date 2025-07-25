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
import config.RefinedTypes._
import utils.RefinedUtils._
import utils.IntUtils.given
$endif$

$! Generic (change QuestionPage type) !$
$if(index.empty)$
case class $className;format="cap"$Page(srn: Srn) extends QuestionPage[Boolean] {
  $else$
  $if(secondaryIndex.empty)$
  case class $className;format="cap"$Page(srn: Srn, index: $index$) extends QuestionPage[Boolean] {
    $else$
    case class $className;format="cap"$Page(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$) extends QuestionPage[Boolean] {
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