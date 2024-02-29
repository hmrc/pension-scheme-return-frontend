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

import config.Refined.Max5000
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._
import models.{Money, SchemeHoldBond}
import models.SchemeId.Srn
import pages.nonsipp.unregulatedorconnectedbonds._
import play.api.mvc.MessagesControllerComponents
import services.SaveService
import shapeless._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BondsMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataSingleIndexController[Max5000.Refined] {

  override val max: Max5000 = refineMV(5000)

  override def pages(srn: Srn, index: Max5000): Pages = HList(
    PageWithValue(AreBondsUnregulatedPage(srn, index), false),
    PageWithValue(BondsFromConnectedPartyPage(srn, index), false),
    PageWithValue(CostOfBondsPage(srn, index), Money(12.34)),
    PageWithValue(IncomeFromBondsPage(srn, index), Money(34.56)),
    PageWithValue(NameOfBondsPage(srn, index), "test bonds"),
    PageWithValue(UnregulatedOrConnectedBondsHeldPage(srn), true),
    PageWithValue(WhenDidSchemeAcquireBondsPage(srn, index), LocalDate.of(2023, 12, 12)),
    PageWithValue(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Transfer)
  )

  override type Pages =
    PageWithValue[Boolean] ::
      PageWithValue[Boolean] ::
      PageWithValue[Money] ::
      PageWithValue[Money] ::
      PageWithValue[String] ::
      PageWithValue[Boolean] ::
      PageWithValue[LocalDate] ::
      PageWithValue[SchemeHoldBond] ::
      HNil
}