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

import play.api.mvc.Call
import pages.Page
import navigation.JourneyNavigator
import models.UserAnswers
import pages.nonsipp.declaration.{PsaDeclarationPage, PspDeclarationPage}

object DeclarationNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = _ => {
    case PsaDeclarationPage(srn) =>
      controllers.nonsipp.routes.ReturnSubmittedController.onPageLoad(srn)
    case PspDeclarationPage(srn) =>
      controllers.nonsipp.routes.ReturnSubmittedController.onPageLoad(srn)
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ => _ => PartialFunction.empty
}
