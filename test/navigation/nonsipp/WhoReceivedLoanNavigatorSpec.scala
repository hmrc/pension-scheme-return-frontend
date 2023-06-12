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
import models.{NormalMode, ReceivedLoanType}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.whoreceivedloan.WhoReceivedLoanPage
import utils.BaseSpec

class WhoReceivedLoanNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "WhoReceivedLoanNavigator" - {
    "NormalMode" - {
      act.like(
        normalmode
          .navigateToWithData(
            WhoReceivedLoanPage,
            Gen.const(ReceivedLoanType.Other),
            (srn, _) =>
              controllers.nonsipp.otherrecipientdetails.routes.OtherRecipientDetailsController
                .onPageLoad(srn, NormalMode)
          )
          .withName("go from who received loan page to other recipient details page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            WhoReceivedLoanPage,
            Gen.const(ReceivedLoanType.Individual),
            (srn, _) =>
              controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController
                .onPageLoad(srn, NormalMode)
          )
          .withName("go from who received loan page to individual recipient name page")
      )

      act.like(
        normalmode
          .navigateTo(
            WhoReceivedLoanPage,
            (_, _) => routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from who received loan page to unauthorised page")
      )
    }
  }
}
