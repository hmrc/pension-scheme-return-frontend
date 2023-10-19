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

import controllers.nonsipp
import controllers.nonsipp.schemedesignatory.routes
import models.{CheckMode, NormalMode, UserAnswers}
import navigation.JourneyNavigator
import pages.Page
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, WhyNoBankAccountPage}
import play.api.mvc.Call

object BankAccountNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ ActiveBankAccountPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        routes.HowManyMembersController.onPageLoad(srn, NormalMode)
      } else {
        nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode)
      }

    case WhyNoBankAccountPage(srn) => routes.HowManyMembersController.onPageLoad(srn, NormalMode)
  }

  override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ ActiveBankAccountPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        routes.HowManyMembersController.onPageLoad(srn, CheckMode)
      } else {
        nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, CheckMode)
      }

    case WhyNoBankAccountPage(srn) =>
      controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode)
  }
}
