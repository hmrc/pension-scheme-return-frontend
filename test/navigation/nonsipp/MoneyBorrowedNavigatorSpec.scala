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
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import utils.BaseSpec

class MoneyBorrowedNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "MoneyBorrowedNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          MoneyBorrowedPage,
          Gen.const(true),
          (_, _) => routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from money borrowed page to unauthorised when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          MoneyBorrowedPage,
          Gen.const(false),
          (_, _) => routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from money borrowed page to unauthorised when no selected")
    )
  }
}
