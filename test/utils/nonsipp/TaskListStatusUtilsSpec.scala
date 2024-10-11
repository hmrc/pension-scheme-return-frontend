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

import pages.nonsipp.employercontributions._
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld._
import config.Refined._
import controllers.TestValues
import pages.nonsipp.landorproperty._
import pages.nonsipp.receivetransfer._
import pages.nonsipp.landorpropertydisposal.{
  HowWasPropertyDisposedOfPage,
  LandOrPropertyDisposalPage,
  LandOrPropertyStillHeldPage
}
import pages.nonsipp.sharesdisposal._
import utils.UserAnswersUtils.UserAnswersOps
import pages.nonsipp.membersurrenderedbenefits._
import models._
import models.SponsoringOrConnectedParty.Sponsoring
import pages.nonsipp.otherassetsdisposal.{AnyPartAssetStillHeldPage, HowWasAssetDisposedOfPage, OtherAssetsDisposalPage}
import pages.nonsipp.schemedesignatory._
import pages.nonsipp.bonds._
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import pages.nonsipp.memberdetails._
import org.scalatest.freespec.AnyFreeSpec
import pages.nonsipp.membercontributions.{MemberContributionsPage, TotalMemberContributionPage}
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import models.ManualOrUpload.Manual
import models.PensionSchemeType.RegisteredPS
import models.IdentityType.{Individual, UKCompany, UKPartnership}
import utils.nonsipp.TaskListStatusUtils.userAnswersUnchangedAllSections
import models.SchemeHoldShare.Acquisition
import pages.nonsipp.memberpensionpayments.{PensionPaymentsReceivedPage, TotalAmountPensionPaymentsPage}
import eu.timepit.refined.refineMV
import pages.nonsipp.{CheckReturnDatesPage, WhichTaxYearPage}
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino
import models.HowSharesDisposed.Sold
import viewmodels.models.TaskListStatus._
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject._
import pages.nonsipp.membertransferout._
import pages.nonsipp.moneyborrowed._
import pages.nonsipp.bondsdisposal.{BondsDisposalPage, BondsStillHeldPage, HowWereBondsDisposedOfPage}
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import viewmodels.models.{MemberState, SectionCompleted}

class TaskListStatusUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  private val index1of300: Max300 = refineMV(1)
  private val index1of50: Max50 = refineMV(1)
  private val index1of5: Max5 = refineMV(1)
  private val index1of5000: Max5000 = refineMV(1)
  private val index2of5000: Max5000 = refineMV(2)
  private val name: String = "name"
  private val reason: String = "reason"

  private val currentUA: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), dateRange) // automatically set
    // Section 1 - Scheme Details
    // (S1) Basic Details
    .unsafeSet(CheckReturnDatesPage(srn), true)
    .unsafeSet(ActiveBankAccountPage(srn), true)
    .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
    // (S1) Financial Details
    .unsafeSet(HowMuchCashPage(srn, NormalMode), moneyInPeriod)
    .unsafeSet(ValueOfAssetsPage(srn, NormalMode), moneyInPeriod)
    .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), money)
    // Section 2 - Member Details
    .unsafeSet(PensionSchemeMembersPage(srn), Manual)
    .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), true)
    .unsafeSet(MemberDetailsNinoPage(srn, index1of300), nino)
    .unsafeSet(MemberStatus(srn, index1of300), MemberState.New)
    // Section 3 - Member Payments
    // (S3) Employer Contributions
    .unsafeSet(EmployerContributionsPage(srn), true)
    .unsafeSet(EmployerNamePage(srn, index1of300, index1of50), name)
    .unsafeSet(EmployerTypeOfBusinessPage(srn, index1of300, index1of50), IdentityType.Other)
    .unsafeSet(OtherEmployeeDescriptionPage(srn, index1of300, index1of50), otherDetails)
    .unsafeSet(TotalEmployerContributionPage(srn, index1of300, index1of50), money)
    .unsafeSet(ContributionsFromAnotherEmployerPage(srn, index1of300, index1of50), false)
    // (S3) Unallocated Employer Contributions
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
    // (S3) Member Contributions
    .unsafeSet(MemberContributionsPage(srn), false)
    // (S3) Transfers In
    .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
    .unsafeSet(TransferringSchemeNamePage(srn, index1of300, index1of5), name)
    .unsafeSet(TransferringSchemeTypePage(srn, index1of300, index1of5), RegisteredPS(pstr))
    .unsafeSet(TotalValueTransferPage(srn, index1of300, index1of5), money)
    .unsafeSet(WhenWasTransferReceivedPage(srn, index1of300, index1of5), localDate)
    .unsafeSet(DidTransferIncludeAssetPage(srn, index1of300, index1of5), true)
    // (S3) Transfers Out
    .unsafeSet(SchemeTransferOutPage(srn), true)
    .unsafeSet(ReceivingSchemeNamePage(srn, index1of300, index1of5), name)
    .unsafeSet(ReceivingSchemeTypePage(srn, index1of300, index1of5), PensionSchemeType.Other(otherDetails))
    .unsafeSet(WhenWasTransferMadePage(srn, index1of300, index1of5), localDate)
    // (S3) PCLS
    .unsafeSet(PensionCommencementLumpSumPage(srn), true)
    .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index1of300), pcls)
    // (S3) Pension Payments
    .unsafeSet(PensionPaymentsReceivedPage(srn), false)
    // (S3) Surrendered Benefits
    .unsafeSet(SurrenderedBenefitsPage(srn), true)
    .unsafeSet(SurrenderedBenefitsAmountPage(srn, index1of300), money)
    .unsafeSet(WhenDidMemberSurrenderBenefitsPage(srn, index1of300), localDate)
    .unsafeSet(WhyDidMemberSurrenderBenefitsPage(srn, index1of300), reason)
    // Section 4 - Loans Made & Money Borrowed
    // (S4) Loans Made
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, index1of5000), name)
    .unsafeSet(IndividualRecipientNinoPage(srn, index1of5000), ConditionalYesNo.yes[String, Nino](nino))
    .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(DatePeriodLoanPage(srn, index1of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index1of5000), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index1of5000), true)
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Money](money))
    // (S4) Money Borrowed
    .unsafeSet(MoneyBorrowedPage(srn), true)
    .unsafeSet(LenderNamePage(srn, index1of5000), lenderName)
    .unsafeSet(IsLenderConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index1of5000), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, index1of5000), localDate)
    .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index1of5000), money)
    .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index1of5000), reason)
    // Section 5 - Shares
    // (S5) Shares
    .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
    .unsafeSet(TypeOfSharesHeldPage(srn, index1of5000), TypeOfShares.SponsoringEmployer)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index1of5000), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index1of5000), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index1of5000), name)
    .unsafeSet(SharesCompanyCrnPage(srn, index1of5000), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(ClassOfSharesPage(srn, index1of5000), classOfShares)
    .unsafeSet(HowManySharesPage(srn, index1of5000), totalShares)
    .unsafeSet(IdentityTypePage(srn, index1of5000, SharesSeller), UKPartnership)
    .unsafeSet(PartnershipShareSellerNamePage(srn, index1of5000), companyName)
    .unsafeSet(PartnershipRecipientUtrPage(srn, index1of5000, SharesSeller), ConditionalYesNo.yes[String, Utr](utr))
    .unsafeSet(CostOfSharesPage(srn, index1of5000), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(TotalAssetValuePage(srn, index1of5000), money)
    .unsafeSet(SharesTotalIncomePage(srn, index1of5000), money)
    .unsafeSet(SharesListPage(srn), false)
    .unsafeSet(SharesCompleted(srn, index1of5000), SectionCompleted)
    // (S5) Shares Disposals
    .unsafeSet(SharesDisposalPage(srn), true)
    .unsafeSet(HowWereSharesDisposedPage(srn, index1of5000, index1of50), Sold)
    .unsafeSet(WhenWereSharesSoldPage(srn, index1of5000, index1of50), localDate)
    .unsafeSet(HowManySharesSoldPage(srn, index1of5000, index1of50), totalShares)
    .unsafeSet(TotalConsiderationSharesSoldPage(srn, index1of5000, index1of50), money)
    .unsafeSet(WhoWereTheSharesSoldToPage(srn, index1of5000, index1of50), IdentityType.Other)
    .unsafeSet(pages.nonsipp.sharesdisposal.OtherBuyerDetailsPage(srn, index1of5000, index1of50), otherRecipientDetails)
    .unsafeSet(pages.nonsipp.sharesdisposal.IsBuyerConnectedPartyPage(srn, index1of5000, index1of50), true)
    .unsafeSet(pages.nonsipp.sharesdisposal.IndependentValuationPage(srn, index1of5000, index1of50), true)
    .unsafeSet(HowManyDisposalSharesPage(srn, index1of5000, index1of50), totalShares)
    // (S5) Quoted Shares
    .unsafeSet(TotalValueQuotedSharesPage(srn), money)
    // Section 6 - Land or Property
    // (S6) Land or Property
    .unsafeSet(LandOrPropertyHeldPage(srn), true)
    .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
    .unsafeSet(LandOrPropertyPostcodeLookupPage(srn, index1of5000), postcodeLookup)
    .unsafeSet(AddressLookupResultsPage(srn, index1of5000), List(address, address, address))
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.no[String, String](reason))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Acquisition)
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LandOrPropertySeller), UKCompany)
    .unsafeSet(CompanySellerNamePage(srn, index1of5000), name)
    .unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, LandOrPropertySeller), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), true)
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index1of5000), (leaseName, money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index1of5000), money)
    .unsafeSet(LandOrPropertyCompleted(srn, index1of5000), SectionCompleted)
    // (S6) Land or Property Disposals
    .unsafeSet(LandOrPropertyDisposalPage(srn), true)
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Transferred)
    .unsafeSet(LandOrPropertyStillHeldPage(srn, index1of5000, index1of50), false)
    // Section 7 - Bonds
    // (S7) Bonds
    .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
    .unsafeSet(NameOfBondsPage(srn, index1of5000), name)
    .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index1of5000), SchemeHoldBond.Acquisition)
    .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index1of5000), localDate)
    .unsafeSet(CostOfBondsPage(srn, index1of5000), money)
    .unsafeSet(BondsFromConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(AreBondsUnregulatedPage(srn, index1of5000), true)
    .unsafeSet(IncomeFromBondsPage(srn, index1of5000), money)
    .unsafeSet(BondsCompleted(srn, index1of5000), SectionCompleted)
    // (S7) Bonds Disposals
    .unsafeSet(BondsDisposalPage(srn), true)
    .unsafeSet(HowWereBondsDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Other(otherDetails))
    .unsafeSet(BondsStillHeldPage(srn, index1of5000, index1of50), bondsStillHeld)
    // Section 8 - Other Assets
    // (S8) Other Assets
    .unsafeSet(OtherAssetsHeldPage(srn), true)
    .unsafeSet(WhatIsOtherAssetPage(srn, index1of5000), otherAssetDescription)
    .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index1of5000), true)
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index1of5000), SchemeHoldAsset.Acquisition)
    .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index1of5000), localDate)
    .unsafeSet(IdentityTypePage(srn, index1of5000, OtherAssetSeller), Individual)
    .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index1of5000), name)
    .unsafeSet(OtherAssetIndividualSellerNINumberPage(srn, index1of5000), ConditionalYesNo.no[String, Nino](reason))
    .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(CostOfOtherAssetPage(srn, index1of5000), money)
    .unsafeSet(pages.nonsipp.otherassetsheld.IndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(IncomeFromAssetPage(srn, index1of5000), money)
    .unsafeSet(OtherAssetsCompleted(srn, index1of5000), SectionCompleted)
    // (S8) Other Assets Disposals
    .unsafeSet(OtherAssetsDisposalPage(srn), true)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Transferred)
    .unsafeSet(AnyPartAssetStillHeldPage(srn, index1of5000, index1of50), true)

  private val previousUA: UserAnswers = currentUA
    .unsafeSet(TotalValueQuotedSharesPage(srn), Money(1))

  private val defaultUserAnswersWithMember: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), false)
    .unsafeSet(NoNINOPage(srn, refineMV(1)), noninoReason)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

  "Loans status" - {
    val firstQuestionPageUrl =
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    def secondQuestionPageUrl(index: Max5000): String =
      controllers.nonsipp.common.routes.IdentityTypeController
        .onPageLoad(srn, index, NormalMode, IdentitySubject.LoanRecipient)
        .url

    "should be Not Started" - {
      "when default data" in {
        val (status, link) = TaskListStatusUtils.getLoansTaskListStatusAndLink(defaultUserAnswers, srn)
        status mustBe NotStarted
        link mustBe firstQuestionPageUrl
      }
    }

    "should be InProgress" - {
      "when only LoansMadeOrOutstandingPage true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
        val (status, link) = TaskListStatusUtils.getLoansTaskListStatusAndLink(customUserAnswers, srn)
        status mustBe InProgress
        link mustBe firstQuestionPageUrl
      }

      "when LoansMadeOrOutstandingPage true and only first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Individual))

        val (status, link) = TaskListStatusUtils.getLoansTaskListStatusAndLink(customUserAnswers, srn)
        status mustBe InProgress
        link mustBe secondQuestionPageUrl(index1of5000)
      }
    }

    "should be Recorded" - {
      "when only LoansMadeOrOutstandingPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), false)
        val (status, link) = TaskListStatusUtils.getLoansTaskListStatusAndLink(customUserAnswers, srn)
        status mustBe Recorded(0, "")
        link mustBe firstQuestionPageUrl
      }

      "when LoansMadeOrOutstandingPage true and equal number of first pages and last pages" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, refineMV(1)), Sponsoring)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.Individual)
          .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, refineMV(1)), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(2)), ConditionalYesNo.yes[Unit, Money](money))
        val (status, link) = TaskListStatusUtils.getLoansTaskListStatusAndLink(customUserAnswers, srn)
        status mustBe Recorded(2, "loans")
        link mustBe listPageUrl
      }

      "when LoansMadeOrOutstandingPage true and more first pages than last pages is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
          .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.Individual)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

        val (status, link) = TaskListStatusUtils.getLoansTaskListStatusAndLink(customUserAnswers, srn)
        status mustBe Recorded(1, "loans")
        link mustBe listPageUrl
      }
    }
  }

  "Land or property status" - {
    val firstQuestionPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    def secondQuestionPageUrl(index: Max5000): String =
      controllers.nonsipp.landorproperty.routes.LandPropertyInUKController
        .onPageLoad(srn, index, NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be InProgress" - {
      "when only landOrPropertyHeldPage true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }

      "when landOrPropertyHeldPage true and only first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPages(srn), Map("0" -> true))

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index1of5000))
      }
    }

    "should be Recorded" - {
      "when only landOrPropertyHeldPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), false)
        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when landOrPropertyHeldPage true and equal number of first pages and last pages are present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyCompleted(srn, index1of5000), SectionCompleted)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyCompleted(srn, index2of5000), SectionCompleted)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(2, "landOrProperties"), listPageUrl)
      }

      "when landOrPropertyHeldPage true and more first pages than last pages is present - index 2 is missing" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          .unsafeSet(LandOrPropertyCompleted(srn, index1of5000), SectionCompleted)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "landOrProperties"), listPageUrl)
      }

      "when landOrPropertyHeldPage true and more first pages than last pages is present - index 1 is missing" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(LandOrPropertyHeldPage(srn), true)
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(1)), true)
          // missing here
          .unsafeSet(LandPropertyInUKPage(srn, refineMV(2)), true)
          .unsafeSet(LandOrPropertyCompleted(srn, index2of5000), SectionCompleted)

        val result = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "landOrProperties"), listPageUrl)
      }
    }
  }

  "Borrowing status" - {
    val firstQuestionPageUrl =
      controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    def secondQuestionPageUrl(index: Max5000): String =
      controllers.nonsipp.moneyborrowed.routes.LenderNameController
        .onPageLoad(srn, index, NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be InProgress" - {
      "when only moneyBorrowedPage true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }

      "when moneyBorrowedPage true and only first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(LenderNamePages(srn), Map("0" -> lenderName))

        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index1of5000))
      }
    }

    "should be Recorded" - {
      "when only moneyBorrowedPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), false)
        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when moneyBorrowedPage true and equal number of first pages and last pages are present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(MoneyBorrowedPage(srn), true)
          .unsafeSet(LenderNamePage(srn, refineMV(1)), lenderName)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(1)), reasonBorrowed)
          .unsafeSet(LenderNamePage(srn, refineMV(2)), lenderName)
          .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(2)), reasonBorrowed)

        val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(2, "borrowings"), listPageUrl)
      }
    }

    "when moneyBorrowedPage true and more first pages than last pages is present - index 2 is missing" in {
      val customUserAnswers = defaultUserAnswers
        .unsafeSet(MoneyBorrowedPage(srn), true)
        .unsafeSet(LenderNamePage(srn, refineMV(1)), lenderName)
        .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(1)), reasonBorrowed)
        .unsafeSet(LenderNamePage(srn, refineMV(2)), lenderName)

      val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
      result mustBe (Recorded(1, "borrowings"), listPageUrl)
    }

    "when moneyBorrowedPage true and more first pages than last pages is present - index 1 is missing" in {
      val customUserAnswers = defaultUserAnswers
        .unsafeSet(MoneyBorrowedPage(srn), true)
        .unsafeSet(LenderNamePage(srn, refineMV(1)), lenderName)
        // missing here
        .unsafeSet(LenderNamePage(srn, refineMV(2)), lenderName)
        .unsafeSet(WhySchemeBorrowedMoneyPage(srn, refineMV(2)), reasonBorrowed)

      val result = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(customUserAnswers, srn)
      result mustBe (Recorded(1, "borrowings"), listPageUrl)
    }
  }

  "Shares status" - {
    val firstQuestionPageUrl =
      controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.shares.routes.SharesListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    def secondQuestionPageUrl(index: Max5000): String =
      controllers.nonsipp.shares.routes.TypeOfSharesHeldController
        .onPageLoad(srn, index, NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when only DidSchemeHoldAnyShares false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), false)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when DidSchemeHoldAnyShares is true and with 1 reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(TypeOfSharesHeldPage(srn, index1of5000), TypeOfShares.ConnectedParty)
          .unsafeSet(SharesCompleted(srn, index1of5000), SectionCompleted)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "shares"), listPageUrl)
      }

      "when DidSchemeHoldAnyShares is true and status is Recorded - first incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          // first share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(1)), TypeOfShares.Unquoted)
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)
          .unsafeSet(SharesCompleted(srn, refineMV(2)), SectionCompleted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "shares"), listPageUrl)
      }

      "when DidSchemeHoldAnyShares is true and status is Recorded - second incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          // first share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(1)), TypeOfShares.Unquoted)
          .unsafeSet(SharesCompleted(srn, refineMV(1)), SectionCompleted)
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "shares"), listPageUrl)
      }
    }

    "should be InProgress" - {
      "when only DidSchemeHoldAnyShares true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }

      "when DidSchemeHoldAnyShares is true and only first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(1)), TypeOfShares.Unquoted)
        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index1of5000))
      }

      "when DidSchemeHoldAnyShares is true and only second exist" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          // nothing for first share
          // second share:
          .unsafeSet(TypeOfSharesHeldPage(srn, refineMV(2)), TypeOfShares.Unquoted)

        val result = TaskListStatusUtils.getSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index2of5000))
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
    "should be Recorded" - {
      "when TotalValueQuotedSharesPage is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(TotalValueQuotedSharesPage(srn), money)
        val result = TaskListStatusUtils.getQuotedSharesTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded, totalValueQuotedSharesCyaUrl)
      }
    }
  }

  "Bonds status" - {
    val firstQuestionPageUrl =
      controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.bonds.routes.BondsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    def secondQuestionPageUrl(index: Max5000): String =
      controllers.nonsipp.bonds.routes.NameOfBondsController
        .onPageLoad(srn, index, NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when only UnregulatedOrConnectedBondsHeldPage false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when only UnregulatedOrConnectedBondsHeldPage true - first incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          // first bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(1)), "NameOfFirstBond")
          // second bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(2)), "NameOfSecondBond")
          .unsafeSet(BondsCompleted(srn, refineMV(2)), SectionCompleted)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "bonds"), listPageUrl)
      }

      "when only UnregulatedOrConnectedBondsHeldPage true - second incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          // first bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(1)), "NameOfFirstBond")
          .unsafeSet(BondsCompleted(srn, refineMV(1)), SectionCompleted)
          // second bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(2)), "NameOfSecondBond")
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "bonds"), listPageUrl)
      }
    }

    "should be InProgress" - {
      "when only UnregulatedOrConnectedBondsHeldPage true" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }

      "when only UnregulatedOrConnectedBondsHeldPage is true and only first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          .unsafeSet(NameOfBondsPage(srn, index1of5000), name)
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index1of5000))
      }

      "when only UnregulatedOrConnectedBondsHeldPage true and only second exist" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
          // nothing for the first bond:
          // second bond:
          .unsafeSet(NameOfBondsPage(srn, refineMV(2)), "NameOfSecondBond")
        val result = TaskListStatusUtils.getBondsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index2of5000))
      }
    }
  }

  "Other assets status" - {
    val firstQuestionPageUrl =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    def secondQuestionPageUrl(index: Max5000): String =
      controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
        .onPageLoad(srn, index, NormalMode)
        .url

    "should be Not Started" - {
      "when default data" in {
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(defaultUserAnswers, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when OtherAssetsHeldPage is false" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), false)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when OtherAssetsHeldPage is true - first asset incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          // first asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(1)), "asset one")
          // second asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "asset two")
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(2)), SectionCompleted)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "otherAssets"), listPageUrl)
      }

      "when OtherAssetsHeldPage is true  - second asset incomplete" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          // first asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(1)), "asset one")
          .unsafeSet(OtherAssetsCompleted(srn, refineMV(1)), SectionCompleted)
          // second asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "asset two")
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "otherAssets"), listPageUrl)
      }
    }

    "should be InProgress" - {
      "when only OtherAssetsHeldPage is true" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }

      "when OtherAssetsHeldPage is true and only first page is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index1of5000), otherAssetDescription)
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index1of5000))
      }

      "when OtherAssetsHeldPage is true and JourneyStatus is InProgress - first asset removed" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          // nothing for the first asset:
          // second asset:
          .unsafeSet(WhatIsOtherAssetPage(srn, refineMV(2)), "asset two")
        val result = TaskListStatusUtils.getOtherAssetsTaskListStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, secondQuestionPageUrl(index2of5000))
      }
    }
  }

  "Employer contributions status" - {
    val firstQuestionPageUrl =
      controllers.nonsipp.employercontributions.routes.EmployerContributionsController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, firstQuestionPageUrl)
      }
    }

    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be In Progress" - {
      "when only employer contributions true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when employer contributions false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), false)
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when employer contributions true is present with 1 contribution recorded" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerContributionsCompleted(srn, index1of300, index1of50), SectionCompleted)
        val result = TaskListStatusUtils.getEmployerContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "contributions"), listPageUrl)
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
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
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

    "should be Recorded" - {
      "when UnallocatedEmployerContributionsPage is true and UnallocatedEmployerAmountPage is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
          .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

        val result = TaskListStatusUtils.getUnallocatedContributionsStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded, cyaPageUrl)
      }
    }
  }

  "Transfer in status" - {

    val firstQuestionPageUrl =
      controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getTransferInStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, firstQuestionPageUrl)
      }
    }

    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getTransferInStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be In Progress" - {
      "when only did scheme received transfer true is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
        val result = TaskListStatusUtils.getTransferInStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when did scheme received transfer false is present" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeReceiveTransferPage(srn), false)
        val result = TaskListStatusUtils.getTransferInStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when did scheme received transfer true is present with 1 transfer recorded" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
          .unsafeSet(TransfersInSectionCompleted(srn, index1of300, index1of5), SectionCompleted)
        val result = TaskListStatusUtils.getTransferInStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "transfers"), listPageUrl)
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

    "should be Recorded" - {
      "when SchemeTransferOutPage is false" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SchemeTransferOutPage(srn), false)
        val result = TaskListStatusUtils.getTransferOutStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), wereTransfersOut)
      }

      "when SchemeTransferOutPage is true and Transfer Out has been reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SchemeTransferOutPage(srn), true)
          .unsafeSet(TransfersOutSectionCompleted(srn, index1of300, index1of5), SectionCompleted)
        val result = TaskListStatusUtils.getTransferOutStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "transfers"), selectMember)
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

    "should be Recorded" - {
      "when SurrenderedBenefitsPage is false" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SurrenderedBenefitsPage(srn), false)
        val result = TaskListStatusUtils.getSurrenderedBenefitsStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), wereSurrenderedBenefits)
      }

      "when SurrenderedBenefitsPage is true and Surrendered Benefit has been reported" in {
        val customUserAnswers = defaultUserAnswers
          .unsafeSet(SurrenderedBenefitsPage(srn), true)
          .unsafeSet(SurrenderedBenefitsCompletedPage(srn, index1of300), SectionCompleted)
        val result = TaskListStatusUtils.getSurrenderedBenefitsStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "surrenders"), selectMember)
      }
    }
  }

  "Member contributions status" - {

    val firstQuestionPageUrl =
      controllers.nonsipp.membercontributions.routes.MemberContributionsController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.membercontributions.routes.MemberContributionListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, firstQuestionPageUrl)
      }
    }

    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be In Progress" - {
      "when only member contributions true is present" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(MemberContributionsPage(srn), true)
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when only member contributions false is present" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(MemberContributionsPage(srn), false)
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when member contributions is true and member contributions list page is true" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(MemberContributionsPage(srn), true)
          .unsafeSet(TotalMemberContributionPage(srn, index1of300), money)
        val result = TaskListStatusUtils.getMemberContributionStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "contributions"), listPageUrl)
      }
    }
  }

  "PCLS status" - {

    val firstQuestionPageUrl =
      controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getPclsStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, firstQuestionPageUrl)
      }
    }
    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getPclsStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be In Progress" - {
      "when only PCLS received true is present" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionCommencementLumpSumPage(srn), true)
        val result = TaskListStatusUtils.getPclsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when PCLS received is false" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionCommencementLumpSumPage(srn), false)
        val result = TaskListStatusUtils.getPclsStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when PCLS received is true with 1 PCLS recorded" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionCommencementLumpSumPage(srn), true)
          .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index1of300), pcls)
        val result = TaskListStatusUtils.getPclsStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "pcls"), listPageUrl)
      }
    }
  }

  "Pension payments status" - {

    val firstQuestionPageUrl =
      controllers.nonsipp.memberpensionpayments.routes.PensionPaymentsReceivedController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    "should be Unable to start" - {
      "when default data" in {
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(defaultUserAnswers, srn)
        result mustBe (UnableToStart, firstQuestionPageUrl)
      }
    }

    "should be Not Started" - {
      "when members are added" in {
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(defaultUserAnswersWithMember, srn)
        result mustBe (NotStarted, firstQuestionPageUrl)
      }
    }

    "should be In Progress" - {
      "when only pension payments received true is present" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionPaymentsReceivedPage(srn), true)
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(customUserAnswers, srn)
        result mustBe (InProgress, firstQuestionPageUrl)
      }
    }

    "should be Recorded" - {
      "when pension payments received false" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionPaymentsReceivedPage(srn), false)
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(0, ""), firstQuestionPageUrl)
      }

      "when were pension payments received is true with 1 payment recorded" in {
        val customUserAnswers = defaultUserAnswersWithMember
          .unsafeSet(PensionPaymentsReceivedPage(srn), true)
          .unsafeSet(TotalAmountPensionPaymentsPage(srn, index1of300), money)
        val result = TaskListStatusUtils.getPensionPaymentsStatusAndLink(customUserAnswers, srn)
        result mustBe (Recorded(1, "payments"), listPageUrl)
      }
    }
  }

  "\"userAnswersUnchangedAllSections\" method" - {
    "should return true when UAs are identical" in {
      userAnswersUnchangedAllSections(currentUA, currentUA)
    }

    "should return false when UAs aren't identical" in {
      userAnswersUnchangedAllSections(currentUA, previousUA)
    }
  }
}
