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

package pages.nonsipp.sharesdisposal

import config.Refined.{Max50, Max5000}
import models.HowSharesDisposed.HowSharesDisposed
import models.SchemeId.Srn
import models.{HowSharesDisposed, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils.removePages
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class HowWereSharesDisposedPage(
  srn: Srn,
  shareIndex: Max5000,
  disposalIndex: Max50,
  answerChanged: Boolean
) extends QuestionPage[HowSharesDisposed] {

  override def path: JsPath =
    Paths.disposedSharesTransaction \ toString \ shareIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"

  override def cleanup(value: Option[HowSharesDisposed], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(HowSharesDisposed.Sold), Some(HowSharesDisposed.Sold)) => Try(userAnswers)
      case (Some(HowSharesDisposed.Transferred), Some(HowSharesDisposed.Transferred)) => Try(userAnswers)
      case (Some(HowSharesDisposed.Redeemed), Some(HowSharesDisposed.Redeemed)) => Try(userAnswers)
      case (Some(HowSharesDisposed.Other(_)), Some(HowSharesDisposed.Other(_))) => Try(userAnswers)
      case (Some(_), Some(_)) => removePages(userAnswers, pages(srn, shareIndex, disposalIndex, isLastRecord = false))
      case (None, _) =>
        val completedPages = userAnswers.map(SharesDisposalCompletedPages(srn))
        removePages(
          userAnswers,
          pages(
            srn,
            shareIndex,
            disposalIndex,
            completedPages.flatten(_._2).size == 1
          )
        )
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, isLastRecord: Boolean): List[Removable[_]] = {
    val list = List(
      WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex),
      HowManySharesRedeemedPage(srn, shareIndex, disposalIndex),
      TotalConsiderationSharesRedeemedPage(srn, shareIndex, disposalIndex),
      HowManySharesPage(srn, shareIndex, disposalIndex),
      WhenWereSharesSoldPage(srn, shareIndex, disposalIndex),
      HowManySharesSoldPage(srn, shareIndex, disposalIndex),
      TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex),
      WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex), // has it's own cleanup
      IndependentValuationPage(srn, shareIndex, disposalIndex)
    )
    if (isLastRecord) list :+ SharesDisposalPage(srn) else list
  }
}

object HowWereSharesDisposedPage {
  def apply(srn: Srn, shareIndex: Max5000, disposalIndex: Max50): HowWereSharesDisposedPage =
    HowWereSharesDisposedPage(srn, shareIndex, disposalIndex, answerChanged = false)
}

case class HowWereSharesDisposedPagesForShare(srn: Srn, shareIndex: Max5000)
    extends QuestionPage[Map[String, HowSharesDisposed]] {
  override def path: JsPath =
    Paths.disposedSharesTransaction \ toString \ shareIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"
}

case class HowWereSharesDisposedPages(srn: Srn) extends QuestionPage[Map[String, Map[String, HowSharesDisposed]]] {
  override def path: JsPath =
    Paths.disposedSharesTransaction \ toString

  override def toString: String = "methodOfDisposal"
}
