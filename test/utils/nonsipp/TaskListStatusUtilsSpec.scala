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

package utils.nonsipp

import controllers.TestValues
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, IdentitySubject, IdentityType, Money, SchemeHoldLandProperty}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.common.{IdentityTypePage, IdentityTypes}
import pages.nonsipp.loansmadeoroutstanding.{
  IsIndividualRecipientConnectedPartyPage,
  LoansMadeOrOutstandingPage,
  OutstandingArrearsOnLoanPage,
  RecipientSponsoringEmployerConnectedPartyPage
}
import viewmodels.models.TaskListStatus.{Completed, InProgress, NotStarted}
import utils.UserAnswersUtils.UserAnswersOps
import models.ConditionalYesNo._
import models.SponsoringOrConnectedParty.Sponsoring
import pages.nonsipp.landorproperty.{
  IsLandPropertyLeasedPage,
  IsLesseeConnectedPartyPage,
  LandOrPropertyHeldPage,
  LandOrPropertyTotalIncomePage,
  LandOrPropertyTotalIncomePages,
  LandPropertyInUKPage,
  LandPropertyInUKPages,
  LandPropertyIndependentValuationPage,
  WhyDoesSchemeHoldLandPropertyPage
}

class TaskListStatusUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {
  "Loans status" - {
    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getLoansTaskListStatus(defaultUserAnswers, srn)
        result mustBe NotStarted
      }
    }
    "should be InProgress" - {
      "when only LoansMadeOrOutstandingPage true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
      "when LoansMadeOrOutstandingPage true and first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Individual))

        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
      "when LoansMadeOrOutstandingPage true and more first pages than last pages is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
          .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.Individual)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
      "when LoansMadeOrOutstandingPage true and there is a missing sponsoring employer page" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.Individual)
          .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(2)), ConditionalYesNo.yes[Unit, Money](money))

        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
      "when LoansMadeOrOutstandingPage true and there is a missing individual connected party page" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, refineMV(1)), Sponsoring)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.Individual)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(2)), ConditionalYesNo.yes[Unit, Money](money))

        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
    }
    "should be Complete" - {
      "when only LoansMadeOrOutstandingPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), false)
        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe Completed
      }
      "when LoansMadeOrOutstandingPage true and equal number of first pages and last pages and sponsoring/connected party pages is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, refineMV(1)), Sponsoring)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.Individual)
          .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(2)), ConditionalYesNo.yes[Unit, Money](money))

        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe Completed
      }

    }
  }

  "Land or property status" - {
    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(defaultUserAnswers, srn)
        result mustBe NotStarted
      }
    }
    "should be InProgress" - {
      "when only landOrPropertyHeldPage true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
      "when landOrPropertyHeldPage true and first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPages(srn), Map("0" -> true))

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
      "when andOrPropertyHeldPage true and more first pages than last pages is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }
      "when landOrPropertyHeldPage true and there is a missing lessee connected party page" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          //  IsLesseeConnectedPartyPage at index 1 is missing here
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }

      "when landOrPropertyHeldPage true and there is a missing independent valuation page" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1)), SchemeHoldLandProperty.Acquisition)
          // indep valuation data is missing
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(2)), SchemeHoldLandProperty.Acquisition)
          .unsafeSet(LandPropertyIndependentValuationPage(srn, refineMV(2)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }

      "when landOrPropertyHeldPage true and there is a missing independent valuation page" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1)), SchemeHoldLandProperty.Transfer)
          // indep valuation data is missing
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(2)), SchemeHoldLandProperty.Transfer)
          .unsafeSet(LandPropertyIndependentValuationPage(srn, refineMV(2)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe InProgress
      }

    }
    "should be Complete" - {
      "when only landOrPropertyHeldPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), false)
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe Completed
      }
      "when landOrPropertyHeldPage true and equal number of first pages and last pages and intermediate sub journey last pages are present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(2)), false)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe Completed
      }

      "when landOrPropertyHeldPage true and equal number of first pages and last pages and intermediate sub journey last + first pages are present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(IsLandPropertyLeasedPage(srn, refineMV(1)), false)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(2)), false)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatus(customUserAnswers, srn)
        result mustBe Completed
      }

    }
  }
}
