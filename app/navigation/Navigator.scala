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

import config.Refined.OneToTen
import config.Refined.OneToThree
import controllers.routes
import eu.timepit.refined.{refineMV, refineV}
import models._
import pages.SchemeBankAccounts.SchemeBankAccountsOps
import pages._
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case StartPage(srn)             => _ => routes.SchemeDetailsController.onPageLoad(srn)
    case SchemeDetailsPage(srn)     => _ => routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

    case page @ CheckReturnDatesPage(srn) => {
      case ua if ua.get(page).contains(true) => ua.schemeBankAccounts(srn) match {
        case Nil => routes.SchemeBankAccountController.onPageLoad(srn, refineMV(1), NormalMode)
        case _   => routes.SchemeBankAccountListController.onPageLoad(srn)
      }
      case _ => routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
    }

    case SchemeBankAccountPage(srn, index) => _ => routes.SchemeBankAccountCheckYourAnswersController.onPageLoad(srn, index)
    case SchemeBankAccountCheckYourAnswersPage(srn) => _ =>
      routes.SchemeBankAccountListController.onPageLoad(srn)
    case SchemeBankAccountListPage(srn, true) => ua =>
      refineV[OneToTen](ua.schemeBankAccounts(srn).length + 1) match {
        case Left(_)          => routes.SchemeBankAccountListController.onPageLoad(srn)
        case Right(nextIndex) => routes.SchemeBankAccountController.onPageLoad(srn, nextIndex, NormalMode)
      }
    case SchemeBankAccountListPage(_, false) => _ => routes.UnauthorisedController.onPageLoad

    case AccountingPeriodPage(srn, _) => _ => routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)
    case AccountingPeriodListPage(srn, false) => _ =>
      routes.SchemeBankAccountController.onPageLoad(srn, refineMV(1), NormalMode)
    case AccountingPeriodListPage(srn, true) => ua =>
      val count = ua.list(AccountingPeriods(srn)).length
      refineV[OneToThree](count + 1).fold(
        _     => routes.SchemeBankAccountController.onPageLoad(srn, refineMV(1), NormalMode),
        index => routes.AccountingPeriodController.onPageLoad(srn, index, NormalMode)
      )

    case RemoveSchemeBankAccountPage(srn) => _ => routes.SchemeBankAccountListController.onPageLoad(srn)

    case _              => _ => routes.IndexController.onPageLoad
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case CheckReturnDatesPage(srn)     => _ => routes.UnauthorisedController.onPageLoad
    case SchemeBankAccountPage(srn, _) => _ => routes.UnauthorisedController.onPageLoad
    case RemoveSchemeBankAccountPage(srn) => _ => routes.SchemeBankAccountListController.onPageLoad(srn)
    case _                             => _ => routes.IndexController.onPageLoad
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
