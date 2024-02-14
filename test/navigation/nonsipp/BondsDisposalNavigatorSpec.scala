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
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.bondsdisposal._
import utils.BaseSpec

class BondsDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)

  "BondsDisposalNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          BondsDisposalPage,
          Gen.const(true),
          (_, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from bank account page to members page when yes selected")
    )
  }
}
