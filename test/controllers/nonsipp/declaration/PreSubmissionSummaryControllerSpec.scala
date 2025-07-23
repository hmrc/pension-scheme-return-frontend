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
import services._
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import models.ConditionalYesNo._
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld._
import controllers.{ControllerBaseSpec, ControllerBehaviours, TestUserAnswers}
import play.api.inject.bind
import pages.nonsipp.landorproperty._
import models._
import pages.nonsipp.common._
import pages.nonsipp.moneyborrowed._
import viewmodels.models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds._
import pages.nonsipp.FbVersionPage
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.loansmadeoroutstanding._
import org.scalatest.matchers.should.Matchers.should

class PreSubmissionSummaryControllerSpec
    extends ControllerBaseSpec
    with ControllerBehaviours
    with BeforeAndAfterEach
    with TestUserAnswers
    with CommonTestValues {

  val index = index1of5000
  private val populatedUserAnswers =
    defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), version)
      .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersUnderThreshold)

      // land or property
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

      // loans
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

      // bonds
      .unsafeSet(NameOfBondsPage(srn, index), otherName)
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)
      .unsafeSet(CostOfBondsPage(srn, index), money)
      .unsafeSet(BondsFromConnectedPartyPage(srn, index), true)
      .unsafeSet(AreBondsUnregulatedPage(srn, index), true)
      .unsafeSet(IncomeFromBondsPage(srn, index), money)
      .unsafeSet(BondsProgress(srn, index), SectionJourneyStatus.Completed)

      // shares
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

      // other assets
      .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
      .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), true)
      .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
      .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller), IdentityType.Individual)
      .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), individualName)
      .unsafeSet(OtherAssetIndividualSellerNINumberPage(srn, index), ConditionalYesNo.yes[String, Nino](nino))
      .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
      .unsafeSet(CostOfOtherAssetPage(srn, index), money)
      .unsafeSet(IndependentValuationPage(srn, index), true)
      .unsafeSet(IncomeFromAssetPage(srn, index), money)
      .unsafeSet(OtherAssetsProgress(srn, index), SectionJourneyStatus.Completed)

      // money borrowed
      .unsafeSet(LenderNamePage(srn, index), lenderName)
      .unsafeSet(IsLenderConnectedPartyPage(srn, index), true)
      .unsafeSet(BorrowedAmountAndRatePage(srn, index), (money, percentage))
      .unsafeSet(WhenBorrowedPage(srn, index), localDate)
      .unsafeSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), money)
      .unsafeSet(WhySchemeBorrowedMoneyPage(srn, index), schemeName)

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val schemeDatePeriod: DateRange = dateRangeGen.sample.value

  lazy val onPageLoad = routes.PreSubmissionSummaryController.onPageLoad(srn)

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

        content should include("<h1 class=\"govuk-heading-xl\">Land or property</h1>")
        content should include("<h1 class=\"govuk-heading-xl\">Bonds</h1>")
        content should include("<h1 class=\"govuk-heading-xl\">Shares</h1>")
        content should include("<h1 class=\"govuk-heading-xl\">Loans</h1>")
        content should include("<h1 class=\"govuk-heading-xl\">Other assets</h1>")
        content should include("<h1 class=\"govuk-heading-xl\">Money borrowed</h1>")
      }
    }
  }
}
