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

import utils.BaseSpec
import models.ConditionalYesNo._
import models.IdentityType.UKCompany
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import utils.IntUtils.given
import navigation.{Navigator, NavigatorBehaviours}
import models._
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class LoansMadeOrOutstandingNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index: Max5000 = 1
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
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName(
          "go from loan made or outstanding page to task list page when no selected"
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
          defaultUserAnswers
            .unsafeSet(ArrearsPrevYears(srn, 1), false)
            .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.no[Unit, Money](()))

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
            srn => LoansListPage(srn, addLoan = true),
            (srn, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, 2, NormalMode, IdentitySubject.LoanRecipient),
            srn =>
              defaultUserAnswers
                .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.Individual)
                .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
                .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)
          )
          .withName("go to who received the loan at index 2")
      )
    }

    "One record at index 2" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LoansListPage(srn, addLoan = true),
            (srn, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, 1, NormalMode, IdentitySubject.LoanRecipient),
            srn =>
              defaultUserAnswers
                .unsafeSet(IdentityTypePage(srn, 2, IdentitySubject.LoanRecipient), IdentityType.Individual)
                .unsafeSet(LoanCompleted(srn, 2), SectionCompleted)
                .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)
          )
          .withName("go to who received the loan at index 3")
      )
    }

    "No existing loan records" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LoansListPage(srn, addLoan = true),
            (srn, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, 1, NormalMode, IdentitySubject.LoanRecipient),
            _ => defaultUserAnswers
          )
          .withName("go to who received the loan at index 1")
      )
    }
  }

  "RemoveLoan" - {
    act.like(
      normalmode
        .navigateTo(
          srn => RemoveLoanPage(srn, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
              .onPageLoad(srn, NormalMode)
        )
        .withName("go from remove page to LoansMadeOrOutstanding page")
    )

    val completedLoanUserAnswers: Srn => UserAnswers =
      srn => defaultUserAnswers.unsafeSet(IdentityTypePage(srn, index, subject), UKCompany)

    act.like(
      normalmode
        .navigateTo(
          srn => RemoveLoanPage(srn, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
              .onPageLoad(srn, 1, NormalMode),
          completedLoanUserAnswers
        )
        .withName("go from remove page to LoansList page")
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

    act.like(
      checkmode
        .navigateTo(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          (srn, _) => controllers.nonsipp.loansmadeoroutstanding.routes.WhatYouWillNeedLoansController.onPageLoad(srn)
        )
        .withName("go from IdentityType page as None to WhatYouWillNeedLoans page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.UKCompany),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.CompanyRecipientNameController
              .onPageLoad(srn, 1, NormalMode),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.Other)
        )
        .withName("go from IdentityType page as UKCompany to CompanyRecipientName page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.Individual),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.IndividualRecipientNameController
              .onPageLoad(srn, 1, NormalMode),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.Other)
        )
        .withName("go from IdentityType page as Individual to IndividualRecipientName page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.UKPartnership),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.PartnershipRecipientNameController
              .onPageLoad(srn, 1, NormalMode),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.Other)
        )
        .withName("go from IdentityType page as Partnership to PartnershipRecipientName page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.Other),
          (srn, _) =>
            controllers.nonsipp.common.routes.OtherRecipientDetailsController
              .onPageLoad(srn, 1, NormalMode, IdentitySubject.LoanRecipient),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.Individual)
        )
        .withName("go from IdentityType page as Other to OtherRecipientDetails page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.UKCompany),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.UKCompany)
              .unsafeSet(CompanyRecipientNamePage(srn, 1), "companyRecipientName")
        )
        .withName("go from IdentityType page as UKCompany to check your answers page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.Individual),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.Individual)
              .unsafeSet(IndividualRecipientNamePage(srn, 1), "individualRecipientName")
        )
        .withName("go from IdentityType page as Individual to check your answers page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.UKPartnership),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.UKPartnership)
              .unsafeSet(PartnershipRecipientNamePage(srn, 1), "partnershipRecipientName")
        )
        .withName("go from IdentityType page as Partnership to check your answers page")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient),
          Gen.const(IdentityType.Other),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode),
          (srn: Srn) =>
            defaultUserAnswers
              .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient), IdentityType.Other)
              .unsafeSet(
                OtherRecipientDetailsPage(srn, 1, IdentitySubject.LoanRecipient),
                RecipientDetails("otherRecipientDetails", "otherRecipientDescription")
              )
        )
        .withName("go from IdentityType page as Other to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          IndividualRecipientNamePage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Individual Recipient Name page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          CompanyRecipientNamePage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Company Recipient Name page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          PartnershipRecipientNamePage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Partnership Recipient Name page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          OtherRecipientDetailsPage(_, 1, subject),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Other Recipient Details page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          IndividualRecipientNinoPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Individual Recipient Nino page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          CompanyRecipientCrnPage(_, 1, subject),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Company Recipient Crn page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          PartnershipRecipientUtrPage(_, 1, subject),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Partnership Recipient Utr page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          IsIndividualRecipientConnectedPartyPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Is Individual Recipient Connected Party page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          RecipientSponsoringEmployerConnectedPartyPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Recipient Sponsoring Employer Connected Party page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          DatePeriodLoanPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Date Period Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          AmountOfTheLoanPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Amount Of The Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          AreRepaymentsInstalmentsPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Are Repayments Instalments page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          InterestOnLoanPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Are Interest On Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          SecurityGivenForLoanPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Are Security Given For Loan page to check your answers page")
    )

    act.like(
      checkmode
        .navigateTo(
          OutstandingArrearsOnLoanPage(_, 1),
          (srn, _) =>
            controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController
              .onPageLoad(srn, 1, NormalMode)
        )
        .withName("go from Outstanding Arrears On Loan page to check your answers page")
    )
  }
}
