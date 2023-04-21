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

import controllers.nonsipp.pensioncommencementlumpsum
import controllers.routes
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.transferout.SchemeTransferOutPage
import utils.BaseSpec

class TransferOutNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "TransferOutNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          SchemeTransferOutPage,
          Gen.const(true),
          (_, _) => routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from did scheme transfer out page to unauthorised page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          SchemeTransferOutPage,
          Gen.const(false),
          pensioncommencementlumpsum.routes.PensionCommencementLumpSumController.onPageLoad
        )
        .withName("go from did scheme transfer out page to pension commencement lump sum page when no selected")
    )
  }
}
