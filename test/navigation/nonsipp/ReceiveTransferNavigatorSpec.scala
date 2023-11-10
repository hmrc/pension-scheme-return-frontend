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

import controllers.nonsipp.memberpayments
import controllers.routes
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.memberpayments.DidSchemeReceiveTransferPage
import utils.BaseSpec

class ReceiveTransferNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "ReceiveTransferNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeReceiveTransferPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.memberpayments.routes.WYWNeedReceivedTransferController.onPageLoad(srn)
        )
        .withName("go from did scheme receive transfer page to unauthorised page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeReceiveTransferPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from did scheme receive transfer page to scheme transfer out page when no selected")
    )
  }
}
