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
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.otherassetsheld.OtherAssetsHeldPage
import utils.BaseSpec

class OtherAssetsHeldNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "OtherAssetsHeldNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          OtherAssetsHeldPage,
          Gen.const(true),
          (_, _) => routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from pension commencement lump sum page to pension payments received page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          OtherAssetsHeldPage,
          Gen.const(false),
          controllers.nonsipp.otherassets.routes.FeesCommissionsWagesSalariesController.onPageLoad
        )
        .withName("go from pension commencement lump sum page to unauthorised when no selected")
    )
  }
}
