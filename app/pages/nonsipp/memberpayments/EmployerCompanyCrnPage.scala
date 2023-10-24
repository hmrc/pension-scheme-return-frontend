package pages.nonsipp.memberpayments

import config.Refined.Max5000
import models.SchemeId.Srn
import pages.QuestionPage
import play.api.libs.json.JsPath

case class EmployerCompanyCrnPage (srn: Srn, index: Max5000)
  extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "crnOfPurchaser"
}