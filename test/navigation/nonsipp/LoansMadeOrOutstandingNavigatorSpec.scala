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

import controllers.routes
import models.{NormalMode, ReceivedLoanType}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.loansmadeoroutstanding._
import utils.BaseSpec

class LoansMadeOrOutstandingNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

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
        .navigateTo(
          IndividualRecipientNamePage,
          controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNinoController.onPageLoad
        )
        .withName("go from Individual recipient name page to individual nino page")
    )

    act.like(
      normalmode
        .navigateTo(
          IndividualRecipientNinoPage,
          controllers.nonsipp.routes.IsMemberOrConnectedPartyController.onPageLoad
        )
        .withName("go from individual recipient nino page to is member or connected party page")
    )

    "WhatYouWillNeedNavigator" - {

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedLoansPage,
            (srn, _) => controllers.nonsipp.whoreceivedloan.routes.WhoReceivedLoanController.onPageLoad(srn)
          )
          .withName("go from what you will need loans page to who received loan page")
      )
    }

    act.like(
      normalmode
        .navigateTo(
          RecipientSponsoringEmployerConnectedPartyPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.DatePeriodLoanController.onPageLoad
        )
        .withName("go from sponsoring employer or connected party page to date period loan page")
    )

    act.like(
      normalmode
        .navigateTo(
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
          .navigateToWithData(
            WhoReceivedLoanPage,
            Gen.const(ReceivedLoanType.Other),
            (srn, _) =>
              controllers.nonsipp.otherrecipientdetails.routes.OtherRecipientDetailsController
                .onPageLoad(srn, NormalMode)
          )
          .withName("go from who received loan page to other recipient details page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            WhoReceivedLoanPage,
            Gen.const(ReceivedLoanType.Individual),
            (srn, _) =>
              controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController
                .onPageLoad(srn, NormalMode)
          )
          .withName("go from who received loan page to individual recipient name page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            WhoReceivedLoanPage,
            Gen.const(ReceivedLoanType.UKCompany),
            controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientNameController.onPageLoad
          )
          .withName("go from who received loan page to company recipient name page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            WhoReceivedLoanPage,
            Gen.const(ReceivedLoanType.UKPartnership),
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
          .navigateTo(
            CompanyRecipientNamePage,
            controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientCrnController.onPageLoad
          )
          .withName("go from company recipient name page to company crn page")
      )

      act.like(
        normalmode
          .navigateTo(
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
        .navigateTo(
          PartnershipRecipientNamePage,
          controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientUtrController.onPageLoad
        )
        .withName("go from partnership recipient name page to partnership recipient Utr page")
    )
  }

  "PartnershipRecipientUtrPage" - {
    act.like(
      normalmode
        .navigateTo(
          PartnershipRecipientUtrPage,
          controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad
        )
        .withName("go from partnership recipient Utr page to sponsoring employer or connected party page")
    )
  }
}
