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

import config.Refined._
import controllers.TestValues
import eu.timepit.refined.refineMV
import generators.ModelGenerators.pensionSchemeIdGen
import models.ConditionalYesNo._
import models.HowSharesDisposed.Sold
import models.IdentitySubject._
import models.IdentityType.{Individual, UKCompany, UKPartnership}
import models.ManualOrUpload.Manual
import models.PensionSchemeType.RegisteredPS
import models.SchemeHoldShare.Acquisition
import models.SponsoringOrConnectedParty.Sponsoring
import models.TypeOfShares.SponsoringEmployer
import models._
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.bonds._
import pages.nonsipp.bondsdisposal.{BondsDisposalPage, BondsStillHeldPage, HowWereBondsDisposedOfPage}
import pages.nonsipp.common._
import pages.nonsipp.employercontributions._
import pages.nonsipp.landorproperty._
import pages.nonsipp.landorpropertydisposal.{
  HowWasPropertyDisposedOfPage,
  LandOrPropertyDisposalPage,
  LandOrPropertyStillHeldPage
}
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.membercontributions.{MemberContributionsListPage, MemberContributionsPage}
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import pages.nonsipp.memberpensionpayments.{MemberPensionPaymentsListPage, PensionPaymentsReceivedPage}
import pages.nonsipp.memberreceivedpcls.{
  PclsMemberListPage,
  PensionCommencementLumpSumAmountPage,
  PensionCommencementLumpSumPage
}
import pages.nonsipp.membersurrenderedbenefits._
import pages.nonsipp.membertransferout._
import pages.nonsipp.moneyborrowed._
import pages.nonsipp.otherassetsdisposal.{AnyPartAssetStillHeldPage, HowWasAssetDisposedOfPage, OtherAssetsDisposalPage}
import pages.nonsipp.otherassetsheld._
import pages.nonsipp.receivetransfer._
import pages.nonsipp.schemedesignatory._
import pages.nonsipp.shares._
import pages.nonsipp.sharesdisposal._
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import pages.nonsipp.{CheckReturnDatesPage, WhichTaxYearPage}
import uk.gov.hmrc.domain.Nino
import utils.UserAnswersUtils.UserAnswersOps
import utils.nonsipp.TaskListStatusUtils.userAnswersUnchangedAllSections
import viewmodels.models.TaskListStatus._
import viewmodels.models.{MemberState, SectionCompleted, SectionStatus}

class MemberCountUtilsSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  private val index: Max300 = refineMV(1)
  private val pensionSchemeId: PensionSchemeId = pensionSchemeIdGen.sample.value
  private val userAnswersOverThresholdNumbers: UserAnswers = defaultUserAnswers
    .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)

  "hasMemberNumbersChangedToOver99" - {
    "should return false" - {
      "when default data" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(defaultUserAnswers, srn, pensionSchemeId)
        result mustBe false
      }

      "when only SchemeMemberNumbers exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          defaultUserAnswers
            .unsafeSet(HowManyMembersPage(srn, psaId), SchemeMemberNumbers(1, 2, 3)),
          srn,
          pensionSchemeId
        )
        result mustBe false
      }

      "when only SchemeMemberNumbers > 99 exist" in {
        val result =
          MemberCountUtils.hasMemberNumbersChangedToOver99(userAnswersOverThresholdNumbers, srn, pensionSchemeId)
        result mustBe false
      }
    }

    "should return true" - {
      "when SchemeMemberNumbers > 99 and member details exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(MemberDetailsPage(srn, index), memberDetails),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }
      "when SchemeMemberNumbers > 99 and loan details exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(LoansMadeOrOutstandingPage(srn), false),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and borrowings exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(MoneyBorrowedPage(srn), false),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and financial information exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(HowMuchCashPage(srn, NormalMode), moneyInPeriod)
            .unsafeSet(ValueOfAssetsPage(srn, NormalMode), moneyInPeriod)
            .unsafeSet(FeesCommissionsWagesSalariesPage(srn, NormalMode), money),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and shares exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(DidSchemeHoldAnySharesPage(srn), false),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and land or properties exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(LandOrPropertyHeldPage(srn), false),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and bonds exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), false),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }

      "when SchemeMemberNumbers > 99 and other assets exist" in {
        val result = MemberCountUtils.hasMemberNumbersChangedToOver99(
          userAnswersOverThresholdNumbers
            .unsafeSet(OtherAssetsHeldPage(srn), false),
          srn,
          pensionSchemeId
        )
        result mustBe true
      }

    }
  }
}
