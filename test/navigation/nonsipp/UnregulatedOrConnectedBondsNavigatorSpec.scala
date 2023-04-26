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

import controllers.routes
import controllers.nonsipp.receivetransfer
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.unregulatedorconnectedbonds.UnregulatedOrConnectedBondsHeldPage
import utils.BaseSpec

class UnregulatedOrConnectedBondsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "UnregulatedOrConnectedBondsNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          UnregulatedOrConnectedBondsHeldPage,
          Gen.const(true),
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController.onPageLoad
        )
        .withName("go from unregulated Or connected bonds held page to other assets held page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          UnregulatedOrConnectedBondsHeldPage,
          Gen.const(false),
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController.onPageLoad
        )
        .withName("go from unregulated Or connected bonds held page to other assets held page when no selected")
    )
  }
}
