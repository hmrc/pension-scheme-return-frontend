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

import controllers.nonsipp.routes
import models._
import pages._
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class RootNavigator @Inject()() extends Navigator {

  val journeys: List[JourneyNavigator] =
    List(new JourneyNavigator {
      override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = _ => {
        case WhatYouWillNeedPage(srn) => routes.WhichTaxYearController.onPageLoad(srn, NormalMode)
      }

      override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = _ => PartialFunction.empty
    })

  override def defaultNormalMode: Call = controllers.routes.IndexController.onPageLoad()

  override def defaultCheckMode: Call = controllers.routes.IndexController.onPageLoad()
}

trait Navigator {
  def journeys: List[JourneyNavigator]
  def defaultNormalMode: Call
  def defaultCheckMode: Call

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {

    case NormalMode =>
      journeys
        .foldLeft(PartialFunction.empty[Page, Call])((acc, curr) => acc.orElse(curr.normalRoutes(userAnswers)))
        .lift(page)
        .getOrElse(defaultNormalMode)

    case CheckMode =>
      journeys
        .foldLeft(PartialFunction.empty[Page, Call])((acc, curr) => acc.orElse(curr.checkRoutes(userAnswers)))
        .lift(page)
        .getOrElse(defaultCheckMode)
  }
}
