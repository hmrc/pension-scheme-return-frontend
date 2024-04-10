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

package pages.nonsipp.otherassetsheld

import utils.RefinedUtils.RefinedIntOps
import utils.PageUtils.removePages
import queries.Removable
import models.SchemeId.Srn
import play.api.libs.json.JsPath
import models.{IdentitySubject, SchemeHoldAsset, UserAnswers}
import pages.nonsipp.common.IdentityTypePage
import config.Refined.Max5000
import pages.QuestionPage

import scala.util.Try

case class WhyDoesSchemeHoldAssetsPage(srn: Srn, index: Max5000) extends QuestionPage[SchemeHoldAsset] {

  override def path: JsPath =
    Paths.otherAssetsTransactions \ toString \ index.arrayIndex.toString

  override def toString: String = "methodOfHolding"

  override def cleanup(value: Option[SchemeHoldAsset], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      // Answer changed ((new), (previous))
      case (Some(SchemeHoldAsset.Contribution), Some(SchemeHoldAsset.Acquisition)) =>
        removePages(userAnswers, acquisitionOnlyPages(srn))
      case (Some(SchemeHoldAsset.Transfer), Some(SchemeHoldAsset.Acquisition)) =>
        removePages(userAnswers, acquisitionOnlyPages(srn) ++ acquisitionAndContributionPages(srn))
      case (Some(SchemeHoldAsset.Acquisition), Some(SchemeHoldAsset.Contribution)) =>
        Try(userAnswers)
      case (Some(SchemeHoldAsset.Transfer), Some(SchemeHoldAsset.Contribution)) =>
        removePages(userAnswers, acquisitionAndContributionPages(srn))
      case (Some(SchemeHoldAsset.Acquisition), Some(SchemeHoldAsset.Transfer)) =>
        Try(userAnswers)
      case (Some(SchemeHoldAsset.Contribution), Some(SchemeHoldAsset.Transfer)) =>
        Try(userAnswers)
      // Answer unchanged
      case (Some(_), Some(_)) => Try(userAnswers)
      case (None, _) => removePages(userAnswers, acquisitionOnlyPages(srn) ++ acquisitionAndContributionPages(srn))
      case _ => Try(userAnswers)
    }

  private def acquisitionOnlyPages(srn: Srn): List[Removable[_]] =
    List(
      IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller),
      OtherAssetSellerConnectedPartyPage(srn, index)
    )

  private def acquisitionAndContributionPages(srn: Srn): List[Removable[_]] =
    List(
      WhenDidSchemeAcquireAssetsPage(srn, index),
      IndependentValuationPage(srn, index)
    )
}
