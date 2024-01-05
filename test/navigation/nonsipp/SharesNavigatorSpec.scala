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

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import models.NormalMode
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.shares.{DidSchemeHoldAnySharesPage, WhatYouWillNeedSharesPage}
import utils.BaseSpec

class SharesNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)

  "SharesNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeHoldAnySharesPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName(
          "go from scheme hold any shares to task list page when no is selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeHoldAnySharesPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.shares.routes.WhatYouWillNeedSharesController.onPageLoad(srn)
        )
        .withName(
          "go from scheme hold any shares to what you will need page when yes is selected"
        )
    )

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedSharesPage,
          (srn, _) => controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, index, NormalMode)
        )
        .withName(
          "go from WhatYouWillNeedSharesPage to type of shares page"
        )
    )

  }
}
