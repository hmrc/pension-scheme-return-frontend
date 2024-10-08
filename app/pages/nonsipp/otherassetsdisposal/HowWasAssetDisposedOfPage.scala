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

package pages.nonsipp.otherassetsdisposal

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import models.HowDisposed.HowDisposed
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{HowDisposed, UserAnswers}
import config.Refined.{Max50, Max5000}
import pages.QuestionPage

import scala.util.Try

case class HowWasAssetDisposedOfPage(
  srn: Srn,
  assetIndex: Max5000,
  disposalIndex: Max50,
  answerChanged: Boolean
) extends QuestionPage[HowDisposed] {

  override def path: JsPath =
    Paths.assetsDisposed \ toString \ assetIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"

  override def cleanup(value: Option[HowDisposed], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(HowDisposed.Sold), Some(HowDisposed.Sold)) => Try(userAnswers)
      case (Some(HowDisposed.Transferred), Some(HowDisposed.Transferred)) => Try(userAnswers)
      case (Some(HowDisposed.Other(_)), Some(HowDisposed.Other(_))) => Try(userAnswers)
      case (Some(_), Some(_)) => removePages(userAnswers, pages(srn, assetIndex, disposalIndex, isLastRecord = false))
      case (None, _) =>
        val completedPages = userAnswers.map(OtherAssetsDisposalProgress.all(srn))
        removePages(
          userAnswers,
          pages(
            srn,
            assetIndex,
            disposalIndex,
            isLastRecord = completedPages.flatten(_._2).size == 1
          )
        )
      case _ => Try(userAnswers)
    }

  private def pages(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    isLastRecord: Boolean
  ): List[Removable[_]] = {
    val list = List(
      AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex),
      TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex),
      OtherAssetsDisposalProgress(srn, assetIndex, disposalIndex),
      WhenWasAssetSoldPage(srn, assetIndex, disposalIndex),
      AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex),
      TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex)
    )
    if (isLastRecord) list :+ OtherAssetsDisposalPage(srn) else list
  }

}

object HowWasAssetDisposedOfPage {
  def apply(srn: Srn, assetIndex: Max5000, disposalIndex: Max50): HowWasAssetDisposedOfPage =
    HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex, answerChanged = false)
}

case class HowWasAssetDisposedOfPagesForEachAsset(
  srn: Srn,
  assetIndex: Max5000
) extends QuestionPage[Map[String, HowDisposed]] {

  override def path: JsPath =
    Paths.assetsDisposed \ toString \ assetIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"
}

case class HowWasAssetDisposedOfPages(srn: Srn) extends QuestionPage[Map[String, Map[String, HowDisposed]]] {

  override def path: JsPath = Paths.assetsDisposed \ toString

  override def toString: String = "methodOfDisposal"
}
