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

package controllers.nonsipp

import models.ConditionalYesNo._
import pages.nonsipp.otherassetsheld._
import config.Refined._
import controllers.ControllerBaseSpec
import models.SchemeHoldShare._
import pages.nonsipp.landorproperty._
import pages.nonsipp.receivetransfer._
import pages.nonsipp.landorpropertydisposal._
import pages.nonsipp.membersurrenderedbenefits._
import models.{ConditionalYesNo, _}
import models.SponsoringOrConnectedParty._
import org.mockito.ArgumentMatchers._
import pages.nonsipp.employercontributions._
import services._
import pages.nonsipp.otherassetsdisposal._
import pages.nonsipp.schemedesignatory._
import pages.nonsipp.memberdetails._
import pages.nonsipp.totalvaluequotedshares._
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds._
import pages.nonsipp.membercontributions._
import pages.nonsipp.memberreceivedpcls._
import pages.nonsipp.shares._
import play.api.mvc.Call
import models.ManualOrUpload._
import models.PensionSchemeType._
import models.IdentityType._
import views.html.TaskListView
import models.TypeOfShares._
import pages.nonsipp.memberpensionpayments._
import eu.timepit.refined.refineMV
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import pages.nonsipp.sharesdisposal._
import pages.nonsipp.{CheckReturnDatesPage, WhichTaxYearPage}
import play.api.inject
import uk.gov.hmrc.domain.Nino
import models.HowSharesDisposed._
import viewmodels.models.TaskListStatus.{Completed, TaskListStatus, Updated}
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject._
import pages.nonsipp.membertransferout._
import pages.nonsipp.moneyborrowed._
import pages.nonsipp.bondsdisposal._
import pages.nonsipp.memberpayments._
import viewmodels.models._

import scala.concurrent.Future

class ViewOnlyTaskListControllerSpec extends ControllerBaseSpec with CommonTestValues {

  // Set up services
  private val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]

  override val additionalBindings: List[GuiceableModule] =
    List(
      inject.bind[PsrVersionsService].toInstance(mockPsrVersionsService)
    )

  override def beforeEach(): Unit =
    when(mockPsrVersionsService.getVersions(any(), any())(any(), any())).thenReturn(Future.successful(Seq()))

  // Set test values
  private val index1of300: Max300 = refineMV(1)
  private val index1of50: Max50 = refineMV(1)
  private val index1of5: Max5 = refineMV(1)
  private val index1of5000: Max5000 = refineMV(1)
  private val name: String = "name"
  private val reason: String = "reason"
  private val numAccountingPeriods: Max3 = refineMV(1)

  // Build userAnswers for current version
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
    .unsafeSet(TypeOfSharesHeldPage(srn, index1of5000), SponsoringEmployer)
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

  private val pureUA: UserAnswers = currentUA.copy()

  lazy val viewModelSubmissionTwo: PageViewModel[TaskListViewModel] = ViewOnlyTaskListController.viewModel(
    srn,
    schemeName,
    dateRange,
    currentUA,
    previousUA,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  lazy val onPageLoadSubmissionTwo: Call = routes.ViewOnlyTaskListController.onPageLoad(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  // Build userAnswers for previous version
  private val previousUA: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), dateRange) // automatically set
    // Section 1 - Scheme Details
    // (S1) Basic Details
    .unsafeSet(CheckReturnDatesPage(srn), false)
    .unsafeSet(AccountingPeriodPage(srn, numAccountingPeriods, NormalMode), dateRange)
    .unsafeSet(ActiveBankAccountPage(srn), false)
    .unsafeSet(WhyNoBankAccountPage(srn), reason)
    .unsafeSet(HowManyMembersPage(srn, psaId), schemeMemberNumbers)
    // (S1) Financial Details
    .unsafeSet(HowMuchCashPage(srn, NormalMode), MoneyInPeriod(money, Money(2)))
    .unsafeSet(ValueOfAssetsPage(srn, NormalMode), MoneyInPeriod(money, Money(3)))
    .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), Money(1))
    // Section 2 - Member Details
    .unsafeSet(PensionSchemeMembersPage(srn), Upload)
    .unsafeSet(CheckMemberDetailsFilePage(srn), true)
    .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), false)
    .unsafeSet(NoNINOPage(srn, index1of300), reason)
    .unsafeSet(MemberStatus(srn, index1of300), MemberState.New)
    // Section 3 - Member Payments
    // (S3) Employer Contributions
    .unsafeSet(EmployerContributionsPage(srn), true)
    .unsafeSet(EmployerNamePage(srn, index1of300, index1of50), name)
    .unsafeSet(EmployerTypeOfBusinessPage(srn, index1of300, index1of50), UKPartnership)
    .unsafeSet(PartnershipEmployerUtrPage(srn, index1of300, index1of50), ConditionalYesNo.yes[String, Utr](utr))
    .unsafeSet(OtherEmployeeDescriptionPage(srn, index1of300, index1of50), otherDetails)
    .unsafeSet(TotalEmployerContributionPage(srn, index1of300, index1of50), money)
    .unsafeSet(ContributionsFromAnotherEmployerPage(srn, index1of300, index1of50), false)
    // (S3) Unallocated Employer Contributions
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), false)
    // (S3) Member Contributions
    .unsafeSet(MemberContributionsPage(srn), true)
    .unsafeSet(TotalMemberContributionPage(srn, index1of300), money)
    // (S3) Transfers In
    .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
    .unsafeSet(TransferringSchemeNamePage(srn, index1of300, index1of5), name)
    .unsafeSet(
      TransferringSchemeTypePage(srn, index1of300, index1of5),
      QualifyingRecognisedOverseasPS(qropsReferenceNumber)
    )
    .unsafeSet(TotalValueTransferPage(srn, index1of300, index1of5), money)
    .unsafeSet(WhenWasTransferReceivedPage(srn, index1of300, index1of5), localDate)
    .unsafeSet(DidTransferIncludeAssetPage(srn, index1of300, index1of5), false)
    // (S3) Transfers Out
    .unsafeSet(SchemeTransferOutPage(srn), true)
    .unsafeSet(ReceivingSchemeNamePage(srn, index1of300, index1of5), name)
    .unsafeSet(ReceivingSchemeTypePage(srn, index1of300, index1of5), RegisteredPS(pstr))
    .unsafeSet(WhenWasTransferMadePage(srn, index1of300, index1of5), localDate)
    // (S3) PCLS
    .unsafeSet(PensionCommencementLumpSumPage(srn), false)
    // (S3) Pension Payments
    .unsafeSet(PensionPaymentsReceivedPage(srn), true)
    .unsafeSet(TotalAmountPensionPaymentsPage(srn, index1of300), money)
    // (S3) Surrendered Benefits
    .unsafeSet(SurrenderedBenefitsPage(srn), true)
    .unsafeSet(SurrenderedBenefitsAmountPage(srn, index1of300), money)
    .unsafeSet(WhenDidMemberSurrenderBenefitsPage(srn, index1of300), localDate)
    .unsafeSet(WhyDidMemberSurrenderBenefitsPage(srn, index1of300), "")
    // Section 4 - Loans Made & Money Borrowed
    // (S4) Loans Made
    .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
    .unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, index1of5000), name)
    .unsafeSet(CompanyRecipientCrnPage(srn, index1of5000, LoanRecipient), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index1of5000), Sponsoring)
    .unsafeSet(DatePeriodLoanPage(srn, index1of5000), (localDate, money, loanPeriod))
    .unsafeSet(AmountOfTheLoanPage(srn, index1of5000), (money, money, money))
    .unsafeSet(AreRepaymentsInstalmentsPage(srn, index1of5000), true)
    .unsafeSet(InterestOnLoanPage(srn, index1of5000), (money, percentage, money))
    .unsafeSet(SecurityGivenForLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Security](security))
    .unsafeSet(OutstandingArrearsOnLoanPage(srn, index1of5000), ConditionalYesNo.yes[Unit, Money](money))
    // (S4) Money Borrowed
    .unsafeSet(MoneyBorrowedPage(srn), true)
    .unsafeSet(LenderNamePage(srn, index1of5000), lenderName)
    .unsafeSet(IsLenderConnectedPartyPage(srn, index1of5000), false)
    .unsafeSet(BorrowedAmountAndRatePage(srn, index1of5000), (money, percentage))
    .unsafeSet(WhenBorrowedPage(srn, index1of5000), localDate)
    .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index1of5000), money)
    .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index1of5000), reason)
    // Section 5 - Shares
    // (S5) Shares
    .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
    .unsafeSet(TypeOfSharesHeldPage(srn, index1of5000), Unquoted)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index1of5000), Contribution)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index1of5000), localDate)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, index1of5000), name)
    .unsafeSet(SharesCompanyCrnPage(srn, index1of5000), ConditionalYesNo.no[String, Crn](reason))
    .unsafeSet(ClassOfSharesPage(srn, index1of5000), classOfShares)
    .unsafeSet(HowManySharesPage(srn, index1of5000), totalShares)
    .unsafeSet(SharesFromConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(CostOfSharesPage(srn, index1of5000), money)
    .unsafeSet(SharesIndependentValuationPage(srn, index1of5000), true)
    .unsafeSet(SharesTotalIncomePage(srn, index1of5000), money)
    .unsafeSet(SharesListPage(srn), false)
    .unsafeSet(SharesCompleted(srn, index1of5000), SectionCompleted)
    // (S5) Shares Disposals
    .unsafeSet(SharesDisposalPage(srn), true)
    .unsafeSet(HowWereSharesDisposedPage(srn, index1of5000, index1of50), Redeemed)
    .unsafeSet(WhenWereSharesRedeemedPage(srn, index1of5000, index1of50), localDate)
    .unsafeSet(HowManySharesRedeemedPage(srn, index1of5000, index1of50), totalShares)
    .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, index1of5000, index1of50), money)
    .unsafeSet(HowManyDisposalSharesPage(srn, index1of5000, index1of50), totalShares)
    // (S5) Quoted Shares
    .unsafeSet(TotalValueQuotedSharesPage(srn), Money(0))
    // Section 6 - Land or Property
    // (S6) Land or Property
    .unsafeSet(LandOrPropertyHeldPage(srn), true)
    .unsafeSet(LandPropertyInUKPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), internationalAddress)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes[String, String](titleNumber))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Contribution)
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index1of5000), false)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index1of5000), money)
    .unsafeSet(LandOrPropertyCompleted(srn, index1of5000), SectionCompleted)
    // (S6) Land or Property Disposals
    .unsafeSet(LandOrPropertyDisposalPage(srn), true)
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Sold)
    .unsafeSet(WhenWasPropertySoldPage(srn, index1of5000, index1of50), localDate)
    .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, index1of5000, index1of50), money)
    .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index1of5000, index1of50), UKPartnership)
    .unsafeSet(pages.nonsipp.landorpropertydisposal.PartnershipBuyerNamePage(srn, index1of5000, index1of50), name)
    .unsafeSet(
      pages.nonsipp.landorpropertydisposal.PartnershipBuyerUtrPage(srn, index1of5000, index1of50),
      ConditionalYesNo.no[String, Utr](reason)
    )
    .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index1of5000, index1of50), true)
    .unsafeSet(DisposalIndependentValuationPage(srn, index1of5000, index1of50), true)
    .unsafeSet(LandOrPropertyStillHeldPage(srn, index1of5000, index1of50), true)
    // Section 7 - Bonds
    // (S7) Bonds
    .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
    .unsafeSet(NameOfBondsPage(srn, index1of5000), name)
    .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index1of5000), SchemeHoldBond.Contribution)
    .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index1of5000), localDate)
    .unsafeSet(CostOfBondsPage(srn, index1of5000), money)
    .unsafeSet(BondsFromConnectedPartyPage(srn, index1of5000), true)
    .unsafeSet(AreBondsUnregulatedPage(srn, index1of5000), true)
    .unsafeSet(IncomeFromBondsPage(srn, index1of5000), money)
    .unsafeSet(BondsCompleted(srn, index1of5000), SectionCompleted)
    // (S7) Bonds Disposals
    .unsafeSet(BondsDisposalPage(srn), true)
    .unsafeSet(HowWereBondsDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Sold)
    .unsafeSet(WhenWereBondsSoldPage(srn, index1of5000, index1of50), localDate)
    .unsafeSet(TotalConsiderationSaleBondsPage(srn, index1of5000, index1of50), money)
    .unsafeSet(BuyerNamePage(srn, index1of5000, index1of50), name)
    .unsafeSet(pages.nonsipp.bondsdisposal.IsBuyerConnectedPartyPage(srn, index1of5000, index1of50), false)
    .unsafeSet(BondsStillHeldPage(srn, index1of5000, index1of50), bondsStillHeld)
    // Section 8 - Other Assets
    // (S8) Other Assets
    .unsafeSet(OtherAssetsHeldPage(srn), true)
    .unsafeSet(WhatIsOtherAssetPage(srn, index1of5000), otherAssetDescription)
    .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index1of5000), false)
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index1of5000), SchemeHoldAsset.Contribution)
    .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index1of5000), localDate)
    .unsafeSet(CostOfOtherAssetPage(srn, index1of5000), money)
    .unsafeSet(pages.nonsipp.otherassetsheld.IndependentValuationPage(srn, index1of5000), false)
    .unsafeSet(IncomeFromAssetPage(srn, index1of5000), money)
    .unsafeSet(OtherAssetsCompleted(srn, index1of5000), SectionCompleted)
    // (S8) Other Assets Disposals
    .unsafeSet(OtherAssetsDisposalPage(srn), true)
    .unsafeSet(HowWasAssetDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Sold)
    .unsafeSet(WhenWasAssetSoldPage(srn, index1of5000, index1of50), localDate)
    .unsafeSet(TotalConsiderationSaleAssetPage(srn, index1of5000, index1of50), money)
    .unsafeSet(TypeOfAssetBuyerPage(srn, index1of5000, index1of50), UKPartnership)
    .unsafeSet(pages.nonsipp.otherassetsdisposal.PartnershipBuyerNamePage(srn, index1of5000, index1of50), name)
    .unsafeSet(
      pages.nonsipp.otherassetsdisposal.PartnershipBuyerUtrPage(srn, index1of5000, index1of50),
      ConditionalYesNo.no[String, Utr](reason)
    )
    .unsafeSet(pages.nonsipp.otherassetsdisposal.IsBuyerConnectedPartyPage(srn, index1of5000, index1of50), false)
    .unsafeSet(AssetSaleIndependentValuationPage(srn, index1of5000, index1of50), false)
    .unsafeSet(AnyPartAssetStillHeldPage(srn, index1of5000, index1of50), false)

  lazy val viewModelVersionOne: PageViewModel[TaskListViewModel] = ViewOnlyTaskListController.viewModel(
    srn,
    schemeName,
    dateRange,
    currentUA,
    currentUA,
    yearString,
    submissionNumberOne,
    submissionNumberZero
  )

  lazy val onPageLoadSubmissionOne: Call = routes.ViewOnlyTaskListController.onPageLoad(
    srn,
    yearString,
    submissionNumberOne,
    0
  )

  "ViewOnlyTaskListController" - {

    act.like(
      renderView(onPageLoadSubmissionTwo, currentUA, pureUA, Some(previousUA)) { implicit app => implicit request =>
        val view = injected[TaskListView]
        view(viewModelSubmissionTwo)
      }.withName("onPageLoad renders ok with indexes /2/1")
    )

    act.like(journeyRecoveryPage(onPageLoadSubmissionTwo).updateName("onPageLoad " + _))

    act.like(
      renderView(onPageLoadSubmissionOne, currentUA, pureUA, None) { implicit app => implicit request =>
        val view = injected[TaskListView]
        view(viewModelVersionOne)
      }.withName("onPageLoad renders ok with indexes /1/0")
    )

    // TODO: implement lower-level journey navigation in future ticket, until then Unauthorised page used for all links

    "Status, content, and URL tests" - {

      "Section 1 - Scheme details" - {

        "(S1) Basic details" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              0,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.details.title",
              expectedLinkUrl = controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
                .onPageLoadViewOnly(srn, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              0,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.details.title",
              expectedLinkUrl = controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
                .onPageLoadViewOnly(srn, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S1) Financial details" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              0,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.finances.title",
              expectedLinkUrl = controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
                .onPageLoadViewOnly(srn, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              0,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.schemedetails.title",
              expectedLinkContentKey = "nonsipp.tasklist.schemedetails.view.finances.title",
              expectedLinkUrl = controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
                .onPageLoadViewOnly(srn, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }
      }

      "Section 2 - Members" - {

        "Completed" in {
          testViewModel(
            currentUA,
            currentUA,
            1,
            0,
            expectedStatus = Completed,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.view.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.SchemeMembersListController
              .onPageLoadViewOnly(srn, page = 1, yearString, submissionNumberTwo, submissionNumberOne)
              .url
          )
        }

        "Updated" in {
          testViewModel(
            currentUA,
            previousUA,
            1,
            0,
            expectedStatus = Updated,
            expectedTitleKey = "nonsipp.tasklist.members.title",
            expectedLinkContentKey = "nonsipp.tasklist.members.view.details.title",
            expectedLinkUrl = controllers.nonsipp.memberdetails.routes.SchemeMembersListController
              .onPageLoadViewOnly(srn, page = 1, yearString, submissionNumberTwo, submissionNumberOne)
              .url
          )
        }
      }

      "Section 3 - Member payments" - {

        "(S3) Employer contributions" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.employercontributions.title",
              expectedLinkUrl =
                controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
                  .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                  .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.employercontributions.title",
              expectedLinkUrl =
                controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
                  .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                  .url
            )
          }
        }

        "(S3) Unallocated employer contributions" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.unallocatedcontributions.title",
              expectedLinkUrl = controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
                .onPageLoadViewOnly(srn, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.unallocatedcontributions.title",
              expectedLinkUrl = controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
                .onPageLoadViewOnly(srn, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S3) Member contributions" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              2,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.memberContributions.title",
              expectedLinkUrl = controllers.nonsipp.membercontributions.routes.MemberContributionListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              2,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.memberContributions.title",
              expectedLinkUrl = controllers.nonsipp.membercontributions.routes.MemberContributionListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }
        "(S3) Transfers in" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              3,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersreceived.title",
              expectedLinkUrl = controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              3,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersreceived.title",
              expectedLinkUrl = controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S3) Transfers out" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              4,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersout.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              4,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.transfersout.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }

        "(S3) Pension commencement lump sum" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              5,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.pcls.title",
              expectedLinkUrl = controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              5,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.pcls.title",
              expectedLinkUrl = controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S3) Pension payments" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              6,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.payments.title",
              expectedLinkUrl = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              6,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.payments.title",
              expectedLinkUrl = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S3) Surrendered benefits" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              2,
              7,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.surrenderedbenefits.title",
              expectedLinkUrl =
                controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
                  .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                  .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              2,
              7,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.memberpayments.title",
              expectedLinkContentKey = "nonsipp.tasklist.memberpayments.view.surrenderedbenefits.title",
              expectedLinkUrl =
                controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
                  .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                  .url
            )
          }
        }
      }

      "Section 4 - Loans made and money borrowed" - {

        "(S4) Loans made" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              3,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.loansmade.title",
              expectedLinkUrl = controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              3,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.loansmade.title",
              expectedLinkUrl = controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S4) Money borrowed" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              3,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.moneyborrowed.title",
              expectedLinkUrl = controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              3,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.loans.title",
              expectedLinkContentKey = "nonsipp.tasklist.loans.view.moneyborrowed.title",
              expectedLinkUrl = controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }
      }

      "Section 5 - Shares" - {

        "(S5) Shares" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              4,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.shares.view.sponsoringemployer.title",
              expectedLinkUrl = controllers.nonsipp.shares.routes.SharesListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              4,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.shares.view.sponsoringemployer.title",
              expectedLinkUrl = controllers.nonsipp.shares.routes.SharesListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S5) Shares Disposals" - {

          "Not Visible with no shares completed" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(SharesCompleted(srn, index1of5000)),
              currentUA.unsafeRemove(SharesCompleted(srn, index1of5000)),
              4,
              1,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedVisibility = false
            )
          }

          "Visible with shares completed in either version" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(SharesCompleted(srn, index1of5000)),
              currentUA,
              4,
              1,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedVisibility = true
            )

            testViewModelVisibility(
              currentUA,
              currentUA.unsafeRemove(SharesCompleted(srn, index1of5000)),
              4,
              1,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedVisibility = true
            )
          }

          "Visible with shares completed in both versions" in {
            testViewModelVisibility(
              currentUA,
              currentUA,
              4,
              1,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedVisibility = true
            )
          }

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              4,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              4,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.shares.title",
              expectedLinkContentKey = "nonsipp.tasklist.sharesdisposal.view.title",
              expectedLinkUrl = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
                .url
            )
          }
        }

        "(S8) Quoted Shares" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              7,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassets.view.quotedshares.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              7,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassets.view.quotedshares.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }
      }

      "Section 6 - Land or property" - {

        "(S6) Land or property" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              5,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorproperty.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              5,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorproperty.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }

        "(S6) Land or Property Disposals" - {

          "Not Visible with no land or property completed" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(LandOrPropertyCompleted(srn, index1of5000)),
              currentUA.unsafeRemove(LandOrPropertyCompleted(srn, index1of5000)),
              5,
              1,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorpropertydisposal.view.title",
              expectedVisibility = false
            )
          }

          "Visible with land or property completed in either version" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(LandOrPropertyCompleted(srn, index1of5000)),
              currentUA,
              5,
              1,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorpropertydisposal.view.title",
              expectedVisibility = true
            )

            testViewModelVisibility(
              currentUA,
              currentUA.unsafeRemove(LandOrPropertyCompleted(srn, index1of5000)),
              5,
              1,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorpropertydisposal.view.title",
              expectedVisibility = true
            )
          }

          "Visible with land or property completed in both versions" in {
            testViewModelVisibility(
              currentUA,
              currentUA,
              5,
              1,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorpropertydisposal.view.title",
              expectedVisibility = true
            )
          }

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              5,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorpropertydisposal.view.title",
              expectedLinkUrl = controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
                .onPageLoadViewOnly(
                  srn,
                  page = 1,
                  yearString,
                  current = submissionNumberTwo,
                  previous = submissionNumberOne
                )
                .url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              5,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.landorproperty.title",
              expectedLinkContentKey = "nonsipp.tasklist.landorpropertydisposal.view.title",
              expectedLinkUrl = controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
                .onPageLoadViewOnly(
                  srn,
                  page = 1,
                  yearString,
                  current = submissionNumberTwo,
                  previous = submissionNumberOne
                )
                .url
            )
          }
        }
      }

      "Section 7 - Bonds" - {

        "(S7) Bonds" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              6,
              0,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.unregulatedorconnected.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              6,
              0,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.unregulatedorconnected.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }

        "(S7) Bonds Disposals" - {

          "Not Visible with no bonds completed" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(BondsCompleted(srn, index1of5000)),
              currentUA.unsafeRemove(BondsCompleted(srn, index1of5000)),
              6,
              1,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.bondsdisposal.title",
              expectedVisibility = false
            )
          }

          "Visible with bonds completed in either version" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(BondsCompleted(srn, index1of5000)),
              currentUA,
              6,
              1,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.bondsdisposal.title",
              expectedVisibility = true
            )

            testViewModelVisibility(
              currentUA,
              currentUA.unsafeRemove(BondsCompleted(srn, index1of5000)),
              6,
              1,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.bondsdisposal.title",
              expectedVisibility = true
            )
          }

          "Visible with bonds completed in both versions" in {
            testViewModelVisibility(
              currentUA,
              currentUA,
              6,
              1,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.bondsdisposal.title",
              expectedVisibility = true
            )
          }

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              6,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.bondsdisposal.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              6,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.bonds.title",
              expectedLinkContentKey = "nonsipp.tasklist.bonds.view.bondsdisposal.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }
      }

      "Section 8 - Other assets" - {

        "(S8) Other Assets" - {

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              7,
              1,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassets.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              7,
              1,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassets.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }

        "(S8) Other Assets Disposals" - {

          "Not Visible with no other assets completed" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(OtherAssetsCompleted(srn, index1of5000)),
              currentUA.unsafeRemove(OtherAssetsCompleted(srn, index1of5000)),
              7,
              2,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.view.title",
              expectedVisibility = false
            )
          }

          "Visible with other assets completed in either version" in {
            testViewModelVisibility(
              currentUA.unsafeRemove(OtherAssetsCompleted(srn, index1of5000)),
              currentUA,
              7,
              2,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.view.title",
              expectedVisibility = true
            )

            testViewModelVisibility(
              currentUA,
              currentUA.unsafeRemove(OtherAssetsCompleted(srn, index1of5000)),
              7,
              2,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.view.title",
              expectedVisibility = true
            )
          }

          "Visible with other assets completed in both versions" in {
            testViewModelVisibility(
              currentUA,
              currentUA,
              7,
              2,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.view.title",
              expectedVisibility = true
            )
          }

          "Completed" in {
            testViewModel(
              currentUA,
              currentUA,
              7,
              2,
              expectedStatus = Completed,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }

          "Updated" in {
            testViewModel(
              currentUA,
              previousUA,
              7,
              2,
              expectedStatus = Updated,
              expectedTitleKey = "nonsipp.tasklist.otherassets.title",
              expectedLinkContentKey = "nonsipp.tasklist.otherassetsdisposal.view.title",
              expectedLinkUrl = controllers.routes.UnauthorisedController.onPageLoad().url
            )
          }
        }
      }

      "Section 9 - Declaration" - {

        "Completed" in {
          testViewModel(
            currentUA,
            currentUA,
            8,
            0,
            expectedStatus = Completed,
            expectedTitleKey = "nonsipp.tasklist.declaration.title",
            expectedLinkContentKey = "nonsipp.tasklist.declaration.view",
            expectedLinkUrl = controllers.nonsipp.routes.ViewOnlyReturnSubmittedController
              .onPageLoad(srn, dateRange.from.toString, submissionNumberTwo)
              .url
          )
        }
      }
    }
  }

  private def testViewModel(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    sectionIndex: Int,
    itemIndex: Int,
    expectedStatus: TaskListStatus,
    expectedTitleKey: String,
    expectedLinkContentKey: String,
    expectedLinkUrl: String
  ): Object = {
    val customViewModel = ViewOnlyTaskListController.viewModel(
      srn,
      schemeName,
      dateRange,
      currentUA,
      previousUA,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
    val sections = customViewModel.page.sections.toList
    sections(sectionIndex).title.key mustBe expectedTitleKey
    sections(sectionIndex).items.fold(
      _ => "",
      list => {
        val item = list.toList(itemIndex)
        item.status mustBe expectedStatus
        item.link.content.key mustBe expectedLinkContentKey
        item.link.url mustBe expectedLinkUrl
      }
    )
  }

  private def testViewModelVisibility(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    sectionIndex: Int,
    itemIndex: Int,
    expectedTitleKey: String,
    expectedLinkContentKey: String,
    expectedVisibility: Boolean
  ): Unit = {
    val customViewModel = ViewOnlyTaskListController.viewModel(
      srn,
      schemeName,
      dateRange,
      currentUA,
      previousUA,
      yearString,
      submissionNumberTwo,
      submissionNumberOne
    )
    val sections = customViewModel.page.sections.toList

    if (expectedVisibility) {
      sections(sectionIndex).title.key mustBe expectedTitleKey
      sections(sectionIndex).items.fold(
        _ => "",
        list => {
          val item = list.toList(itemIndex)
          item.link.content.key mustBe expectedLinkContentKey
        }
      )
    } else {
      val section: List[TaskListItemViewModel] = sections(sectionIndex).items.fold(_ => List(), list => {
        list.toList
      })
      section.map(_.link.content.key) must not contain expectedLinkContentKey
    }
  }
}
