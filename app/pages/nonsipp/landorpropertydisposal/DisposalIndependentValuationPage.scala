package pages.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import models.SchemeId.Srn
import pages.QuestionPage
import pages.nonsipp.landorproperty.Paths
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps

case class DisposalIndependentValuationPage(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50)
  extends QuestionPage[Boolean] {

  override def path: JsPath =
    Paths.heldPropertyTransactions \ toString \ landOrPropertyIndex.arrayIndex.toString

  override def toString: String = "indepValuationSupport"

}