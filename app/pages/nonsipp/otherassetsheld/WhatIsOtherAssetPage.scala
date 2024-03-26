/*
 * Copyright 2023 HM Revenue & Customs
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

package pages.nonsipp.otherassetsheld

import config.Refined.Max5000
import models.SchemeId.Srn
import models.UserAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils.removePages
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class WhatIsOtherAssetPage(srn: Srn, index: Max5000) extends QuestionPage[String] {

  override def path: JsPath = Paths.otherAssetsTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "assetDescription"

  override def cleanup(value: Option[String], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(_), Some(_)) => Try(userAnswers)
      case (None, _) =>
        val completedPages = userAnswers.map(IncomeFromAssetPages(srn))
        removePages(
          userAnswers,
          pages(srn, index, completedPages.size == 1)
        )
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn, index: Max5000, isLastRecord: Boolean): List[Removable[_]] = {
    val list = List(
      IsAssetTangibleMoveablePropertyPage(srn, index),
      WhyDoesSchemeHoldAssetsPage(srn, index), // This triggers cleanup of any other dependent pages in the journey
      CostOfOtherAssetPage(srn, index),
      IncomeFromAssetPage(srn, index),
      OtherAssetsCYAPointOfEntry(srn, index),
      OtherAssetsCompleted(srn, index)
    )
    if (isLastRecord) list :+ OtherAssetsHeldPage(srn) else list
  }
}

case class WhatIsOtherAssetPages(srn: Srn) extends QuestionPage[Map[String, String]] {

  override def path: JsPath = Paths.otherAssetsTransactions \ toString

  override def toString: String = "assetDescription"
}
