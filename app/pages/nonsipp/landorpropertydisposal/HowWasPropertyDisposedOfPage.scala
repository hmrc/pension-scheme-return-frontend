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

package pages.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import models.HowDisposed.HowDisposed
import models.SchemeId.Srn
import models.{HowDisposed, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Removable
import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages

import scala.util.Try

case class HowWasPropertyDisposedOfPage(
  srn: Srn,
  landOrPropertyIndex: Max5000,
  disposalIndex: Max50,
  answerChanged: Boolean
) extends QuestionPage[HowDisposed] {

  override def path: JsPath =
    Paths.disposalPropertyTransaction \ toString \ landOrPropertyIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"

  override def cleanup(value: Option[HowDisposed], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(HowDisposed.Sold), Some(HowDisposed.Sold)) => Try(userAnswers)
      case (Some(HowDisposed.Transferred), Some(HowDisposed.Transferred)) => Try(userAnswers)
      case (Some(HowDisposed.Other(_)), Some(HowDisposed.Other(_))) => Try(userAnswers)
      case (Some(_), Some(_)) => removePages(userAnswers, pages(srn, landOrPropertyIndex, disposalIndex, false))
      case (None, _) =>
        val completedPages = userAnswers.map(LandPropertyDisposalCompletedPages(srn))
        removePages(
          userAnswers,
          pages(
            srn,
            landOrPropertyIndex,
            disposalIndex,
            completedPages.flatten(_._2).size == 1
          )
        )
      case _ => Try(userAnswers)
    }

  private def pages(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    isLastRecord: Boolean
  ): List[Removable[_]] = {
    val list = List(
      LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex),
      WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex),
      LandPropertyDisposalCompletedPage(srn, landOrPropertyIndex, disposalIndex),
      RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex),
      WhenWasPropertySoldPage(srn, landOrPropertyIndex, disposalIndex),
      DisposalIndependentValuationPage(srn, landOrPropertyIndex, disposalIndex),
      TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex)
    )
    if (isLastRecord) list :+ LandOrPropertyDisposalPage(srn) else list
  }
}

case class HowWasPropertyDisposedOfPages(
  srn: Srn,
  landOrPropertyIndex: Max5000
) extends QuestionPage[Map[String, HowDisposed]] {

  override def path: JsPath =
    Paths.disposalPropertyTransaction \ toString \ landOrPropertyIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"
}

object HowWasPropertyDisposedOfPage {
  def apply(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50): HowWasPropertyDisposedOfPage =
    HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex, answerChanged = false)
}
