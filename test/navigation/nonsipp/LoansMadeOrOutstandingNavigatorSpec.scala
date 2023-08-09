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

import config.Refined.OneTo9999999
import eu.timepit.refined.refineMV
import models.{IdentitySubject, IdentityType, NormalMode}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding._
import utils.BaseSpec

class LoansMadeOrOutstandingNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[OneTo9999999](1)
  private val subject = IdentitySubject.LoanRecipient

  "loansMadeOrOutstandingNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          LoansMadeOrOutstandingPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.loansmadeoroutstanding.routes.WhatYouWillNeedLoansController.onPageLoad(srn)
        )
        .withName("go from loan made or outstanding page to what you will need Loans page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          LoansMadeOrOutstandingPage,
          Gen.const(false),
          controllers.nonsipp.sharesinsponsoringemployer.routes.DidSchemeHoldSharesInSponsoringEmployerController.onPageLoad
        )
        .withName(
          "go from loan made or outstanding page to did scheme hold shares in sponsoring employer page when no selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          IndividualRecipientNamePage,
          controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNinoController.onPageLoad
        )
        .withName("go from Individual recipient name page to individual nino page")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          IndividualRecipientNinoPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.IsMemberOrConnectedPartyController.onPageLoad
        )
        .withName("go from individual recipient nino page to is member or connected party page")
    )

    "WhatYouWillNeedNavigator" - {

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedLoansPage,
            controllers.nonsipp.common.routes.IdentityTypeController
              .onPageLoad(_, index, _, IdentitySubject.LoanRecipient)
          )
          .withName("go from what you will need loans page to who received loan page")
      )
    }

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          RecipientSponsoringEmployerConnectedPartyPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.DatePeriodLoanController.onPageLoad
        )
        .withName("go from sponsoring employer or connected party page to date period loan page")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          DatePeriodLoanPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.AmountOfTheLoanController.onPageLoad
        )
        .withName("go from date period loan page to amount of loan page")
    )
  }

  "WhoReceivedLoanNavigator" - {
    "NormalMode" - {
      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.Other),
            controllers.nonsipp.loansmadeoroutstanding.routes.OtherRecipientDetailsController.onPageLoad
          )
          .withName("go from who received loan page to other recipient details page")
      )

      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.Individual),
            controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController.onPageLoad
          )
          .withName("go from who received loan page to individual recipient name page")
      )

      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.UKCompany),
            controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientNameController.onPageLoad
          )
          .withName("go from who received loan page to company recipient name page")
      )

      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.UKPartnership),
            controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientNameController.onPageLoad
          )
          .withName("go from who received loan page UKPartnership to partnership recipient name page")
      )
    }
  }

  "CompanyRecipientJourney" - {
    "NormalMode" - {

      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CompanyRecipientNamePage,
            controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientCrnController.onPageLoad
          )
          .withName("go from company recipient name page to company crn page")
      )

      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CompanyRecipientCrnPage,
            controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad
          )
          .withName("go from company recipient CRN page to sponsoring employer or connected party page")
      )
    }
  }

  "PartnershipRecipientNamePage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          PartnershipRecipientNamePage,
          controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientUtrController.onPageLoad
        )
        .withName("go from partnership recipient name page to partnership recipient Utr page")
    )
  }

  "PartnershipRecipientUtrPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          PartnershipRecipientUtrPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad
        )
        .withName("go from partnership recipient Utr page to sponsoring employer or connected party page")
    )
  }

  "AmountOfTheLoanPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          AmountOfTheLoanPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.AreRepaymentsInstalmentsController.onPageLoad
        )
        .withName("go from amount of the loan page to are repayments instalments page")
    )
  }

  "AreRepaymentsInstalmentsPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          AreRepaymentsInstalmentsPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.InterestOnLoanController.onPageLoad
        )
        .withName("go from are repayments instalments page to interest on loan page")
    )
  }

  "InterestOnLoanPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          InterestOnLoanPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.SecurityGivenForLoanController.onPageLoad
        )
        .withName("go from interest on loan page to security given for loan page")
    )
  }

  "SecurityGivenForLoan" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          SecurityGivenForLoanPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.OutstandingArrearsOnLoanController.onPageLoad
        )
        .withName("go from interest on loan page to outstanding arrears on loan page")
    )
  }

  "RemoveLoan" - {
    act.like(
      normalmode
        .navigateTo(
          srn => RemoveLoanPage(srn, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
              .onPageLoad(srn, NormalMode)
        )
        .withName("go from remove page to list page")
    )

  }
}
