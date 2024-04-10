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

package pages.nonsipp.shares

import utils.RefinedUtils.RefinedIntOps
import play.api.libs.json.JsPath
import models.{TypeOfShares, UserAnswers}
import config.Refined.Max5000
import pages.QuestionPage
import models.TypeOfShares.{SponsoringEmployer, Unquoted}
import models.SchemeId.Srn

import scala.util.Try

case class TypeOfSharesHeldPage(srn: Srn, index: Max5000) extends QuestionPage[TypeOfShares] {

  override def path: JsPath =
    Paths.shareTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "typeOfSharesHeld"

  override def cleanup(value: Option[TypeOfShares], userAnswers: UserAnswers): Try[UserAnswers] =
    (userAnswers.get(this), value) match {
      // if unchanged, do nothing
      case (Some(a), Some(b)) if a == b => Try(userAnswers)
      // if changed from Unquoted to any other type of shares, remove connected party
      case (Some(Unquoted), Some(_)) => userAnswers.remove(SharesFromConnectedPartyPage(srn, index))
      // if changed from SponsoringEmployer to any other type of shares, remove total asset value
      case (Some(SponsoringEmployer), Some(_)) => userAnswers.remove(TotalAssetValuePage(srn, index))
      case _ => Try(userAnswers)
    }
}

case class TypeOfSharesHeldPages(srn: Srn) extends QuestionPage[Map[String, TypeOfShares]] {
  override def path: JsPath =
    Paths.shareTransactions \ toString
  override def toString: String = "typeOfSharesHeld"
}
