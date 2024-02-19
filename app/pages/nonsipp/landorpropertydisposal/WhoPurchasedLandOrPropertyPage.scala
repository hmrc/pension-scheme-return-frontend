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
import models.SchemeId.Srn
import models.{IdentityType, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import queries.Removable
import utils.PageUtils.removePages
import utils.RefinedUtils.RefinedIntOps

import scala.util.Try

case class WhoPurchasedLandOrPropertyPage(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50)
    extends QuestionPage[IdentityType] {

  override def path: JsPath =
    Paths.disposalPropertyTransaction \ toString \ landOrPropertyIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "purchasedLandOrProperty"

  private def pages(srn: Srn): List[Removable[_]] = List(
    LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex),
    IndividualBuyerNinoNumberPage(srn, landOrPropertyIndex, disposalIndex),
    CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex),
    CompanyBuyerCrnPage(srn, landOrPropertyIndex, disposalIndex),
    PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex),
    PartnershipBuyerUtrPage(srn, landOrPropertyIndex, disposalIndex),
    OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex),
    LandOrPropertyDisposalBuyerConnectedPartyPage(srn, landOrPropertyIndex, disposalIndex)
  )
  override def cleanup(value: Option[IdentityType], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(IdentityType.Individual), Some(IdentityType.Individual)) => Try(userAnswers)
      case (Some(IdentityType.UKCompany), Some(IdentityType.UKCompany)) => Try(userAnswers)
      case (Some(IdentityType.UKPartnership), Some(IdentityType.UKPartnership)) => Try(userAnswers)
      case (Some(IdentityType.Other), Some(IdentityType.Other)) => Try(userAnswers)
      case (None, _) => removePages(userAnswers, pages(srn)) // delete
      case (Some(_), None) => Try(userAnswers) // create
      case _ => removePages(userAnswers, pages(srn)) // update - where values were changed
    }
}
