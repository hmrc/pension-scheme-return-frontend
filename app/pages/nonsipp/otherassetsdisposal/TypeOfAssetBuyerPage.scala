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

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import pages.QuestionPage
import config.RefinedTypes.{Max50, Max5000}
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{IdentityType, UserAnswers}

import scala.util.Try

case class TypeOfAssetBuyerPage(srn: Srn, assetIndex: Max5000, disposalIndex: Max50)
    extends QuestionPage[IdentityType] {

  override def path: JsPath =
    Paths.assetsDisposed \ toString \ assetIndex.arrayIndex.toString \ disposalIndex.arrayIndex.toString

  override def toString: String = "purchaserType"

  private def pages(srn: Srn): List[Removable[?]] = List(
    IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex),
    AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex),
    CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex),
    AssetCompanyBuyerCrnPage(srn, assetIndex, disposalIndex),
    PartnershipBuyerNamePage(srn, assetIndex, disposalIndex),
    PartnershipBuyerUtrPage(srn, assetIndex, disposalIndex),
    OtherBuyerDetailsPage(srn, assetIndex, disposalIndex),
    IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex)
  )

  override def cleanup(value: Option[IdentityType], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (Some(IdentityType.Individual), Some(IdentityType.Individual)) => Try(userAnswers)
      case (Some(IdentityType.UKCompany), Some(IdentityType.UKCompany)) => Try(userAnswers)
      case (Some(IdentityType.UKPartnership), Some(IdentityType.UKPartnership)) => Try(userAnswers)
      case (Some(IdentityType.Other), Some(IdentityType.Other)) => Try(userAnswers)
      case (Some(_), _) => removePages(userAnswers, pages(srn))
      case (None, _) => removePages(userAnswers, pages(srn))
      case _ => Try(userAnswers)
    }
}
