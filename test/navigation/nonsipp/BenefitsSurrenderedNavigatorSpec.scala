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
import pages.nonsipp.memberpayments.BenefitsSurrenderedPage
import utils.BaseSpec

class BenefitsSurrenderedNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "BenefitsSurrenderedNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          BenefitsSurrenderedPage,
          Gen.const(true),
          (_, _) => routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from benefits surrendered page to unauthorised page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          BenefitsSurrenderedPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from benefits surrendered page to task list page  when no selected")
    )

  }
}
