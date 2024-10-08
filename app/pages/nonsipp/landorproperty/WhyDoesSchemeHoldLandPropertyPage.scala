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

package pages.nonsipp.landorproperty

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils._
import queries.Removable
import models.SchemeId.Srn
import models.{IdentitySubject, SchemeHoldLandProperty, UserAnswers}
import config.Refined.Max5000
import pages.QuestionPage
import pages.nonsipp.common
import play.api.libs.json.JsPath

import scala.util.Try

case class WhyDoesSchemeHoldLandPropertyPage(srn: Srn, index: Max5000) extends QuestionPage[SchemeHoldLandProperty] {

  override def path: JsPath =
    Paths.heldPropertyTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "methodOfHolding"

  override def cleanup(value: Option[SchemeHoldLandProperty], userAnswers: UserAnswers): Try[UserAnswers] = {
    val saved = userAnswers.get(this)
    (value, saved) match {
      case (Some(SchemeHoldLandProperty.Acquisition), Some(SchemeHoldLandProperty.Acquisition)) => Try(userAnswers)
      case (Some(SchemeHoldLandProperty.Contribution), Some(SchemeHoldLandProperty.Contribution)) => Try(userAnswers)
      case (Some(SchemeHoldLandProperty.Transfer), Some(SchemeHoldLandProperty.Transfer)) => Try(userAnswers)
      case (Some(SchemeHoldLandProperty.Acquisition), Some(_)) => removePages(userAnswers, pages(srn))
      case (Some(SchemeHoldLandProperty.Contribution), Some(_)) => removePages(userAnswers, pages(srn))
      case (Some(SchemeHoldLandProperty.Transfer), Some(_)) => removePages(userAnswers, pages(srn))
      case (None, _) => removePages(userAnswers, pages(srn))
      case _ => Try(userAnswers)
    }
  }

  private def pages(srn: Srn): List[Removable[_]] =
    List(
      LandOrPropertyWhenDidSchemeAcquirePage(srn, index),
      LandPropertyIndependentValuationPage(srn, index),
      common.IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller)
    )
}
