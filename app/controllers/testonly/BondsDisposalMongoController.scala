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
import models.HowDisposed._
import config.Refined.{Max50, Max5000}
import models.SchemeId.Srn
import shapeless.{::, HList, HNil}
import pages.nonsipp.bondsdisposal._
import viewmodels.models.SectionCompleted
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineMV

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class BondsDisposalMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataDoubleIndexController[Max5000.Refined, Max50.Refined] {

  override type Pages =
    PageWithValue[Boolean] ::
      PageWithValue[SectionCompleted] ::
      PageWithValue[HowDisposed] ::
      PageWithValue[Int] ::
      HNil

  override val max: Max50 = refineMV(50)

  override def pages(srn: Srn, index: Max5000, secondaryIndex: Max50): Pages = HList(
    (
      PageWithValue(BondsDisposalPage(srn), true),
      PageWithValue(BondsDisposalProgress(srn, index, secondaryIndex), SectionCompleted),
      PageWithValue(HowWereBondsDisposedOfPage(srn, index, secondaryIndex), Transferred),
      PageWithValue(BondsStillHeldPage(srn, index, secondaryIndex), 3)
    )
  )
}
