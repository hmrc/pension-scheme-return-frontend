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

package pages.nonsipp.bondsdisposal

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import models.HowDisposed.HowDisposed
import pages.QuestionPage
import config.RefinedTypes.{Max50, Max5000}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{HowDisposed, UserAnswers}

import scala.util.Try

case class HowWereBondsDisposedOfPage(
  srn: Srn,
  bondIndex: Max5000,
  disposalIndex: Max50,
  answerChanged: Boolean
) extends QuestionPage[HowDisposed] {

  override def path: JsPath =
    Paths.bondsDisposed \ toString \ bondIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"

  override def cleanup(value: Option[HowDisposed], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(HowDisposed.Sold), Some(HowDisposed.Sold)) => Try(userAnswers)
      case (Some(HowDisposed.Transferred), Some(HowDisposed.Transferred)) => Try(userAnswers)
      case (Some(HowDisposed.Other(_)), Some(HowDisposed.Other(_))) => Try(userAnswers)
      case (Some(_), Some(_)) =>
        removePages(
          userAnswers,
          pages(srn, bondIndex, disposalIndex, removeExtraPages = false, isLastRecord = false)
        )
      case (None, _) =>
        val completedPages = userAnswers.map(BondsDisposalProgress.all())
        removePages(
          userAnswers,
          pages(
            srn,
            bondIndex,
            disposalIndex,
            removeExtraPages = true,
            isLastRecord = completedPages.flatten(using _._2).size == 1
          )
        )
      case _ => Try(userAnswers)
    }

  private def pages(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    removeExtraPages: Boolean,
    isLastRecord: Boolean
  ): List[Removable[?]] = {
    val list = List(
      WhenWereBondsSoldPage(srn, bondIndex, disposalIndex),
      TotalConsiderationSaleBondsPage(srn, bondIndex, disposalIndex),
      BuyerNamePage(srn, bondIndex, disposalIndex),
      IsBuyerConnectedPartyPage(srn, bondIndex, disposalIndex)
    )

    (removeExtraPages, isLastRecord) match {
      case (true, true) =>
        list :+ BondsDisposalPage(
          srn
        ) :+ BondsStillHeldPage(
          srn,
          bondIndex,
          disposalIndex
        ) :+ BondsDisposalCYAPointOfEntry(
          srn,
          bondIndex,
          disposalIndex
        ) :+ BondsDisposalProgress(
          srn,
          bondIndex,
          disposalIndex
        )
      case (true, false) =>
        list :+ BondsStillHeldPage(
          srn,
          bondIndex,
          disposalIndex
        ) :+ BondsDisposalCYAPointOfEntry(
          srn,
          bondIndex,
          disposalIndex
        ) :+ BondsDisposalProgress(
          srn,
          bondIndex,
          disposalIndex
        )
      case (_, _) => list
    }
  }
}

object HowWereBondsDisposedOfPage {
  def apply(srn: Srn, bondIndex: Max5000, disposalIndex: Max50): HowWereBondsDisposedOfPage =
    HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex, answerChanged = false)
}

case class HowWereBondsDisposedOfPagesForEachBond(
  srn: Srn,
  bondIndex: Max5000
) extends QuestionPage[Map[String, HowDisposed]] {

  override def path: JsPath =
    Paths.bondsDisposed \ toString \ bondIndex.arrayIndex.toString

  override def toString: String = "methodOfDisposal"
}

case class HowWereBondsDisposedOfPages(srn: Srn) extends QuestionPage[Map[String, Map[String, HowDisposed]]] {

  override def path: JsPath = Paths.bondsDisposed \ toString

  override def toString: String = "methodOfDisposal"
}
