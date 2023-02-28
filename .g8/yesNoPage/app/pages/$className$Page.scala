package pages

import play.api.libs.json.JsPath
import models.SchemeId.Srn

case class $className;format="cap"$Page(snr: Srn) extends QuestionPage[Boolean] {
  
  override def path: JsPath = JsPath \ toString
  
  override def toString: String = "$className;format="decap"$"
}
