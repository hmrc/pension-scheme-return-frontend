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
import play.api.mvc.MessagesControllerComponents
import models.PensionSchemeType.PensionSchemeType
import config.Refined.{Max300, Max5}
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer._
import models.{Money, PensionSchemeType}
import shapeless._
import viewmodels.models.{SectionCompleted, SectionStatus}
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._

import scala.concurrent.ExecutionContext

import java.time.LocalDate
import javax.inject.Inject

class TransferInMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataDoubleIndexController[Max300.Refined, Max5.Refined] {

  override val max: Max5 = refineMV(5)

  override type Pages =
    PageWithValue[String] ::
      PageWithValue[PensionSchemeType] ::
      PageWithValue[Money] ::
      PageWithValue[LocalDate] ::
      PageWithValue[Boolean] ::
      PageWithValue[SectionCompleted.type] ::
      PageWithValue[SectionStatus] ::
      HNil

  override def pages(srn: Srn, index: Max300, secondaryIndex: Max5): Pages = HList(
    (
      PageWithValue(TransferringSchemeNamePage(srn, index, secondaryIndex), "test scheme"),
      PageWithValue(
        TransferringSchemeTypePage(srn, index, secondaryIndex),
        PensionSchemeType.Other("some description")
      ),
      PageWithValue(TotalValueTransferPage(srn, index, secondaryIndex), Money(12.34)),
      PageWithValue(WhenWasTransferReceivedPage(srn, index, secondaryIndex), LocalDate.now()),
      PageWithValue(DidTransferIncludeAssetPage(srn, index, secondaryIndex), true),
      PageWithValue(TransfersInSectionCompleted(srn, index, secondaryIndex), SectionCompleted),
      PageWithValue(TransfersInJourneyStatus(srn), SectionStatus.Completed)
    )
  )
}
