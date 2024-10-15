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
import pages.QuestionPage
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.UserAnswers

import scala.util.Try
case class IsLandPropertyLeasedPage(srn: Srn, index: Max5000) extends QuestionPage[Boolean] {

  override def path: JsPath = Paths.heldPropertyTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "landOrPropertyLeased"

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(true), Some(true)) => Try(userAnswers)
      case (Some(false), Some(false)) => Try(userAnswers)
      case (Some(true), Some(false)) => Try(userAnswers)
      case (Some(false), Some(true)) => removePages(userAnswers, pages(srn))
      case (None, _) => removePages(userAnswers, pages(srn))
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn): List[Removable[_]] =
    List(
      LandOrPropertyLeaseDetailsPage(srn, index),
      IsLesseeConnectedPartyPage(srn, index)
    )

}
