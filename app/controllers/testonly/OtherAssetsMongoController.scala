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

package controllers.testonly

import services.SaveService
import play.api.mvc.MessagesControllerComponents
import pages.nonsipp.otherassetsheld._
import config.Refined.Max5000
import models.SchemeId.Srn
import models.{Money, SchemeHoldAsset}
import shapeless._
import viewmodels.models.SectionCompleted
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._

import scala.concurrent.ExecutionContext

import java.time.LocalDate
import javax.inject.Inject

class OtherAssetsMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataSingleIndexController[Max5000.Refined] {

  override val max: Max5000 = refineMV(5000)

  override def pages(srn: Srn, index: Max5000): Pages =
    HList(
      (
        PageWithValue(OtherAssetsHeldPage(srn), true),
        PageWithValue(OtherAssetsCompleted(srn, index), SectionCompleted),
        PageWithValue(OtherAssetsListPage(srn), false),
        PageWithValue(IncomeFromAssetPage(srn, index), Money(34.56)),
        PageWithValue(IndependentValuationPage(srn, index), false),
        PageWithValue(CostOfOtherAssetPage(srn, index), Money(12.34)),
        PageWithValue(WhenDidSchemeAcquireAssetsPage(srn, index), LocalDate.of(2023, 12, 12)),
        PageWithValue(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Contribution),
        PageWithValue(IsAssetTangibleMoveablePropertyPage(srn, index), false),
        PageWithValue(WhatIsOtherAssetPage(srn, index), s"Test asset ${index.value}"),
        PageWithValue(OtherAssetsHeldPage(srn), true)
      )
    )

  override type Pages =
    PageWithValue[Boolean] ::
      PageWithValue[SectionCompleted] ::
      PageWithValue[Boolean] ::
      PageWithValue[Money] ::
      PageWithValue[Boolean] ::
      PageWithValue[Money] ::
      PageWithValue[LocalDate] ::
      PageWithValue[SchemeHoldAsset] ::
      PageWithValue[Boolean] ::
      PageWithValue[String] ::
      PageWithValue[Boolean] ::
      HNil
}
