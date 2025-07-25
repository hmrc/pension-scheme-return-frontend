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

import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, WhyNoBankAccountPage}
import utils.BaseSpec
import controllers.nonsipp.schemedesignatory.routes
import controllers.nonsipp.schemedesignatory
import org.scalacheck.Gen
import navigation.{Navigator, NavigatorBehaviours}

class BankAccountNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "BankAccountNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          ActiveBankAccountPage.apply,
          Gen.const(true),
          routes.HowManyMembersController.onPageLoad
        )
        .withName("go from bank account page to members page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          ActiveBankAccountPage.apply,
          Gen.const(false),
          schemedesignatory.routes.WhyNoBankAccountController.onPageLoad
        )
        .withName("go from bank account page to no bank account page when no selected")
    )

    act.like(
      normalmode.navigateTo(
        WhyNoBankAccountPage.apply,
        routes.HowManyMembersController.onPageLoad
      )
    )
  }
}
