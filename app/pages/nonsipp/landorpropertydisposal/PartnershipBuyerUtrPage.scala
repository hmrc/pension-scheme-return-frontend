package pages.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import models.SchemeId.Srn
import models.{ConditionalYesNo, Utr}
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.RefinedUtils.RefinedIntOps

case class PartnershipBuyerUtrPage(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50)
  extends QuestionPage[ConditionalYesNo[String, Utr]] {

  override def path: JsPath =
    Paths.disposalPropertyTransaction \ toString \ landOrPropertyIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "idNumber"
}