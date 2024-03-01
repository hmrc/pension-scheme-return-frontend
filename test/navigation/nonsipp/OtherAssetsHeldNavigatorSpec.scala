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

import config.Refined.{Max5000, OneTo5000}
import controllers.routes
import eu.timepit.refined.refineMV
import models.NormalMode
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.otherassetsheld._
import utils.BaseSpec

class OtherAssetsHeldNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)

  "OtherAssetsHeldNavigator" - {

    "OtherAssetsHeldPage" - {
      act.like(
        normalmode
          .navigateToWithData(
            OtherAssetsHeldPage,
            Gen.const(true),
            (srn, _) => controllers.nonsipp.otherassetsheld.routes.WhatYouWillNeedOtherAssetsController.onPageLoad(srn)
          )
          .withName("go from other assets held page to WhatYouWillNeedOtherAssets page when yes selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            OtherAssetsHeldPage,
            Gen.const(false),
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from other assets held page to task list page when no selected")
      )
    }

    "WhatYouWillNeedOtherAssetsPage" - {
      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedOtherAssetsPage,
            (srn, _) =>
              controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from WhatYouWillNeedOtherAssets page to WhatIsOtherAsset page"
          )
      )
    }

    "WhatIsOtherAssetPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhatIsOtherAssetPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.otherassetsheld.routes.IsAssetTangibleMoveablePropertyController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from WhatIsOtherAsset page to IsAssetTangibleMoveableProperty page"
          )
      )
    }

    "IsAssetTangibleMoveablePropertyPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            IsAssetTangibleMoveablePropertyPage,
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName(
            "go from IsAssetTangibleMoveablePropertyPage to Unauthorised"
          )
      )
    }
  }
}
