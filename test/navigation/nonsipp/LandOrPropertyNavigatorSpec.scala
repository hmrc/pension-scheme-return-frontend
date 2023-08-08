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

import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.landorproperty.{
  LandOrPropertyHeldPage,
  LandPropertyInUKPage,
  WhatYouWillNeedLandOrPropertyPage,
  WhyDoesSchemeHoldLandPropertyPage
}
import utils.BaseSpec

class LandOrPropertyNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

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
        .navigateToWithData(
          LandPropertyInUKPage,
          Gen.const(true),
          controllers.nonsipp.landorproperty.routes.WhyDoesSchemeHoldLandPropertyController.onPageLoad
        )
        .withName("go from land or property in uk page to unauthorised when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          LandPropertyInUKPage,
          Gen.const(false),
          (_, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from land or property in uk page to unauthorised when no selected")
    )
  }

  "WhatYouWillNeedNavigator" - {

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedLandOrPropertyPage,
          controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad
        )
        .withName("go from what you will need Land or Property page to Unauthorised page")
    )
  }

  "WhyDoesSchemeHoldLandPropertyNavigator" - {

    act.like(
      normalmode
        .navigateTo(
          WhyDoesSchemeHoldLandPropertyPage,
          (_, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from why does scheme hold land property page to unauthorised page")
    )
  }
}
