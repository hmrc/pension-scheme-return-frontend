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

import pages.nonsipp.totalvaluequotedshares._
import play.api.mvc.Call
import pages.Page
import navigation.JourneyNavigator
import models.{NormalMode, UserAnswers}

object TotalValueQuotedSharesNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case page @ QuotedSharesManagedFundsHeldPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      }

    case TotalValueQuotedSharesPage(srn) =>
      controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController.onPageLoad(srn)

    case TotalValueQuotedSharesCYAPage(srn) =>
      controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

    case RemoveTotalValueQuotedSharesPage(srn) =>
      if (userAnswers.get(TotalValueQuotedSharesPage(srn)).isEmpty) {
        controllers.nonsipp.totalvaluequotedshares.routes.QuotedSharesManagedFundsHeldController
          .onPageLoad(srn, NormalMode)
      } else {
        controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
          .onPageLoad(srn)
      }
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ => _ => PartialFunction.empty

}
