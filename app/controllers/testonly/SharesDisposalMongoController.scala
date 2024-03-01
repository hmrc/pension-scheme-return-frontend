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
import eu.timepit.refined.refineMV
import models.HowSharesDisposed._
import models.SchemeId.Srn
import pages.nonsipp.sharesdisposal.{HowManyDisposalSharesPage, HowWereSharesDisposedPage, SharesDisposalCompletedPage}
import play.api.mvc.MessagesControllerComponents
import services.SaveService
import shapeless.{::, HList, HNil}
import viewmodels.models.SectionCompleted

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SharesDisposalMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataDoubleIndexController[Max5000.Refined, Max50.Refined] {

  override type Pages =
    PageWithValue[SectionCompleted] ::
      PageWithValue[HowSharesDisposed] ::
      PageWithValue[Int] ::
      HNil

  override val max: Max50 = refineMV(50)

  override def pages(srn: Srn, index: Max5000, secondaryIndex: Max50): Pages = HList(
    PageWithValue(SharesDisposalCompletedPage(srn, index, secondaryIndex), SectionCompleted),
    PageWithValue(HowWereSharesDisposedPage(srn, index, secondaryIndex), Transferred),
    PageWithValue(HowManyDisposalSharesPage(srn, index, secondaryIndex), 3)
  )
}
