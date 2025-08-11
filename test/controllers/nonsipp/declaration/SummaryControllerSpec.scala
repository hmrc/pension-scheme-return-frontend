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

package controllers.nonsipp.declaration

import play.api.test.FakeRequest
import models.ConditionalYesNo._
import pages.nonsipp.shares._
import models.ManualOrUpload.Manual
import play.api.inject.bind
import pages.nonsipp.landorproperty._
import pages.nonsipp.receivetransfer._
import pages.nonsipp.landorpropertydisposal._
import pages.nonsipp.memberpensionpayments.{PensionPaymentsReceivedPage, TotalAmountPensionPaymentsPage}
import pages.nonsipp.membersurrenderedbenefits._
import models._
import pages.nonsipp.common._
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.employercontributions._
import services._
import pages.nonsipp.otherassetsdisposal.OtherAssetsDisposalPage
import pages.nonsipp.schemedesignatory._
import pages.nonsipp.memberdetails._
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds._
import pages.nonsipp.membercontributions.{MemberContributionsPage, TotalMemberContributionPage}
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import pages.nonsipp.otherassetsheld._
import models.PointOfEntry.{HowWereBondsDisposedPointOfEntry, HowWereSharesDisposedPointOfEntry}
import viewmodels.models.MemberState.New
import controllers.{ControllerBaseSpec, ControllerBehaviours, TestUserAnswers}
import pages.nonsipp.accountingperiod.{AccountingPeriodPage, AccountingPeriodRecordVersionPage}
import pages.nonsipp.sharesdisposal._
import pages.nonsipp._
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.loansmadeoroutstanding._
import org.scalatest.matchers.should.Matchers.should
import pages.nonsipp.membertransferout._
import pages.nonsipp.moneyborrowed._
import pages.nonsipp.declaration.PensionSchemeDeclarationPage
import pages.nonsipp.bondsdisposal._
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import viewmodels.models._

class SummaryControllerSpec
    extends ControllerBaseSpec
    with ControllerBehaviours
    with BeforeAndAfterEach
    with TestUserAnswers
    with CommonTestValues {

  val index = index1of5000
  // Shares parameters
  private val dateSharesSold = Some(localDate)
  private val numberSharesSold = Some(totalShares)
  private val considerationSharesSold = Some(money)
  private val nameOfBuyer = Some(buyerName)
  private val isBuyerConnectedParty = Some(true)
  private val isIndependentValuation = Some(true)
  private val sharesStillHeld = totalShares - 1

  private val dateSold = Some(localDate)
  private val considerationAssetSold = Some(money)

  private val populatedUserAnswers =
    defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), version)
      .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersUnderThreshold)

      // basic details
      .unsafeSet(WhichTaxYearPage(srn), currentReturnTaxYear)
      .unsafeSet(CheckReturnDatesPage(srn), true)
      .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), currentReturnTaxYear)
      .unsafeSet(AccountingPeriodRecordVersionPage(srn), recordVersion)
      .unsafeSet(ActiveBankAccountPage(srn), true)
      .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
      .unsafeSet(SchemeDesignatoryRecordVersionPage(srn), recordVersion)
      .unsafeSet(FbVersionPage(srn), recordVersion)
      .unsafeSet(FbStatus(srn), Submitted)
      .unsafeSet(CompilationOrSubmissionDatePage(srn), currentReturnTaxYearSubmissionDate)
      .unsafeSet(PensionSchemeDeclarationPage(srn), declarationData)
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), false)
      .unsafeSet(NoNINOPage(srn, index1of300), noninoReason)
      .unsafeSet(MemberStatus(srn, index1of300), New)
      .unsafeSet(MemberDetailsCompletedPage(srn, index1of300), SectionCompleted)

      // financial details
      .unsafeSet(HowMuchCashPage(srn, NormalMode), moneyInPeriod)
      .unsafeSet(ValueOfAssetsPage(srn, NormalMode), moneyInPeriod)
      .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), money)

      // member details
      .unsafeSet(PensionSchemeMembersPage(srn), Manual)
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), true)
      .unsafeSet(MemberDetailsNinoPage(srn, index1of300), nino)
      .unsafeSet(MemberStatus(srn, index1of300), MemberState.New)
      .unsafeSet(MemberDetailsCompletedPage(srn, index1of300), SectionCompleted)
      .unsafeSet(MemberDetailsManualProgress(srn, index1of300), SectionJourneyStatus.Completed)

      // employer contributions
      .unsafeSet(EmployerContributionsPage(srn), true)
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(EmployerNamePage(srn, index1of300, index1of50), employerName)
      .unsafeSet(EmployerTypeOfBusinessPage(srn, index1of300, index1of50), IdentityType.UKCompany)
      .unsafeSet(TotalEmployerContributionPage(srn, index1of300, index1of50), money)
      .unsafeSet(EmployerCompanyCrnPage(srn, index1of300, index1of50), ConditionalYesNo.yes[String, Crn](crn))
      .unsafeSet(EmployerContributionsCompleted(srn, index1of300, index1of50), SectionCompleted)
      .unsafeSet(EmployerContributionsProgress(srn, index1of300, index1of50), SectionJourneyStatus.Completed)

      // unallocated contributions
      .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
      .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

      // member contributions
      .unsafeSet(MemberContributionsPage(srn), true)
      .unsafeSet(TotalMemberContributionPage(srn, index1of300), money)

      // transfers in
      .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(TransfersInSectionCompleted(srn, index1of300, index1of5), SectionCompleted)
      .unsafeSet(ReceiveTransferProgress(srn, index1of300, index1of5), SectionJourneyStatus.Completed)
      .unsafeSet(TransferringSchemeNamePage(srn, index1of300, index1of5), transferringSchemeName)
      .unsafeSet(TransferringSchemeTypePage(srn, index1of300, index1of5), PensionSchemeType.Other("other"))
      .unsafeSet(TotalValueTransferPage(srn, index1of300, index1of5), money)
      .unsafeSet(WhenWasTransferReceivedPage(srn, index1of300, index1of5), localDate)
      .unsafeSet(DidTransferIncludeAssetPage(srn, index1of300, index1of5), true)

      // transfers out
      .unsafeSet(SchemeTransferOutPage(srn), true)
      .unsafeSet(SchemeTransferOutPage(srn), true)
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(TransfersOutSectionCompleted(srn, index1of300, index1of5), SectionCompleted)
      .unsafeSet(MemberTransferOutProgress(srn, index1of300, index1of5), SectionJourneyStatus.Completed)
      .unsafeSet(ReceivingSchemeNamePage(srn, index1of300, index1of5), receivingSchemeName)
      .unsafeSet(ReceivingSchemeTypePage(srn, index1of300, index1of5), PensionSchemeType.Other("other"))
      .unsafeSet(WhenWasTransferMadePage(srn, index1of300, index1of5), localDate)

      // PCLS
      .unsafeSet(PensionCommencementLumpSumPage(srn), true)
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index1of300), pensionCommencementLumpSumGen.sample.value)

      // member pension payments
      .unsafeSet(PensionPaymentsReceivedPage(srn), true)
      .unsafeSet(TotalAmountPensionPaymentsPage(srn, index1of300), money)
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(MemberDetailsCompletedPage(srn, index1of300), SectionCompleted)

      // surrendered benefits
      .unsafeSet(SurrenderedBenefitsPage(srn), true)
      .unsafeSet(SurrenderedBenefitsAmountPage(srn, index1of300), surrenderedBenefitsAmount)
      .unsafeSet(WhenDidMemberSurrenderBenefitsPage(srn, index1of300), localDate)
      .unsafeSet(WhyDidMemberSurrenderBenefitsPage(srn, index1of300), reasonSurrenderedBenefits)
      .unsafeSet(SurrenderedBenefitsCompletedPage(srn, index1of300), SectionCompleted)
      .unsafeSet(MemberSurrenderedBenefitsProgress(srn, index1of300), SectionJourneyStatus.Completed)

      // loans
      .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
      .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient), IdentityType.UKCompany)
      .unsafeSet(CompanyRecipientNamePage(srn, index), recipientName)
      .unsafeSet(
        CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient),
        ConditionalYesNo.yes[String, Crn](crn)
      )
      .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), SponsoringOrConnectedParty.ConnectedParty)
      .unsafeSet(DatePeriodLoanPage(srn, index), (localDate, money, loanPeriod))
      .unsafeSet(AmountOfTheLoanPage(srn, index), amountOfTheLoan)
      .unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)
      .unsafeSet(InterestOnLoanPage(srn, index), interestOnLoan)
      .unsafeSet(SecurityGivenForLoanPage(srn, index), ConditionalYesNo.yes[Unit, Security](security))
      .unsafeSet(ArrearsPrevYears(srn, index), true)
      .unsafeSet(OutstandingArrearsOnLoanPage(srn, index), ConditionalYesNo.yes[Unit, Money](money))
      .unsafeSet(LoanCompleted(srn, index), SectionCompleted)
      .unsafeSet(LoansProgress(srn, index), SectionJourneyStatus.Completed)

      // money borrowed
      .unsafeSet(MoneyBorrowedPage(srn), true)
      .unsafeSet(LenderNamePage(srn, index), lenderName)
      .unsafeSet(IsLenderConnectedPartyPage(srn, index), true)
      .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))
      .unsafeSet(WhenBorrowedPage(srn, index), localDate)
      .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), money)
      .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index), schemeName)
      .unsafeSet(MoneyBorrowedProgress(srn, index), SectionJourneyStatus.Completed)

      // shares
      .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Contribution)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)
      .unsafeSet(SharesCompanyCrnPage(srn, index), ConditionalYesNo.yes[String, Crn](crn))
      .unsafeSet(ClassOfSharesPage(srn, index), companyName)
      .unsafeSet(HowManySharesPage(srn, index), totalShares)
      .unsafeSet(PartnershipShareSellerNamePage(srn, index), companyName)
      .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.SharesSeller), IdentityType.UKCompany)
      .unsafeSet(SharesFromConnectedPartyPage(srn, index), false)
      .unsafeSet(CostOfSharesPage(srn, index), money)
      .unsafeSet(SharesIndependentValuationPage(srn, index), true)
      .unsafeSet(TotalAssetValuePage(srn, index), money)
      .unsafeSet(SharesTotalIncomePage(srn, index), money)
      .unsafeSet(SharesProgress(srn, index), SectionJourneyStatus.Completed)

      // shares disposals
      .unsafeSet(SharesDisposalPage(srn), true)
      .unsafeSet(HowWereSharesDisposedPage(srn, index, index1of50), HowSharesDisposed.Sold)
      .unsafeSet(WhenWereSharesSoldPage(srn, index, index1of50), dateSharesSold.get)
      .unsafeSet(HowManySharesSoldPage(srn, index, index1of50), numberSharesSold.get)
      .unsafeSet(TotalConsiderationSharesSoldPage(srn, index, index1of50), considerationSharesSold.get)
      .unsafeSet(WhoWereTheSharesSoldToPage(srn, index, index1of50), IdentityType.Individual)
      .unsafeSet(SharesIndividualBuyerNamePage(srn, index, index1of50), buyerName)
      .unsafeSet(
        pages.nonsipp.sharesdisposal.IndividualBuyerNinoNumberPage(srn, index, index1of50),
        ConditionalYesNo.yes[String, Nino](nino)
      )
      .unsafeSet(
        pages.nonsipp.sharesdisposal.IsBuyerConnectedPartyPage(srn, index, index1of50),
        isBuyerConnectedParty.get
      )
      .unsafeSet(
        pages.nonsipp.sharesdisposal.IndependentValuationPage(srn, index, index1of50),
        isIndependentValuation.get
      )
      .unsafeSet(HowManyDisposalSharesPage(srn, index, index1of50), sharesStillHeld)
      .unsafeSet(SharesDisposalCYAPointOfEntry(srn, index, index1of50), HowWereSharesDisposedPointOfEntry)
      .unsafeSet(SharesDisposalProgress(srn, index, index1of50), SectionJourneyStatus.Completed)

      // total value quoted shares
      .unsafeSet(TotalValueQuotedSharesPage(srn), money)

      // land or property
      .unsafeSet(LandOrPropertyHeldPage(srn), true)
      .unsafeSet(LandPropertyInUKPage(srn, index), true)
      .unsafeSet(LandOrPropertyPostcodeLookupPage(srn, index), postcodeLookup)
      .unsafeSet(AddressLookupResultsPage(srn, index), List(address, address, address))
      .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
      .unsafeSet(
        LandRegistryTitleNumberPage(srn, index),
        ConditionalYesNo.yes[String, String]("landRegistryTitleNumber")
      )
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Transfer)
      .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)
      .unsafeSet(IsLandOrPropertyResidentialPage(srn, index), false)
      .unsafeSet(IsLandPropertyLeasedPage(srn, index), false)
      .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)
      .unsafeSet(LandOrPropertyProgress(srn, index), SectionJourneyStatus.Completed)
      .unsafeSet(LandOrPropertyCompleted(srn, index), SectionCompleted)

      // land or property disposals
      .unsafeSet(LandOrPropertyDisposalPage(srn), true)
      .unsafeSet(HowWasPropertyDisposedOfPage(srn, index, index1of50), HowDisposed.Sold)
      .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index, index1of50), IdentityType.Individual)
      .unsafeSet(LandOrPropertyIndividualBuyerNamePage(srn, index, index1of50), recipientName)
      .unsafeSet(
        pages.nonsipp.sharesdisposal.IndividualBuyerNinoNumberPage(srn, index, index1of50),
        conditionalYesNoNino
      )
      .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
      .unsafeSet(LandOrPropertyStillHeldPage(srn, index, index1of50), true)
      .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, index, index1of50), money)
      .unsafeSet(DisposalIndependentValuationPage(srn, index, index1of50), true)
      .unsafeSet(
        LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, index1of50),
        isBuyerConnectedParty.get
      )
      .unsafeSet(LandOrPropertyDisposalProgress(srn, index, index1of50), SectionJourneyStatus.Completed)
      .unsafeSet(WhenWasPropertySoldPage(srn, index, index1of50), dateSold.get)

      // bonds
      .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
      .unsafeSet(NameOfBondsPage(srn, index), otherName)
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)
      .unsafeSet(CostOfBondsPage(srn, index), money)
      .unsafeSet(BondsFromConnectedPartyPage(srn, index), true)
      .unsafeSet(AreBondsUnregulatedPage(srn, index), true)
      .unsafeSet(IncomeFromBondsPage(srn, index), money)
      .unsafeSet(BondsProgress(srn, index), SectionJourneyStatus.Completed)

      // bonds disposals
      .unsafeSet(BondsDisposalPage(srn), true)
      .unsafeSet(NameOfBondsPage(srn, index), "name")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(CostOfBondsPage(srn, index), money)
      .unsafeSet(HowWereBondsDisposedOfPage(srn, index, index1of50), HowDisposed.Sold)
      .unsafeSet(WhenWereBondsSoldPage(srn, index, index1of50), localDate)
      .unsafeSet(TotalConsiderationSaleBondsPage(srn, index, index1of50), money)
      .unsafeSet(BuyerNamePage(srn, index, index1of50), nameOfBuyer.get)
      .unsafeSet(
        pages.nonsipp.bondsdisposal.IsBuyerConnectedPartyPage(srn, index, index1of50),
        isBuyerConnectedParty.get
      )
      .unsafeSet(BondsStillHeldPage(srn, index, index1of50), bondsStillHeld)
      .unsafeSet(BondsDisposalCYAPointOfEntry(srn, index, index1of50), HowWereBondsDisposedPointOfEntry)
      .unsafeSet(BondsDisposalProgress(srn, index, index1of50), SectionJourneyStatus.Completed)

      // other assets
      .unsafeSet(OtherAssetsHeldPage(srn), true)
      .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
      .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), true)
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
      .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller), IdentityType.Individual)
      .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), individualName)
      .unsafeSet(OtherAssetIndividualSellerNINumberPage(srn, index), ConditionalYesNo.yes[String, Nino](nino))
      .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
      .unsafeSet(CostOfOtherAssetPage(srn, index), money)
      .unsafeSet(pages.nonsipp.otherassetsheld.IndependentValuationPage(srn, index), true)
      .unsafeSet(IncomeFromAssetPage(srn, index), money)
      .unsafeSet(OtherAssetsProgress(srn, index), SectionJourneyStatus.Completed)

      // other assets disposals
      .unsafeSet(OtherAssetsDisposalPage(srn), true)
      .unsafeSet(pages.nonsipp.otherassetsdisposal.HowWasAssetDisposedOfPage(srn, index, index1of50), HowDisposed.Sold)
      .unsafeSet(pages.nonsipp.otherassetsheld.WhatIsOtherAssetPage(srn, index), otherName)
      .unsafeSet(pages.nonsipp.otherassetsdisposal.AnyPartAssetStillHeldPage(srn, index, index1of50), true)
      .unsafeSet(
        pages.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetPage(srn, index, index1of50),
        considerationAssetSold.get
      )
      .unsafeSet(pages.nonsipp.otherassetsdisposal.AssetSaleIndependentValuationPage(srn, index, index1of50), true)
      .unsafeSet(
        pages.nonsipp.otherassetsdisposal.TypeOfAssetBuyerPage(srn, index, index1of50),
        IdentityType.UKPartnership
      )
      .unsafeSet(pages.nonsipp.otherassetsdisposal.PartnershipBuyerNamePage(srn, index, index1of50), recipientName)
      .unsafeSet(pages.nonsipp.otherassetsdisposal.WhenWasAssetSoldPage(srn, index, index1of50), localDate)
      .unsafeSet(
        pages.nonsipp.otherassetsdisposal.IsBuyerConnectedPartyPage(srn, index, index1of50),
        isBuyerConnectedParty.get
      )
      .unsafeSet(
        pages.nonsipp.otherassetsdisposal.OtherAssetsDisposalProgress(srn, index, index1of50),
        SectionJourneyStatus.Completed
      )

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val schemeDatePeriod: DateRange = dateRangeGen.sample.value

  lazy val onPageLoad = routes.SummaryController.onPageLoadPreSubmission(srn)

  override protected def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    when(mockSchemeDateService.schemeDate(any())(using any())).thenReturn(Some(schemeDatePeriod))
    when(mockSchemeDateService.taxYearOrAccountingPeriods(any())(using any())).thenReturn(Some(Left(schemeDatePeriod)))
    super.beforeEach()
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "PreSubmissionSummaryController" - {

    "summary page should display correct content" in {
      running(_ => applicationBuilder(userAnswers = Some(populatedUserAnswers))) { implicit app =>
        val request = FakeRequest(GET, onPageLoad.url)
        val result = route(app, request).value

        withClue(if (status(result) == SEE_OTHER) s"Expected 200 but got 303 to ${redirectLocation(result)}") {
          status(result) mustEqual OK
        }

        val content = contentAsString(result)

        // scheme details
        content should include("<h2 class=\"govuk-heading-xl\">Basic details</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Financial details</h2>")

        // members
        content should include("<h2 class=\"govuk-heading-xl\">Member details</h2>")

        // member payments
        content should include("<h2 class=\"govuk-heading-xl\">Employer contributions</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Unallocated contributions</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Member contributions</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Transfers in</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Transfers out</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">PCLS</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Pension payments</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Surrendered benefits</h2>")

        // loans
        content should include("<h2 class=\"govuk-heading-xl\">Loans</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Money borrowed</h2>")

        // shares
        content should include("<h2 class=\"govuk-heading-xl\">Shares</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Disposal of shares</h2>")

        // land or property
        content should include("<h2 class=\"govuk-heading-xl\">Land or property</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Disposal of land or property</h2>")

        // bonds
        content should include("<h2 class=\"govuk-heading-xl\">Bonds</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Disposal of bonds</h2>")

        // other assets
        content should include("<h2 class=\"govuk-heading-xl\">Total value of quoted shares</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Other assets</h2>")
        content should include("<h2 class=\"govuk-heading-xl\">Disposal of other assets</h2>")
      }
    }
  }
}
