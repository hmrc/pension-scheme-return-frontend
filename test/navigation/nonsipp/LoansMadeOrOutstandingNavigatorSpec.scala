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

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import models.CheckOrChange.Check
import models.ConditionalYesNo._
import models.SchemeId.Srn
import models.{ConditionalYesNo, IdentitySubject, IdentityType, Money, NormalMode, RecipientDetails, UserAnswers}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.loansmadeoroutstanding._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class LoansMadeOrOutstandingNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[OneTo5000](1)
  private val subject = IdentitySubject.LoanRecipient

  "loansMadeOrOutstandingNavigator in normal mode" - {

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
          controllers.nonsipp.loansmadeoroutstanding.routes.IsIndividualRecipientConnectedPartyController.onPageLoad
        )
        .withName("go from individual recipient nino page to is individual recipient connected party page")
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

      val completedLoanUserAnswers: Srn => UserAnswers =
        srn =>
          defaultUserAnswers.unsafeSet(
            OutstandingArrearsOnLoanPage(srn, refineMV(1)),
            ConditionalYesNo.no[Unit, Money](())
          )

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedLoansPage,
            (srn, mode) =>
              controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController.onPageLoad(srn, 1, mode),
            completedLoanUserAnswers
          )
          .withName("go from what you will need loans page to loans list page when a loan has already been completed")
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
          .navigateToWithDataIndexAndSubjectBoth(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.Other),
            controllers.nonsipp.common.routes.OtherRecipientDetailsController.onPageLoad
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
          .navigateToWithDataIndexAndSubjects(
            index,
            subject,
            CompanyRecipientNamePage,
            Gen.const(""),
            controllers.nonsipp.common.routes.CompanyRecipientCrnController.onPageLoad
          )
          .withName("go from company recipient name page to company crn page")
      )

      act.like(
        normalmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
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
        .navigateToWithDataIndexAndSubjects(
          index,
          subject,
          PartnershipRecipientNamePage,
          Gen.const(""),
          controllers.nonsipp.common.routes.PartnershipRecipientUtrController.onPageLoad
        )
        .withName("go from partnership recipient name page to partnership recipient Utr page")
    )
  }

  "PartnershipRecipientUtrPage" - {
    act.like(
      normalmode
        .navigateToWithIndexAndSubject(
          index,
          subject,
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

  "AddLoan" - {
    "One record at index 1" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LoansListPage(srn, true),
            (srn, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, refineMV(2), NormalMode, IdentitySubject.LoanRecipient),
            (srn) =>
              defaultUserAnswers
                .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.Individual)
                .unsafeSet(IndividualRecipientNamePage(srn, refineMV(1)), individualName)
          )
          .withName("go to who received the loan at index 2")
      )
    }
    "One record at index 2" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LoansListPage(srn, true),
            (srn, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, refineMV(3), NormalMode, IdentitySubject.LoanRecipient),
            (srn) =>
              defaultUserAnswers
                .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.Individual)
                .unsafeSet(IndividualRecipientNamePage(srn, refineMV(2)), individualName)
          )
          .withName("go to who received the loan at index 3")
      )
    }
    "No existing loan records" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LoansListPage(srn, true),
            (srn, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, refineMV(2), NormalMode, IdentitySubject.LoanRecipient),
            (srn) => defaultUserAnswers
          )
          .withName("go to who received the loan at index 1")
      )
    }
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

  "otherRecipientsDetailsPage" - {

    val recipientDetails = RecipientDetails(
      "testName",
      "testDescription"
    )

    act.like(
      normalmode
        .navigateToWithDataIndexAndSubject(
          index,
          subject,
          OtherRecipientDetailsPage,
          Gen.const(recipientDetails),
          controllers.nonsipp.loansmadeoroutstanding.routes.RecipientSponsoringEmployerConnectedPartyController.onPageLoad
        )
        .withName("go from other recipient details page to recipient connected party page")
    )
  }

  "loansMadeOrOutstandingNavigator in check mode" - {

    /* TODO - updatedUserAnswer doesn't seem taking the value of CompanyRecipientNamePage */

    val updatedUserAnswer =
      (srn: Srn) => defaultUserAnswers.unsafeSet(CompanyRecipientNamePage(srn, refineMV(1)), "recipientName")

    act.like(
      checkmode
        .navigateToWithData(
          IdentityTypePage(_, refineMV(1), IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.UKCompany),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check),
          updatedUserAnswer
        )
        .withName("go from IdentityType page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          IndividualRecipientNamePage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Individual Recipient Name page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          CompanyRecipientNamePage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Company Recipient Name page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          PartnershipRecipientNamePage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Partnership Recipient Name page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          OtherRecipientDetailsPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Other Recipient Details page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          IndividualRecipientNinoPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Individual Recipient Nino page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          CompanyRecipientCrnPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Company Recipient Crn page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          PartnershipRecipientUtrPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Partnership Recipient Utr page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          IsIndividualRecipientConnectedPartyPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Is Individual Recipient Connected Party page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          RecipientSponsoringEmployerConnectedPartyPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Recipient Sponsoring Employer Connected Party page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          DatePeriodLoanPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Date Period Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          AmountOfTheLoanPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Amount Of The Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          AreRepaymentsInstalmentsPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Are Repayments Instalments page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          InterestOnLoanPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Are Interest On Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          SecurityGivenForLoanPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Are Security Given For Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          OutstandingArrearsOnLoanPage(_, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, refineMV(1), Check)
        )
        .withName("go from Outstanding Arrears On Loan page to check your answers page")
    )
  }
}
