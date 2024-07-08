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

import pages.nonsipp.schemedesignatory._
import play.api.mvc.Call
import com.google.inject.Singleton
import pages.Page
import controllers.nonsipp
import controllers.nonsipp.routes
import eu.timepit.refined.refineMV
import models.PensionSchemeId.{PsaId, PspId}
import models.{CheckMode, NormalMode, UserAnswers}
import pages.nonsipp._
import navigation.{JourneyNavigator, Navigator}

import javax.inject.Inject

@Singleton
class NonSippNavigator @Inject()() extends Navigator {

  val nonSippNavigator: JourneyNavigator = new JourneyNavigator {
    override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

      case WhichTaxYearPage(srn) => routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

      case page @ CheckReturnDatesPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode)
        } else {
          nonsipp.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
        }

      case HowMuchCashPage(srn, mode) => nonsipp.schemedesignatory.routes.ValueOfAssetsController.onPageLoad(srn, mode)
      case ValueOfAssetsPage(srn, mode) =>
        nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController.onPageLoad(srn, mode)
      case FinancialDetailsCheckYourAnswersPage(srn) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

      case page @ HowManyMembersPage(srn, PsaId(_)) =>
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)

      case page @ HowManyMembersPage(srn, PspId(_)) =>
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
    }

    override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
      _ =>
        userAnswers => {

          case page @ CheckReturnDatesPage(srn) =>
            if (userAnswers.get(page).contains(true)) {
              nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode)
            } else {
              nonsipp.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
            }

          case page @ HowManyMembersPage(srn, PsaId(_)) =>
            controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode)

          case page @ HowManyMembersPage(srn, PspId(_)) =>
            controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode)

          case HowMuchCashPage(srn, mode) =>
            nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController.onPageLoad(srn, mode)
          case ValueOfAssetsPage(srn, mode) =>
            nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController.onPageLoad(srn, mode)
          case FinancialDetailsCheckYourAnswersPage(srn) =>
            controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
              .onPageLoad(srn, NormalMode)
        }
  }

  val journeys: List[JourneyNavigator] =
    List(
      AccountingPeriodNavigator,
      BankAccountNavigator,
      DeclarationNavigator,
      EmployerContributionsNavigator,
      MemberDetailsNavigator,
      nonSippNavigator,
      MemberContributionsNavigator,
      ReceiveTransferNavigator,
      TransferOutNavigator,
      OtherAssetsNavigator,
      UnregulatedOrConnectedBondsNavigator,
      LandOrPropertyNavigator,
      LandOrPropertyDisposalNavigator,
      LoansMadeOrOutstandingNavigator,
      PensionCommencementLumpSumNavigator,
      SharesNavigator,
      SharesDisposalNavigator,
      MoneyBorrowedNavigator,
      PensionPaymentsReceivedNavigator,
      SurrenderedBenefitsNavigator,
      OtherAssetsHeldNavigator,
      OtherAssetsDisposalNavigator,
      UnallocatedEmployerContributionsNavigator,
      TotalValueQuotedSharesNavigator,
      BondsDisposalNavigator
    )

  override val defaultNormalMode: Call = controllers.routes.IndexController.onPageLoad()
  override val defaultCheckMode: Call = controllers.routes.IndexController.onPageLoad()
}
