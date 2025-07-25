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

package pages.nonsipp.bonds

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import pages.QuestionPage
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{SchemeHoldBond, UserAnswers}

import scala.util.Try

case class WhyDoesSchemeHoldBondsPage(srn: Srn, index: Max5000) extends QuestionPage[SchemeHoldBond] {

  override def path: JsPath =
    Paths.bondTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "methodOfHolding"

  override def cleanup(value: Option[SchemeHoldBond], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(SchemeHoldBond.Acquisition), Some(SchemeHoldBond.Acquisition)) => Try(userAnswers) // no change
      case (Some(SchemeHoldBond.Contribution), Some(SchemeHoldBond.Contribution)) => Try(userAnswers) // no change
      case (Some(SchemeHoldBond.Transfer), Some(SchemeHoldBond.Transfer)) => Try(userAnswers) // no change
      case (Some(_), Some(_)) => removePages(userAnswers, dependantPages(srn))
      case (None, _) => removePages(userAnswers, dependantPages(srn))
      case _ => Try(userAnswers)
    }

  private def dependantPages(srn: Srn): List[Removable[?]] =
    List(
      WhenDidSchemeAcquireBondsPage(srn, index),
      BondsFromConnectedPartyPage(srn, index)
    )
}
