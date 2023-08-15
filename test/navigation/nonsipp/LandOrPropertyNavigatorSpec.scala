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

package navigation.nonsipp

import config.Refined.Max5000
import eu.timepit.refined.refineMV
import models.SchemeHoldLandProperty.{Acquisition, Contribution, Transfer}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.landorproperty._
import utils.BaseSpec

class LandOrPropertyNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max5000.Refined](1)

  "LandOrPropertyNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyHeldPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.landorproperty.routes.WhatYouWillNeedLandOrPropertyController.onPageLoad(srn)
        )
        .withName("go from land or property held page to what you will need Land or Property page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyHeldPage,
          Gen.const(false),
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad
        )
        .withName("go from land or property held page to money borrowed page when no selected")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandPropertyInUKPage,
          Gen.const(true),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyAddressLookupController.onPageLoad(srn, index)
        )
        .withName("go from land or property in uk page to land or property address lookup when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandPropertyInUKPage,
          Gen.const(false),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyAddressLookupController.onPageLoad(srn, index)
        )
        .withName("go from land or property in uk page to land or property address lookup when no selected")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          (srn, _: Max5000) => WhatYouWillNeedLandOrPropertyPage(srn),
          controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad
        )
        .withName("go from what you will need Land or Property page to LandProperty In UK page")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          LandOrPropertyWhenDidSchemeAcquirePage,
          (_, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from land or property when did scheme acquire page to unauthorised page")
    )
  }

  "WhyDoesSchemeHoldLandPropertyNavigator" - {

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          (srn, _: Max5000) => WhyDoesSchemeHoldLandPropertyPage(srn, index),
          Gen.const(Acquisition),
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.WhenDidSchemeAcquireController.onPageLoad(srn, index, mode)
        )
        .withName("why does scheme hold land property page to WhenDidSchemeAcquireController page on Acquisition")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          (srn, _: Max5000) => WhyDoesSchemeHoldLandPropertyPage(srn, index),
          Gen.const(Contribution),
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.WhenDidSchemeAcquireController.onPageLoad(srn, index, mode)
        )
        .withName("why does scheme hold land property page to WhenDidSchemeAcquireController page on Contribution")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          (srn, _: Max5000) => WhyDoesSchemeHoldLandPropertyPage(srn, index),
          Gen.const(Transfer),
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController.onPageLoad(srn, index, mode)
        )
        .withName("why does scheme hold land property page to WhenDidSchemeAcquireController page on Transfer")
    )
  }
}
