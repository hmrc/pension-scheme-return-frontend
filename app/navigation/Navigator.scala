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

package navigation

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import pages.{CheckReturnDatesPage, _}
import models._

@Singleton
class Navigator @Inject()() {



  private val normalRoutes: Page => UserAnswers => Call = {
    case StartPage(srn)             => _ => routes.SchemeDetailsController.onPageLoad(srn)
    case SchemeDetailsPage(srn)     => _ => routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

    case page@CheckReturnDatesPage(srn)  => ua =>
      if(ua.get(page).contains(true))
        routes.SchemeBankAccountController.onPageLoad(srn, NormalMode)
      else
        routes.UnauthorisedController.onPageLoad
        
    case SchemeBankAccountPage(srn) => _ => routes.UnauthorisedController.onPageLoad
    case _                          => _ => routes.IndexController.onPageLoad
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case CheckReturnDatesPage(srn)  => _ => routes.UnauthorisedController.onPageLoad
    case SchemeBankAccountPage(srn) => _ => routes.UnauthorisedController.onPageLoad
    case _                          => _ => routes.IndexController.onPageLoad
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
