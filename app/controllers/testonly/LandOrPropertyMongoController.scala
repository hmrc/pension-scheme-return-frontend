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
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import pages.nonsipp.landorproperty._
import models._
import shapeless._
import viewmodels.models.SectionCompleted
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class LandOrPropertyMongoController @Inject()(
  val saveService: SaveService,
  val identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends TestDataSingleIndexController[Max5000.Refined] {

  override val max: Max5000 = refineMV(5000)

  override def pages(srn: Srn, index: Max5000): Pages = HList(
    (
      PageWithValue(LandOrPropertyHeldPage(srn), true),
      PageWithValue(LandPropertyInUKPage(srn, index), true),
      PageWithValue(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo.yes[String, String]("title number")),
      PageWithValue(LandOrPropertyChosenAddressPage(srn, index), address(index.value)),
      PageWithValue(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Transfer),
      PageWithValue(LandOrPropertyTotalCostPage(srn, index), Money(12.34)),
      PageWithValue(LandOrPropertyTotalIncomePage(srn, index), Money(12.34)),
      PageWithValue(IsLandOrPropertyResidentialPage(srn, index), true),
      PageWithValue(IsLandPropertyLeasedPage(srn, index), false),
      PageWithValue(LandOrPropertyCompleted(srn, index), SectionCompleted)
    )
  )

  override type Pages =
    PageWithValue[Boolean] ::
      PageWithValue[Boolean] ::
      PageWithValue[ConditionalYesNo[String, String]] ::
      PageWithValue[Address] ::
      PageWithValue[SchemeHoldLandProperty] ::
      PageWithValue[Money] ::
      PageWithValue[Money] ::
      PageWithValue[Boolean] ::
      PageWithValue[Boolean] ::
      PageWithValue[SectionCompleted] ::
      HNil

  private def address(index: Int) = Address(
    "123",
    addressLine1 = s"${index.toString} test street",
    addressLine2 = Some("test line 2"),
    addressLine3 = None,
    town = "test town",
    postCode = Some("ZZ1 1ZZ"),
    country = "United Kingdom",
    countryCode = "GB",
    ManualAddress
  )
}
