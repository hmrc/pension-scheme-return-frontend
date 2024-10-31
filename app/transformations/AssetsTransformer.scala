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

package transformations

import pages.nonsipp.bonds.UnregulatedOrConnectedBondsHeldPage
import models.SchemeId.Srn
import pages.nonsipp.landorproperty.{LandOrPropertyHeldPage, LandPropertyInUKPages}
import models.requests.psr._
import models._
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import models.requests.DataRequest
import pages.nonsipp.otherassetsheld._
import com.google.inject.Singleton

import scala.util.Try

import javax.inject.Inject

@Singleton()
class AssetsTransformer @Inject()(
  landOrPropertyTransformer: LandOrPropertyTransformer,
  borrowingTransformer: BorrowingTransformer,
  bondsTransformer: BondsTransformer,
  otherAssetsTransformer: OtherAssetsTransformer
) extends Transformer {

  def transformToEtmp(srn: Srn, initialUA: UserAnswers)(
    implicit request: DataRequest[_]
  ): Option[Assets] = {
    val optLandOrPropertyHeld = request.userAnswers.get(LandOrPropertyHeldPage(srn))
    val optLandOrPropertyHeldOrList = Option.when(
      optLandOrPropertyHeld.nonEmpty || request.userAnswers.map(LandPropertyInUKPages(srn)).toList.nonEmpty
    )(true)
    val optMoneyWasBorrowed = request.userAnswers.get(MoneyBorrowedPage(srn))
    val optUnregulatedOrConnectedBondsHeld = request.userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn))
    val optOtherAssetsHeld = request.userAnswers.get(OtherAssetsHeldPage(srn))

    Option.when(
      List(
        optLandOrPropertyHeldOrList,
        optMoneyWasBorrowed, optUnregulatedOrConnectedBondsHeld, optOtherAssetsHeld)
        .exists(
          _.isDefined
        )
    )(
      Assets(
        optLandOrProperty = landOrPropertyTransformer.transformToEtmp(srn, optLandOrPropertyHeld, initialUA),
        optBorrowing = borrowingTransformer.transformToEtmp(srn, optMoneyWasBorrowed, initialUA),
        optBonds = bondsTransformer.transformToEtmp(srn, optUnregulatedOrConnectedBondsHeld, initialUA),
        optOtherAssets = otherAssetsTransformer.transformToEtmp(srn, optOtherAssetsHeld, initialUA)
      )
    )
  }

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, assets: Assets): Try[UserAnswers] =
    for {
      transformedLandOrPropertyUa <- assets.optLandOrProperty
        .map(
          landOrProperty => {
            landOrPropertyTransformer
              .transformFromEtmp(
                userAnswers,
                srn,
                landOrProperty
              )
          }
        )
        .getOrElse(Try(userAnswers))

      transformedMoneyBorrowingUa <- assets.optBorrowing
        .map(
          borrowing =>
            borrowingTransformer
              .transformFromEtmp(
                transformedLandOrPropertyUa,
                srn,
                borrowing
              )
        )
        .getOrElse(Try(transformedLandOrPropertyUa))

      transformedBondsUa <- assets.optBonds
        .map(
          bonds =>
            bondsTransformer
              .transformFromEtmp(
                transformedMoneyBorrowingUa,
                srn,
                bonds
              )
        )
        .getOrElse(Try(transformedMoneyBorrowingUa))

      transformedOtherAssetsUa <- assets.optOtherAssets
        .map(
          otherAssets =>
            otherAssetsTransformer
              .transformFromEtmp(
                transformedBondsUa,
                srn,
                otherAssets
              )
        )
        .getOrElse(Try(transformedBondsUa))

    } yield transformedOtherAssetsUa
}
