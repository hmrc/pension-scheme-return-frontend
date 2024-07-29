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

package utils.nonsipp

import pages.nonsipp.employercontributions.{EmployerContributionsPage, EmployerContributionsSectionStatus}
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld._
import config.Refined._
import controllers.TestValues
import pages.nonsipp.landorproperty._
import pages.nonsipp.receivetransfer.{DidSchemeReceiveTransferPage, TransfersInJourneyStatus}
import utils.UserAnswersUtils.UserAnswersOps
import org.scalatest.OptionValues
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsCompletedPage, SurrenderedBenefitsPage}
import models.{TypeOfShares, _}
import pages.nonsipp.loansmadeoroutstanding._
import models.SponsoringOrConnectedParty.Sponsoring
import pages.nonsipp.bonds._
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import pages.nonsipp.memberdetails._
import org.scalatest.freespec.AnyFreeSpec
import pages.nonsipp.membercontributions.{MemberContributionsListPage, MemberContributionsPage}
import pages.nonsipp.memberreceivedpcls.{PclsMemberListPage, PensionCommencementLumpSumPage}
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import pages.nonsipp.memberpensionpayments.{MemberPensionPaymentsListPage, PensionPaymentsReceivedPage}
import eu.timepit.refined.refineMV
import viewmodels.models.TaskListStatus._
import pages.nonsipp.common.{IdentityTypePage, IdentityTypes}
import pages.nonsipp.membertransferout.{SchemeTransferOutPage, TransfersOutSectionCompleted}
import pages.nonsipp.moneyborrowed._
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import viewmodels.models.{SectionCompleted, SectionStatus}

class TaskListStatusUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  private val index1of5: Max5 = refineMV(1)
  private val index1of300: Max300 = refineMV(1)
  private val index1of5000: Max5000 = refineMV(1)

  private val defaultUserAnswersWithMember: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), false)
    .unsafeSet(NoNINOPage(srn, refineMV(1)), noninoReason)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

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
    "should be Reported" - {
      "when only LoansMadeOrOutstandingPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), false)
        val result = TaskListStatusUtils.getLoansTaskListStatus(customUserAnswers, srn)
        result mustBe Reported(0)
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
        result mustBe Reported(2)
      }
    }
  }

  "Land or property status" - {
    val heldPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode).url
    val listPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, 1, NormalMode).url

    def inUkPageUrl(index: Max5000) =
      controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad(srn, index, NormalMode).url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, heldPageUrl)
      }
    }
    "should be InProgress" - {
      "when only landOrPropertyHeldPage true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(1)))
      }
      "when landOrPropertyHeldPage true and first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPages(srn), Map("0" -> true))

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(1)))
      }
      "when andOrPropertyHeldPage true and more first pages than last pages is present - index 2 is missing" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(2)))
      }
      "when andOrPropertyHeldPage true and more first pages than last pages is present - index 1 is missing" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          // missing here
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(1)))
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

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(1)))
      }

      "when landOrPropertyHeldPage true and there is a missing lessee connected party page at index 2" in {
        val customUserAnswers = defaultUserAnswers
        // index 1
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)

          // index 2
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          //  IsLesseeConnectedPartyPage at index 1 is missing here
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(2)))
      }

      "when landOrPropertyHeldPage true and there is a missing independent valuation page" in {
        val customUserAnswers = defaultUserAnswers
        // index 1
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1)), SchemeHoldLandProperty.Acquisition)
          // indep valuation data is missing
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)

          // index 2
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(2)), SchemeHoldLandProperty.Acquisition)
          .unsafeSet(LandPropertyIndependentValuationPage(srn, refineMV(2)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(1)))
      }
      "when landOrPropertyHeldPage true and there is a missing independent valuation page at index 2" in {
        val customUserAnswers = defaultUserAnswers
        // index 1
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1)), SchemeHoldLandProperty.Acquisition)
          .unsafeSet(LandPropertyIndependentValuationPage(srn, refineMV(1)), true)
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(1)), money)

          // index 2
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(2)), SchemeHoldLandProperty.Acquisition)
          // indep valuation data is missing
          .unsafeSet(IsLesseeConnectedPartyPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyTotalIncomePage(srn, refineMV(2)), money)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, inUkPageUrl(refineMV(2)))
      }
    }
    "should be Complete" - {
      "when only landOrPropertyHeldPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), false)
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, heldPageUrl)
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

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, listPageUrl)
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

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, listPageUrl)
      }

    }
  }

  "Borrowing status" - {
    val moneyBorrowedPageUrl =
      controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, NormalMode).url
    val listPageUrl =
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, 1, NormalMode).url

    def lenderNamePageUrl(index: Max5000) =
      controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, index, NormalMode).url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, moneyBorrowedPageUrl)
      }
    }
    "should be InProgress" - {
      "when only moneyBorrowedPage true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, lenderNamePageUrl(refineMV(1)))
      }
      "when moneyBorrowedPage true and first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(LenderNamePages(srn), Map("0" -> lenderName))

        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, lenderNamePageUrl(refineMV(1)))
      }
      "when moneyBorrowedPage true and more first pages than last pages is present - index 2 is missing" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(LenderNamePage(srn, refineMV(1)), lenderName)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(1)), reasonBorrowed)
          .unsafeSet(LenderNamePage(srn, refineMV(2)), lenderName)

        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, lenderNamePageUrl(refineMV(2)))
      }
      "when moneyBorrowedPage true and more first pages than last pages is present - index 1 is missing" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(LenderNamePage(srn, refineMV(1)), lenderName)
          // missing here
          .unsafeSet(LenderNamePage(srn, refineMV(2)), lenderName)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(2)), reasonBorrowed)

        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, lenderNamePageUrl(refineMV(1)))
      }
    }
    "should be Complete" - {
      "when only moneyBorrowedPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), false)
        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, listPageUrl)
      }
      "when moneyBorrowedPage true and equal number of first pages and last pages are present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(LenderNamePage(srn, refineMV(1)), lenderName)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(1)), reasonBorrowed)
          .unsafeSet(LenderNamePage(srn, refineMV(2)), lenderName)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(2)), reasonBorrowed)

        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, listPageUrl)
      }
    }
  }

  "Shares status" - {
    val hadSharesPageUrl =
      controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController
        .onPageLoad(srn, NormalMode)
        .url
    val sharesListPageUrl =
      controllers.nonsipp.shares.routes.SharesListController
        .onPageLoad(srn, 1, NormalMode)
        .url
    val typeOfSharesHeldPageOneUrl =
      controllers.nonsipp.shares.routes.TypeOfSharesHeldController
        .onPageLoad(srn, refineMV(1), NormalMode)
        .url
    val typeOfSharesHeldPageTwoUrl =
      controllers.nonsipp.shares.routes.TypeOfSharesHeldController
        .onPageLoad(srn, refineMV(2), NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, hadSharesPageUrl)
      }
    }
    "should be Reported" - {
      "when only DidSchemeHoldAnyShares false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), false)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(0), hadSharesPageUrl)
      }
      "when DidSchemeHoldAnyShares is true and with 1 reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(TypeOfSharesHeldPage(srn, index1of5000), TypeOfShares.ConnectedParty)
          .unsafeSet(SharesCompleted(srn, index1of5000), SectionCompleted)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(1), sharesListPageUrl)
      }
      "when DidSchemeHoldAnyShares is true and status is Reported - first incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(SharesJourneyStatus(srn), SectionStatus.InProgress)
          // first share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(1)), TypeOfShares.Unquoted)
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)
          .unsafeSet(SharesCompleted(srn, refineMV(2)), SectionCompleted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(1), typeOfSharesHeldPageOneUrl)
      }
      "when DidSchemeHoldAnyShares is true and status is Reported - second incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(SharesJourneyStatus(srn), SectionStatus.InProgress)
          // first share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(1)), TypeOfShares.Unquoted)
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(1), typeOfSharesHeldPageTwoUrl)
      }
    }
    "should be InProgress" - {
      "when only DidSchemeHoldAnyShares true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, typeOfSharesHeldPageOneUrl)
      }
      "when DidSchemeHoldAnyShares is true and status is InProgress" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(SharesJourneyStatus(srn), SectionStatus.InProgress)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, typeOfSharesHeldPageOneUrl)
      }
      "when DidSchemeHoldAnyShares is true and only second exist" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          // nothing for first share
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, typeOfSharesHeldPageTwoUrl)
      }
    }
  }

  "Quoted shares status" - {
    val quotedSharesManagedFundsUrl =
      controllers.nonsipp.totalvaluequotedshares.routes.QuotedSharesManagedFundsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val totalValueQuotedSharesCyaUrl =
      controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
        .onPageLoad(srn)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getQuotedSharesTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, quotedSharesManagedFundsUrl)
      }
    }
    "should be Reported" - {
      "when TotalValueQuotedSharesPage is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(TotalValueQuotedSharesPage(srn), money)
        val result = TaskListStatusUtils.getQuotedSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported, totalValueQuotedSharesCyaUrl)
      }
    }
  }

  "Bonds status" - {
    val hadBondsPageUrl =
      controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val bondsListPageUrl =
      controllers.nonsipp.bonds.routes.BondsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val nameOfBondsUrlIndexOne =
      controllers.nonsipp.bonds.routes.NameOfBondsController
        .onPageLoad(srn, refineMV(1), NormalMode)
        .url

    val nameOfBondsUrlIndexTwo =
      controllers.nonsipp.bonds.routes.NameOfBondsController
        .onPageLoad(srn, refineMV(2), NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, hadBondsPageUrl)
      }
    }

    "should be Complete" - {
      "when only UnregulatedOrConnectedBondsHeldPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, hadBondsPageUrl)
      }
      "when only UnregulatedOrConnectedBondsHeldPage true and status is completed" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(BondsJourneyStatus(srn), SectionStatus.Completed)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, bondsListPageUrl)
      }
    }

    "should be InProgress" - {
      "when only UnregulatedOrConnectedBondsHeldPage true" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, nameOfBondsUrlIndexOne)
      }

      "when only UnregulatedOrConnectedBondsHeldPage true and status is InProgress" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(BondsJourneyStatus(srn), SectionStatus.InProgress)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, nameOfBondsUrlIndexOne)
      }

      "when only UnregulatedOrConnectedBondsHeldPage true and status is InProgress - first incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(BondsJourneyStatus(srn), SectionStatus.InProgress)
          // first bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(1)), "NameOfFirstBond")
          // second bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(2)), "NameOfSecondBond")
          .unsafeSet(BondsCompleted(srn, refineMV(2)), SectionCompleted)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, nameOfBondsUrlIndexOne)
      }

      "when only UnregulatedOrConnectedBondsHeldPage true and status is InProgress - second incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(BondsJourneyStatus(srn), SectionStatus.InProgress)
          // first bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(1)), "NameOfFirstBond")
          .unsafeSet(BondsCompleted(srn, refineMV(1)), SectionCompleted)
          // second bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(2)), "NameOfSecondBond")
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, nameOfBondsUrlIndexTwo)
      }

      "when only UnregulatedOrConnectedBondsHeldPage true and only second exist" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(BondsJourneyStatus(srn), SectionStatus.InProgress)
          // nothing for the first bond:
          // second bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(2)), "NameOfSecondBond")
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, nameOfBondsUrlIndexTwo)
      }
    }
  }

  "Other assets status" - {
    val hadAssetsPageUrl =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val assetsListPageUrl =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val whatIsAssetUrlIndexOne =
      controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
        .onPageLoad(srn, refineMV(1), NormalMode)
        .url

    val whatIsAssetUrlIndexTwo =
      controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
        .onPageLoad(srn, refineMV(2), NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, hadAssetsPageUrl)
      }
    }

    "should be Complete" - {
      "when OtherAssetsHeldPage is false" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), false)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, hadAssetsPageUrl)
      }

      "when OtherAssetsHeldPage is true and JourneyStatus is Completed" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(OtherAssetsJourneyStatus(srn), SectionStatus.Completed)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, assetsListPageUrl)
      }
    }

    "should be InProgress" - {
      "when only OtherAssetsHeldPage is true" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, whatIsAssetUrlIndexOne)
      }

      "when OtherAssetsHeldPage is true and JourneyStatus is InProgress" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(OtherAssetsJourneyStatus(srn), SectionStatus.InProgress)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, whatIsAssetUrlIndexOne)
      }

      "when OtherAssetsHeldPage is true and JourneyStatus is InProgress - first asset incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(OtherAssetsJourneyStatus(srn), SectionStatus.InProgress)
          // first asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(1)), "asset one")
          // second asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "asset two")
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(2)), SectionCompleted)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, whatIsAssetUrlIndexOne)
      }

      "when OtherAssetsHeldPage is true and JourneyStatus is InProgress - second asset incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(OtherAssetsJourneyStatus(srn), SectionStatus.InProgress)
          // first asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(1)), "asset one")
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
          // second asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "asset two")
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, whatIsAssetUrlIndexTwo)
      }

      "when OtherAssetsHeldPage is true and JourneyStatus is InProgress - first asset removed" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(OtherAssetsJourneyStatus(srn), SectionStatus.InProgress)
          // nothing for the first asset:
          // second asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "asset two")
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, whatIsAssetUrlIndexTwo)
      }
    }
  }

  "Employer contributions status" - {
    val wereContributions =
      controllers.nonsipp.employercontributions.routes.EmployerContributionsController
        .onPageLoad(srn, NormalMode)
        .url
    val selectMember =
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, wereContributions)
      }
    }
    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, wereContributions)
      }
    }

    "should be In Progress" - {
      "when only were section status is progress and employer contributions true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.InProgress)
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }
    }
    "should be Complete" - {
      "when section status is complete and employer contributions false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), false)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, wereContributions)
      }
      "when section status is complete and employer contributions true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, selectMember)
      }
    }
  }

  "Unallocated Employer Contributions status" - {

    val firstQuestionPageUrl =
      controllers.nonsipp.memberpayments.routes.UnallocatedEmployerContributionsController
        .onPageLoad(srn, NormalMode)
        .url

    val cyaPageUrl =
      controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
        .onPageLoad(srn, NormalMode)
        .url

    "should be Unable to start" - {
      "when no Members are reported" in {
        val result = TaskListStatusUtils.getUnallocatedContributionsStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, firstQuestionPageUrl)
      }
    }

    "should be Not started" - {
      "when Member is reported and UnallocatedEmployerContributionsPage is None" in {
        val result = TaskListStatusUtils.getUnallocatedContributionsStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be None reported" - {
      "when UnallocatedEmployerContributionsPage is false" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(UnallocatedEmployerContributionsPage(srn), false)

        val result = TaskListStatusUtils.getUnallocatedContributionsStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(0), firstQuestionPageUrl)
      }
    }

    "should be In progress" - {
      "when UnallocatedEmployerContributionsPage is true and UnallocatedEmployerAmountPage is None" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)

        val result = TaskListStatusUtils.getUnallocatedContributionsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }
    }

    "should be Reported" - {
      "when UnallocatedEmployerContributionsPage is true and UnallocatedEmployerAmountPage is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
          .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

        val result = TaskListStatusUtils.getUnallocatedContributionsStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported, cyaPageUrl)
      }
    }
  }

  "Transfer in status" - {
    val wereTransfersIn =
      controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController
        .onPageLoad(srn, NormalMode)
        .url
    val selectMember =
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getTransferInStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, wereTransfersIn)
      }
    }
    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getTransferInStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, wereTransfersIn)
      }
    }

    "should be In Progress" - {
      "when only were section status is progress and did scheme received transfer true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
          .unsafeSet(TransfersInJourneyStatus(srn), SectionStatus.InProgress)
        val result = TaskListStatusUtils.getTransferInStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }
    }
    "should be Complete" - {
      "when section status is complete and  did scheme received transfer false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeReceiveTransferPage(srn), false)
          .unsafeSet(TransfersInJourneyStatus(srn), SectionStatus.Completed)
        val result = TaskListStatusUtils.getTransferInStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, wereTransfersIn)
      }
      "when section status is complete and did scheme received transfer true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
          .unsafeSet(TransfersInJourneyStatus(srn), SectionStatus.Completed)
        val result = TaskListStatusUtils.getTransferInStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, selectMember)
      }
    }
  }

  "Transfer out status" - {

    val wereTransfersOut =
      controllers.nonsipp.membertransferout.routes.SchemeTransferOutController
        .onPageLoad(srn, NormalMode)
        .url
    val selectMember =
      controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getTransferOutStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, wereTransfersOut)
      }
    }

    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getTransferOutStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, wereTransfersOut)
      }
    }

    "should be In Progress" - {
      "when SchemeTransferOutPage is true and no Transfer Out has been reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SchemeTransferOutPage(srn), true)
        val result = TaskListStatusUtils.getTransferOutStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, wereTransfersOut)
      }
    }

    "should be Reported" - {
      "when SchemeTransferOutPage is false" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SchemeTransferOutPage(srn), false)
        val result = TaskListStatusUtils.getTransferOutStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(0), wereTransfersOut)
      }

      "when SchemeTransferOutPage is true and Transfer Out has been reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SchemeTransferOutPage(srn), true)
          .unsafeSet(TransfersOutSectionCompleted(srn, index1of300, index1of5), SectionCompleted)
        val result = TaskListStatusUtils.getTransferOutStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(1), selectMember)
      }
    }
  }

  "Surrendered benefits status" - {

    val wereSurrenderedBenefits =
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsController
        .onPageLoad(srn, NormalMode)
        .url

    val selectMember =
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getSurrenderedBenefitsStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, wereSurrenderedBenefits)
      }
    }

    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getSurrenderedBenefitsStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, wereSurrenderedBenefits)
      }
    }

    "should be In Progress" - {
      "when SurrenderedBenefitsPage is true and no Surrendered Benefit has been reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SurrenderedBenefitsPage(srn), true)
        val result = TaskListStatusUtils.getSurrenderedBenefitsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, wereSurrenderedBenefits)
      }
    }

    "should be Reported" - {
      "when SurrenderedBenefitsPage is false" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SurrenderedBenefitsPage(srn), false)
        val result = TaskListStatusUtils.getSurrenderedBenefitsStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(0), wereSurrenderedBenefits)
      }

      "when SurrenderedBenefitsPage is true and Surrendered Benefit has been reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SurrenderedBenefitsPage(srn), true)
          .unsafeSet(SurrenderedBenefitsCompletedPage(srn, index1of300), SectionCompleted)
        val result = TaskListStatusUtils.getSurrenderedBenefitsStatusAndLink(customUserAnswers, srn)
        result mustBe (Reported(1), selectMember)
      }
    }
  }

  "Member contributions status" - {
    val wereContributions =
      controllers.nonsipp.membercontributions.routes.MemberContributionsController
        .onPageLoad(srn, NormalMode)
        .url
    val selectMember =
      controllers.nonsipp.membercontributions.routes.MemberContributionListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, wereContributions)
      }
    }
    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, wereContributions)
      }
    }

    "should be In Progress" - {
      "when only member contributions true is present" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(MemberContributionsPage(srn), true)
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }

      "when member contributions is true and member contributions list page is false" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(MemberContributionsPage(srn), true)
          .unsafeSet(MemberContributionsListPage(srn), false)
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }
    }
    "should be Complete" - {
      "when member contributions is true and member contributions list page is true" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(MemberContributionsPage(srn), true)
          .unsafeSet(MemberContributionsListPage(srn), true)
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, selectMember)
      }
    }
  }

  "Pcls status" - {
    val werePcls =
      controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumController
        .onPageLoad(srn, NormalMode)
        .url
    val selectMember =
      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getPclsStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, werePcls)
      }
    }
    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getPclsStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, werePcls)
      }
    }

    "should be In Progress" - {
      "when only PCLS received true is present" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionCommencementLumpSumPage(srn), true)
        val result = TaskListStatusUtils.getPclsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }

      "when PCLS received is true and member list page is false" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionCommencementLumpSumPage(srn), true)
          .unsafeSet(PclsMemberListPage(srn), false)
        val result = TaskListStatusUtils.getPclsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }
    }
    "should be Complete" - {
      "when PCLS received is true and member list page is true" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionCommencementLumpSumPage(srn), true)
          .unsafeSet(PclsMemberListPage(srn), true)
        val result = TaskListStatusUtils.getPclsStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, selectMember)
      }
    }
  }

  "Pension payments status" - {
    val werePensionPayments =
      controllers.nonsipp.memberpensionpayments.routes.PensionPaymentsReceivedController
        .onPageLoad(srn, NormalMode)
        .url
    val selectMember =
      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, werePensionPayments)
      }
    }
    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, werePensionPayments)
      }
    }

    "should be In Progress" - {
      "when only pension payments received true is present" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionPaymentsReceivedPage(srn), true)
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }

      "when were pension payments received  is true and member payments list page is false" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionPaymentsReceivedPage(srn), true)
          .unsafeSet(MemberPensionPaymentsListPage(srn), false)
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, selectMember)
      }
    }
    "should be Complete" - {
      "when pension payments received  is true and member payments list page is true" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionPaymentsReceivedPage(srn), true)
          .unsafeSet(MemberPensionPaymentsListPage(srn), true)
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, selectMember)
      }
    }
  }
}
