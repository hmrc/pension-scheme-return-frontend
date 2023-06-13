package pages

import play.api.libs.json.JsPath
import models.SchemeId.Srn

case class $className;format="cap"$Page(srn: Srn) extends QuestionPage[???] {
  
  override def path: JsPath = JsPath \ toString
  
  override def toString: String = "$className;format="decap"$"
}
