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

import config.Refined.Max5000
import controllers.TestValues
import eu.timepit.refined.refineMV
import models.ConditionalYesNo._
import models.SponsoringOrConnectedParty.Sponsoring
import models.{ConditionalYesNo, IdentitySubject, IdentityType, Money, NormalMode, SchemeHoldLandProperty, TypeOfShares}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.common.{IdentityTypePage, IdentityTypes}
import pages.nonsipp.employercontributions.{EmployerContributionsPage, EmployerContributionsSectionStatus}
import pages.nonsipp.landorproperty._
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsPage, NoNINOPage}
import pages.nonsipp.moneyborrowed.{LenderNamePage, LenderNamePages, MoneyBorrowedPage, WhySchemeBorrowedMoneyPage}
import pages.nonsipp.otherassetsheld.OtherAssetsHeldPage
import pages.nonsipp.shares.{DidSchemeHoldAnySharesPage, SharesCompleted, SharesJourneyStatus, TypeOfSharesHeldPage}
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import pages.nonsipp.bonds.UnregulatedOrConnectedBondsHeldPage
import utils.UserAnswersUtils.UserAnswersOps
import viewmodels.models.{SectionCompleted, SectionStatus}
import viewmodels.models.TaskListStatus.{Completed, InProgress, NotStarted, UnableToStart}

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
    "should be Complete" - {
      "when only DidSchemeHoldAnyShares false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), false)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, hadSharesPageUrl)
      }
      "when DidSchemeHoldAnyShares is true and status is completed" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(SharesJourneyStatus(srn), SectionStatus.Completed)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, sharesListPageUrl)
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
      "when DidSchemeHoldAnyShares is true and status is InProgress - first incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(SharesJourneyStatus(srn), SectionStatus.InProgress)
          // first share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(1)), TypeOfShares.Unquoted)
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)
          .unsafeSet(SharesCompleted(srn, refineMV(2)), SectionCompleted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, typeOfSharesHeldPageOneUrl)
      }
      "when DidSchemeHoldAnyShares is true and status is InProgress - second incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(SharesJourneyStatus(srn), SectionStatus.InProgress)
          // first share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(1)), TypeOfShares.Unquoted)
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, typeOfSharesHeldPageTwoUrl)
      }
      "when DidSchemeHoldAnyShares is true and only second exist " in {
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
    val totalSharesPageUrl =
      controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesController
        .onPageLoad(srn)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getQuotedSharesTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, totalSharesPageUrl)
      }
    }
    "should be Complete" - {
      "when TotalValueQuotedSharesPage is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(TotalValueQuotedSharesPage(srn), money)
        val result = TaskListStatusUtils.getQuotedSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, totalSharesPageUrl)
      }
    }
  }
  "Bonds status" - {
    val hadBondsPageUrl =
      controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, hadBondsPageUrl)
      }
    }
    "should be Complete" - {
      "when only DidSchemeHoldAnySharesPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, hadBondsPageUrl)
      }
    }
  }

  "Other assets status" - {
    val hadAssetsPageUrl =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, hadAssetsPageUrl)
      }
    }
    "should be Complete" - {
      "when only OtherAssetsHeldPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), false)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Completed, hadAssetsPageUrl)
      }
    }
  }

  "Employer contributions" - {
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
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(
          defaultUserAnswers
            .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), false)
            .unsafeSet(NoNINOPage(srn, refineMV(1)), noninoReason),
          srn
        )
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

}
