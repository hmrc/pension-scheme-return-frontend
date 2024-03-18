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

package controllers.testonly

import services.SaveService
import pages.nonsipp.otherassetsdisposal._
import play.api.mvc.MessagesControllerComponents
import models.HowDisposed.HowDisposed
import config.Refined.{Max50, Max5000}
import models.SchemeId.Srn
import models._
import shapeless._
import viewmodels.models.SectionCompleted
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._

import scala.concurrent.ExecutionContext

import java.time.LocalDate
import javax.inject.Inject

class AssetsDisposalMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataDoubleIndexController[Max5000.Refined, Max50.Refined] {

  override val max: Max50 = refineMV(50)

  override def pages(srn: Srn, index: Max5000, secondaryIndex: Max50): Pages =
    HList(
      (
        PageWithValue(OtherAssetsDisposalCompleted(srn, index, secondaryIndex), SectionCompleted),
        PageWithValue(HowWasAssetDisposedOfPage(srn, index, secondaryIndex), HowDisposed.Transferred),
        PageWithValue(IsBuyerConnectedPartyPage(srn, index, secondaryIndex), false),
        PageWithValue(TotalConsiderationSaleAssetPage(srn, index, secondaryIndex), Money(12.34)),
        PageWithValue(WhenWasAssetSoldPage(srn, index, secondaryIndex), LocalDate.of(2023, 12, 12)),
        PageWithValue(TypeOfAssetBuyerPage(srn, index, secondaryIndex), IdentityType.Other),
        PageWithValue(
          OtherBuyerDetailsPage(srn, index, secondaryIndex),
          RecipientDetails("test name", "test description")
        ),
        PageWithValue(OtherAssetsDisposalPage(srn), true)
      )
    )

  override type Pages =
    PageWithValue[SectionCompleted] ::
      PageWithValue[HowDisposed] ::
      PageWithValue[Boolean] ::
      PageWithValue[Money] ::
      PageWithValue[LocalDate] ::
      PageWithValue[IdentityType] ::
      PageWithValue[RecipientDetails] ::
      PageWithValue[Boolean] ::
      HNil
}
