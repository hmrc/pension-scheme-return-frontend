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

import config.Refined.{Max50, Max5000}
import models.HowDisposed.HowDisposed
import models.SchemeId.Srn
import models.{HowDisposed, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils.removePages
import utils.RefinedUtils.RefinedIntOps

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
      case (Some(_), Some(_)) =>
        removePages(
          userAnswers,
          pages(srn)
        )
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn): List[Removable[_]] =
    List(
      )
}

object HowWasAssetDisposedOfPage {
  def apply(srn: Srn, assetIndex: Max5000, disposalIndex: Max50): HowWasAssetDisposedOfPage =
    HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex, answerChanged = false)
}
