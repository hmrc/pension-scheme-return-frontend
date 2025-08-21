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
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld._
import models.ManualOrUpload.Manual
import controllers.TestValues
import pages.nonsipp.landorproperty._
import pages.nonsipp.receivetransfer._
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalPage
import pages.nonsipp.memberpensionpayments.PensionPaymentsReceivedPage
import pages.nonsipp.sharesdisposal._
import utils.UserAnswersUtils.UserAnswersOps
import models._
import pages.nonsipp.otherassetsdisposal.OtherAssetsDisposalPage
import pages.nonsipp.schemedesignatory._
import pages.nonsipp.bonds._
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import pages.nonsipp.memberdetails._
import org.scalatest.freespec.AnyFreeSpec
import pages.nonsipp.membercontributions.MemberContributionsPage
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import pages.nonsipp._
import org.scalatest.OptionValues
import utils.nonsipp.TaskListUtils._
import pages.nonsipp.membersurrenderedbenefits._
import generators.ModelGenerators.{psaIdGen, pspIdGen}
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject._
import pages.nonsipp.membertransferout._
import pages.nonsipp.moneyborrowed._
import pages.nonsipp.bondsdisposal.BondsDisposalPage
import viewmodels.DisplayMessage
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models._

class TaskListUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  private val currentUA: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), dateRange)
    .unsafeSet(FbStatus(srn), Submitted)
    .unsafeSet(FbVersionPage(srn), "002")
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
    .unsafeSet(MemberDetailsCompletedPage(srn, index1of300), SectionCompleted)
    .unsafeSet(MemberDetailsManualProgress(srn, index1of300), SectionJourneyStatus.Completed)
    // Section 3 - Member Payments
    // (S3) Employer Contributions
    .unsafeSet(EmployerContributionsPage(srn), false)
    // (S3) Unallocated Employer Contributions
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
    // (S3) Member Contributions
    .unsafeSet(MemberContributionsPage(srn), false)
    // (S3) Transfers In
    .unsafeSet(DidSchemeReceiveTransferPage(srn), false)
    // (S3) Transfers Out
    .unsafeSet(SchemeTransferOutPage(srn), false)
    // (S3) PCLS
    .unsafeSet(PensionCommencementLumpSumPage(srn), true)
    .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index1of300), pcls)
    // (S3) Pension Payments
    .unsafeSet(PensionPaymentsReceivedPage(srn), false)
    // (S3) Surrendered Benefits
    .unsafeSet(SurrenderedBenefitsPage(srn), false)
    // Section 4 - Loans Made & Money Borrowed
    // (S4) Loans Made
    .unsafeSet(LoansMadeOrOutstandingPage(srn), false)
    // (S4) Money Borrowed
    .unsafeSet(MoneyBorrowedPage(srn), false)
    // Section 5 - Shares
    // (S5) Shares
    .unsafeSet(DidSchemeHoldAnySharesPage(srn), false)
    .unsafeSet(SharesCompleted(srn, index1of5000), SectionCompleted)
    // (S5) Shares Disposals
    .unsafeSet(SharesDisposalPage(srn), false)
    // (S5) Quoted Shares
    .unsafeSet(TotalValueQuotedSharesPage(srn), money)
    // Section 6 - Land or Property
    // (S6) Land or Property
    .unsafeSet(LandOrPropertyHeldPage(srn), false)
    .unsafeSet(LandOrPropertyCompleted(srn, index1of5000), SectionCompleted)
    // (S6) Land or Property Disposals
    .unsafeSet(LandOrPropertyDisposalPage(srn), false)
    // Section 7 - Bonds
    // (S7) Bonds
    .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false)
    .unsafeSet(BondsCompleted(srn, index1of5000), SectionCompleted)
    // (S7) Bonds Disposals
    .unsafeSet(BondsDisposalPage(srn), false)
    // Section 8 - Other Assets
    // (S8) Other Assets
    .unsafeSet(OtherAssetsHeldPage(srn), false)
    .unsafeSet(OtherAssetsCompleted(srn, index1of5000), SectionCompleted)
    // (S8) Other Assets Disposals
    .unsafeSet(OtherAssetsDisposalPage(srn), false)

  private val previousUA: UserAnswers = currentUA
    .unsafeSet(FbStatus(srn), Submitted)
    .unsafeSet(FbVersionPage(srn), "001")

  "getSectionList" - {
    "should return view declaration submitted link if navigated from view and change and no changes were made" in {
      val sectionList = getSectionList(
        srn,
        schemeName,
        currentUA,
        psaIdGen.sample.value,
        Some(previousUA),
        Some(currentUA),
        dateRange.from,
        isPrePop = false
      )
      sectionList.size mustBe 9
      sectionList.reverse.head.items.fold(
        message =>
          message.head.asInstanceOf[LinkMessage].url mustBe controllers.nonsipp.routes.ViewOnlyReturnSubmittedController
            .onPageLoad(
              srn,
              dateRange.from.toString,
              1
            )
            .url,
        _ => fail("Expected link is missing")
      )
    }

    "should return view declaration submitted link if navigated from view and change and there is no previous version and pure version is submitted" in {
      val sectionList = getSectionList(
        srn,
        schemeName,
        previousUA,
        psaIdGen.sample.value,
        None,
        Some(previousUA),
        dateRange.from,
        isPrePop = false
      )
      sectionList.size mustBe 9
      sectionList.reverse.head.items.fold(
        message =>
          message.head.asInstanceOf[LinkMessage].url mustBe controllers.nonsipp.routes.ViewOnlyReturnSubmittedController
            .onPageLoad(
              srn,
              dateRange.from.toString,
              1
            )
            .url,
        _ => fail("Expected link is missing")
      )
    }

    "should return view declaration submitted link if navigated from view and change and there is no previous version " +
      "and pure version is submitted and there are changes to the user answers" in {
        val sectionList = getSectionList(
          srn,
          schemeName,
          previousUA.unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), Money(123)),
          psaIdGen.sample.value,
          None,
          Some(previousUA),
          dateRange.from,
          isPrePop = false
        )
        sectionList.size mustBe 9
        sectionList.reverse.head.items.fold(
          message =>
            message.head
              .asInstanceOf[LinkMessage]
              .url mustBe controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn).url,
          _ => fail("Expected link is missing")
        )
      }

    "should return submit declaration link for psa if there were changes in current user answers" in {
      val sectionList = getSectionList(
        srn,
        schemeName,
        currentUA
          .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), otherMoney),
        psaIdGen.sample.value,
        Some(previousUA),
        Some(currentUA),
        dateRange.from,
        isPrePop = false
      )
      sectionList.size mustBe 9
      sectionList.reverse.head.items.fold(
        message =>
          message.head
            .asInstanceOf[LinkMessage]
            .url mustBe controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn).url,
        _ => fail("Expected link is missing")
      )
    }
    "should return submit declaration link for psp if there were changes is current user answers" in {
      val sectionList = getSectionList(
        srn,
        schemeName,
        currentUA
          .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), otherMoney),
        pspIdGen.sample.value,
        Some(previousUA),
        Some(currentUA),
        dateRange.from,
        isPrePop = false
      )
      sectionList.size mustBe 9
      sectionList.reverse.head.items.fold(
        message =>
          message.head
            .asInstanceOf[LinkMessage]
            .url mustBe controllers.nonsipp.declaration.routes.PspDeclarationController.onPageLoad(srn).url,
        _ => fail("Expected link is missing")
      )
    }
    "should return incomplete declaration text if there are sections outstanding" in {
      val sectionList = getSectionList(
        srn,
        schemeName,
        currentUA
          .remove(FeesCommissionsWagesSalariesPage(srn, NormalMode))
          .get,
        psaIdGen.sample.value,
        Some(previousUA),
        Some(currentUA),
        dateRange.from,
        isPrePop = false
      )
      sectionList.size mustBe 9
      sectionList.reverse.head.items.fold(
        message => message.head.asInstanceOf[DisplayMessage] mustBe Message("nonsipp.tasklist.declaration.incomplete"),
        _ => fail("Expected link is missing")
      )
    }
    "should return submit declaration link if there is no history" in {
      val sectionList =
        getSectionList(
          srn,
          schemeName,
          currentUA,
          psaIdGen.sample.value,
          None,
          Some(currentUA),
          dateRange.from,
          isPrePop = false
        )
      sectionList.size mustBe 9
      sectionList.reverse.head.items.fold(
        message =>
          message.head
            .asInstanceOf[LinkMessage]
            .url mustBe controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn).url,
        _ => fail("Expected link is missing")
      )
    }
  }
}
