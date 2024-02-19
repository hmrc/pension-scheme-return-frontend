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

import config.Refined.{Max50, Max5000}
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._
import models.HowDisposed.HowDisposed
import models.SchemeId.Srn
import models.{HowDisposed, IdentityType, Money, RecipientDetails}
import pages.nonsipp.landorpropertydisposal._
import play.api.mvc.MessagesControllerComponents
import services.SaveService
import shapeless._
import viewmodels.models.SectionCompleted

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class LandOrPropertyDisposalMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataDoubleIndexController[Max5000.Refined, Max50.Refined] {

  override type Pages =
    PageWithValue[RecipientDetails] ::
      PageWithValue[Boolean] ::
      PageWithValue[HowDisposed] ::
      PageWithValue[Boolean] ::
      PageWithValue[Money] ::
      PageWithValue[Boolean] ::
      PageWithValue[IdentityType] ::
      PageWithValue[LocalDate] ::
      PageWithValue[SectionCompleted.type] ::
      HNil

  override val max: Max50 = refineMV(50)

  override def pages(srn: Srn, index: Max5000, secondaryIndex: Max50): Pages = HList(
    (
      PageWithValue(
        OtherBuyerDetailsPage(srn, index, secondaryIndex),
        RecipientDetails("test recipient", "test description")
      ),
      PageWithValue(LandOrPropertyStillHeldPage(srn, index, secondaryIndex), true),
      PageWithValue(HowWasPropertyDisposedOfPage(srn, index, secondaryIndex), HowDisposed.Sold),
      PageWithValue(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, secondaryIndex), true),
      PageWithValue(TotalProceedsSaleLandPropertyPage(srn, index, secondaryIndex), Money(123.45)),
      PageWithValue(DisposalIndependentValuationPage(srn, index, secondaryIndex), true),
      PageWithValue(WhoPurchasedLandOrPropertyPage(srn, index, secondaryIndex), IdentityType.Other),
      PageWithValue(WhenWasPropertySoldPage(srn, index, secondaryIndex), LocalDate.of(2024, 12, 12)),
      PageWithValue(LandPropertyDisposalCompletedPage(srn, index, secondaryIndex), SectionCompleted)
    )
  )
}
