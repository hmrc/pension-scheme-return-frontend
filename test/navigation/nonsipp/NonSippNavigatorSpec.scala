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
import controllers.nonsipp.accountingperiod
import controllers.nonsipp.schemedesignatory
import controllers.nonsipp.declaration
import controllers.nonsipp.memberdetails
import controllers.nonsipp.routes
import eu.timepit.refined.refineMV
import models.{NormalMode, SchemeMemberNumbers}
import navigation.{Navigator, NavigatorBehaviours, UnknownPage}
import org.scalacheck.Gen
import pages.nonsipp.schemedesignatory.{HowManyMembersPage, HowMuchCashPage, ValueOfAssetsPage}
import pages.nonsipp.{CheckReturnDatesPage, FinancialDetailsCheckYourAnswersPage, WhichTaxYearPage}
import utils.BaseSpec

class NonSippNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  "NonSippNavigator" - {

    "NormalMode" - {
      act.like(
        normalmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad())
          .withName("redirect any unknown pages to index page")
      )

      act.like(
        normalmode
          .navigateTo(
            WhichTaxYearPage,
            routes.CheckReturnDatesController.onPageLoad
          )
          .withName("go from which tax year page to check return dates page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckReturnDatesPage,
            Gen.const(true),
            schemedesignatory.routes.ActiveBankAccountController.onPageLoad
          )
          .withName("go from check return dates page to bank account page when yes selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckReturnDatesPage,
            Gen.const(false),
            accountingperiod.routes.AccountingPeriodController.onPageLoad(_, refineMV(1), _)
          )
          .withName("go from check return dates page to accounting period page when no is selected")
      )

      act.like(
        normalmode
          .navigateTo(
            HowMuchCashPage,
            schemedesignatory.routes.ValueOfAssetsController.onPageLoad
          )
          .withName("go from how much cash page to value of assets page")
      )

      act.like(
        normalmode
          .navigateTo(
            ValueOfAssetsPage,
            nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController.onPageLoad
          )
          .withName("go from value of assets page to fees commissions wages salaries page")
      )

      act.like(
        normalmode
          .navigateTo(
            FinancialDetailsCheckYourAnswersPage,
            (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from financial details check your answers page to task list page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            HowManyMembersPage(_, pensionSchemeIdGen.sample.value),
            Gen.chooseNum(1, 99).map(SchemeMemberNumbers(_, 0, 0)),
            routes.BasicDetailsCheckYourAnswersController.onPageLoad
          )
          .withName("go from how many members page to check your answers page when less than 100 members")
      )

      act.like(
        normalmode
          .navigateToWithData(
            HowManyMembersPage(_, psaIdGen.sample.value),
            Gen.chooseNum(100, 1000).map(SchemeMemberNumbers(_, 0, 0)),
            (srn, _) => declaration.routes.PsaDeclarationController.onPageLoad(srn)
          )
          .withName("go from how many members to psa declaration when more than 99 members and psa signed in")
      )

      act.like(
        normalmode
          .navigateToWithData(
            HowManyMembersPage(_, pspIdGen.sample.value),
            Gen.chooseNum(100, 1000).map(SchemeMemberNumbers(_, 0, 0)),
            (srn, _) => declaration.routes.PspDeclarationController.onPageLoad(srn)
          )
          .withName("go from how many members to psp declaration when more than 99 members and psp signed in")
      )
    }

    "CheckMode" - {

      act.like(
        checkmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad())
          .withName("redirect any unknown pages to index page")
      )
    }
  }
}
