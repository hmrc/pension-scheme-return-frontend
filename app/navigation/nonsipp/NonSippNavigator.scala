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

import com.google.inject.Singleton
import controllers.nonsipp
import controllers.nonsipp.routes
import controllers.nonsipp.schemedesignatory.FinancialDetailsCheckYourAnswersController
import eu.timepit.refined.refineMV
import models.PensionSchemeId.{PsaId, PspId}
import models.{NormalMode, UserAnswers}
import navigation.{JourneyNavigator, Navigator}
import pages.Page
import pages.nonsipp._
import pages.nonsipp.schemedesignatory.{
  FinancialDetailsCheckYourAnswersPage,
  HowManyMembersPage,
  HowMuchCashPage,
  ValueOfAssetsPage
}
import play.api.mvc.Call

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

      case HowMuchCashPage(srn) => nonsipp.schemedesignatory.routes.ValueOfAssetsController.onPageLoad(srn, NormalMode)
      case ValueOfAssetsPage(srn) =>
        nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController.onPageLoad(srn, NormalMode)
      case FinancialDetailsCheckYourAnswersPage(srn) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)

      case page @ HowManyMembersPage(srn, PsaId(_)) =>
        if (userAnswers.get(page).exists(_.total > 99)) {
          nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn)
        } else {
          controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
        }

      case page @ HowManyMembersPage(srn, PspId(_)) =>
        if (userAnswers.get(page).exists(_.total > 99)) {
          nonsipp.declaration.routes.PspDeclarationController.onPageLoad(srn)
        } else {
          controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
        }
    }

    override def checkRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

      case page @ CheckReturnDatesPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode)
        } else {
          nonsipp.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
        }

      case HowManyMembersPage(_, _) => controllers.routes.UnauthorisedController.onPageLoad()
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
      LoansMadeOrOutstandingNavigator,
      PensionCommencementLumpSumNavigator,
      SharesInSponsoringEmployerNavigator,
      PensionCommencementLumpSumNavigator,
      MoneyBorrowedNavigator,
      PensionPaymentsReceivedNavigator,
      BenefitsSurrenderedNavigator,
      OtherAssetsHeldNavigator,
      UnallocatedEmployerContributionsNavigator,
      SharesAcquiredFromConnectedPartyNavigator,
      UnquotedSharesNavigator,
      TotalValueQuotedSharesNavigator,
      OtherRecipientDetailsNavigator
    )

  override val defaultNormalMode: Call = controllers.routes.IndexController.onPageLoad()
  override val defaultCheckMode: Call = controllers.routes.IndexController.onPageLoad()
}
