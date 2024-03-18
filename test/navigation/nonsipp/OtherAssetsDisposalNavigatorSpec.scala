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

package navigation.nonsipp

import config.Refined.{Max50, Max5000}
import eu.timepit.refined.refineMV
import models.HowDisposed
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.otherassetsdisposal._
import utils.BaseSpec

class OtherAssetsDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  override val navigator: Navigator = new NonSippNavigator

  private val assetIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  "OtherAssetsDisposalNavigator" - {

    "OtherAssetsDisposalPage" - {
      act.like(
        normalmode
          .navigateToWithData(
            OtherAssetsDisposalPage,
            Gen.const(true),
            (srn, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.WhatYouWillNeedOtherAssetsDisposalController
                .onPageLoad(srn)
          )
          .withName(
            "go from Did the scheme dispose of any other assets page to WhatYouWillNeedOtherAssetsDisposal page" +
              " when yes is selected"
          )
      )

      act.like(
        normalmode
          .navigateToWithData(
            OtherAssetsDisposalPage,
            Gen.const(false),
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName(
            "go from Did the scheme dispose of any other assets page to taskList page" +
              " when no is selected"
          )
      )

    }

    "WhatYouWillNeedOtherAssetsDisposalPage" - {
      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedOtherAssetsDisposalPage,
            (_, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName(
            "go from WhatYouWillNeedOtherAssetsDisposal page to unauthorised page"
          )
      )
    }

    "HowWasAssetDisposedOfPage" - {

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Sold),
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from HowWasAssetDisposedOfPage to Unauthorised page")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Transferred),
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from HowWasAssetDisposedOfPage to Unauthorised page (Transferred)")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Other("test details")),
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from HowWasAssetDisposedOfPage to Unauthorised page (Other)")
      )
    }

  }
}
