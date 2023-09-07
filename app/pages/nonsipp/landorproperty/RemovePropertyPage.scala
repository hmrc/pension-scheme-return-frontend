package pages.nonsipp.landorproperty

import config.Refined.Max5000
import models.SchemeId.Srn
import pages.QuestionPage
import pages.nonsipp.landorproperty.Paths
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps

class RemovePropertyPage (srn: Srn, index: Max5000) extends QuestionPage[Boolean] {

  override def path: JsPath = Paths.landOrPropertyTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "removeProperty"
}