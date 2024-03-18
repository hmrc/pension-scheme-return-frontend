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

package pages.nonsipp.shares

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import models.SchemeId.Srn
import models.{IdentitySubject, SchemeHoldShare, UserAnswers}
import config.Refined.Max5000
import pages.QuestionPage
import pages.nonsipp.common
import play.api.libs.json.JsPath

import scala.util.Try

case class WhyDoesSchemeHoldSharesPage(srn: Srn, index: Max5000) extends QuestionPage[SchemeHoldShare] {

  override def path: JsPath =
    Paths.heldSharesTransaction \ toString \ index.arrayIndex.toString

  override def toString: String = "methodOfHolding"

  override def cleanup(value: Option[SchemeHoldShare], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(SchemeHoldShare.Acquisition), Some(SchemeHoldShare.Acquisition)) => Try(userAnswers) // no change
      case (Some(SchemeHoldShare.Contribution), Some(SchemeHoldShare.Contribution)) => Try(userAnswers) // no change
      case (Some(SchemeHoldShare.Transfer), Some(SchemeHoldShare.Transfer)) => Try(userAnswers) // no change
      case (Some(_), Some(_)) => removePages(userAnswers, dependantPages(srn))
      case (None, _) => removePages(userAnswers, dependantPages(srn))
      case _ => Try(userAnswers)
    }

  private def dependantPages(srn: Srn): List[Removable[_]] =
    List(
      WhenDidSchemeAcquireSharesPage(srn, index),
      common.IdentityTypePage(srn, index, IdentitySubject.SharesSeller),
      TotalAssetValuePage(srn, index),
      SharesCompleted(srn, index)
    )
}
