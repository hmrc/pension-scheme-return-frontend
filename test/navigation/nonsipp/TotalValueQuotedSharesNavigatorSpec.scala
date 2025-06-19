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

import org.scalacheck.Gen
import navigation.{Navigator, NavigatorBehaviours}
import utils.BaseSpec
import pages.nonsipp.totalvaluequotedshares._

class TotalValueQuotedSharesNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "QuotedSharesManagedFundsHeldPage" - {
    act.like(
      normalmode
        .navigateToWithData(
          QuotedSharesManagedFundsHeldPage.apply,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesController.onPageLoad(srn)
        )
        .withName("go from quoted shares managed funds held page to total value quoted shares page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          QuotedSharesManagedFundsHeldPage.apply,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from quoted shares managed funds held page to task list page when no selected")
    )
  }

  "TotalValueQuotedSharesNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          TotalValueQuotedSharesPage.apply,
          moneyGen,
          (srn, _) =>
            controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
              .onPageLoad(srn)
        )
        .withName("go from total value quoted shares page to check your answers page")
    )
  }

  "TotalValueQuotedSharesCYAPage" - {
    act.like(
      normalmode
        .navigateTo(
          TotalValueQuotedSharesCYAPage.apply,
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName(
          "go from total value quoted shares CYA page to task list page"
        )
    )
  }

}
