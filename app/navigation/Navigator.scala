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

import config.Refined.{OneTo99, OneToThree}
import controllers.routes
import eu.timepit.refined.{refineMV, refineV}
import models.PensionSchemeId.{PsaId, PspId}
import models._
import pages.MembersDetails._
import pages._
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case StartPage(srn) => _ => routes.WhichTaxYearController.onPageLoad(srn, NormalMode)
    case WhichTaxYearPage(srn) => _ => routes.SchemeDetailsController.onPageLoad(srn)
    case SchemeDetailsPage(srn) => _ => routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

    case page @ CheckReturnDatesPage(srn) => {
      case ua if ua.get(page).contains(true) =>
        routes.HowManyMembersController.onPageLoad(srn, NormalMode)
      case _ => routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
    }

    case RemoveMemberDetailsPage(srn) => _ => routes.SchemeMembersListController.onPageLoad(srn, page = 1)

    case AccountingPeriodPage(srn, index) =>
      _ => routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, index)
    case AccountingPeriodCheckYourAnswersPage(srn) =>
      _ => routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)

    case AccountingPeriodListPage(srn, false) =>
      _ => routes.HowManyMembersController.onPageLoad(srn, NormalMode)

    case AccountingPeriodListPage(srn, true) =>
      ua =>
        val count = ua.list(AccountingPeriods(srn)).length
        refineV[OneToThree](count + 1).fold(
          _ => routes.HowManyMembersController.onPageLoad(srn, NormalMode),
          index => routes.AccountingPeriodController.onPageLoad(srn, index, NormalMode)
        )

    case RemoveAccountingPeriodPage(srn) => _ => routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)

    case HowMuchCashPage(srn) => _ => routes.ValueOfAssetsController.onPageLoad(srn, NormalMode)
    case ValueOfAssetsPage(srn) => _ => routes.PensionSchemeMembersController.onPageLoad(srn)

    case PensionSchemeMembersPage(srn) =>
      ua =>
        if (ua.get(PensionSchemeMembersPage(srn)).contains(ManualOrUpload.Manual)) {
          routes.MemberDetailsController.onPageLoad(srn, refineMV(1))
        } else {
          routes.UnauthorisedController.onPageLoad()
        }

    case MemberDetailsPage(srn, index) =>
      _ => routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, NormalMode)
    case page @ NationalInsuranceNumberPage(srn, index) => {
      case ua if ua.get(page).contains(true) => routes.MemberDetailsNinoController.onPageLoad(srn, index, NormalMode)
      case _ => routes.NoNINOController.onPageLoad(srn, index, NormalMode)
    }
    case MemberDetailsNinoPage(srn, index) =>
      _ => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckOrChange.Check)
    case SchemeMemberDetailsAnswersPage(srn) => _ => routes.SchemeMembersListController.onPageLoad(srn, page = 1)
    case PsaDeclarationPage(srn) => _ => routes.UnauthorisedController.onPageLoad()
    case PspDeclarationPage(srn) => _ => routes.UnauthorisedController.onPageLoad()

    case page @ HowManyMembersPage(srn, PsaId(_)) => {
      case ua if ua.get(page).exists(_.total > 99) => routes.PsaDeclarationController.onPageLoad(srn)
      case _ => routes.HowMuchCashController.onPageLoad(srn, NormalMode)
    }

    case page @ HowManyMembersPage(srn, PspId(_)) => {
      case ua if ua.get(page).exists(_.total > 99) => routes.PspDeclarationController.onPageLoad(srn)
      case _ => routes.HowMuchCashController.onPageLoad(srn, NormalMode)
    }

    case NoNINOPage(srn, index) =>
      _ => routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckOrChange.Check)
    case SchemeMembersListPage(srn, false) => _ => routes.UnauthorisedController.onPageLoad()

    case SchemeMembersListPage(srn, true) =>
      ua =>
        refineV[OneTo99](ua.membersDetails(srn).length + 1).fold(
          _ => routes.JourneyRecoveryController.onPageLoad(),
          index => routes.MemberDetailsController.onPageLoad(srn, index)
        )
    case _ => _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case CheckReturnDatesPage(srn) => _ => routes.UnauthorisedController.onPageLoad()

    case page @ HowManyMembersPage(srn, _) => {
      case _ => routes.UnauthorisedController.onPageLoad()
    }

    case NoNINOPage(srn, _) => _ => routes.UnauthorisedController.onPageLoad()

    case _ => _ => routes.IndexController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
