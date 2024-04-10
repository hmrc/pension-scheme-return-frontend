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

package transformations

import pages.nonsipp.employercontributions._
import org.scalatest.matchers.must.Matchers
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
import config.Refined.{Max300, Max5, Max50}
import controllers.TestValues
import models.requests.psr._
import utils.UserAnswersUtils.UserAnswersOps
import org.scalatest.OptionValues
import pages.nonsipp.membersurrenderedbenefits._
import models._
import pages.nonsipp.membertransferout._
import pages.nonsipp.memberdetails._
import org.scalatest.freespec.AnyFreeSpec
import pages.nonsipp.membercontributions._
import pages.nonsipp.memberreceivedpcls._
import cats.implicits.catsSyntaxEitherId
import pages.nonsipp.receivetransfer._
import pages.nonsipp.memberpensionpayments._
import eu.timepit.refined.refineMV
import pages.nonsipp.memberpayments._
import com.softwaremill.diffx.generic.AutoDerivation
import viewmodels.models._

import scala.util.Try

import java.time.LocalDate

class MemberPaymentsTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with DiffShouldMatcher
    with AutoDerivation {

  private val transfersInTransformer = new TransfersInTransformer()
  private val transfersOutTransformer = new TransfersOutTransformer()
  private val pensionSurrenderTransformer = new PensionSurrenderTransformer()
  private val memberPaymentsTransformer =
    new MemberPaymentsTransformer(transfersInTransformer, transfersOutTransformer, pensionSurrenderTransformer)

  private val memberPayments = MemberPayments(
    memberDetails = List(
      MemberDetails(
        personalDetails = MemberPersonalDetails(
          firstName = memberDetails.firstName,
          lastName = memberDetails.lastName,
          nino = Some(nino.value),
          reasonNoNINO = None,
          dateOfBirth = memberDetails.dob
        ),
        employerContributions = List(
          EmployerContributions(
            employerName = employerName,
            employerType = EmployerType.UKCompany(Right(crn.value)),
            totalTransferValue = money.value
          )
        ),
        transfersIn = List(
          TransfersIn(
            schemeName = schemeName,
            dateOfTransfer = localDate,
            transferSchemeType = PensionSchemeType.RegisteredPS("123"),
            transferValue = money.value,
            transferIncludedAsset = true
          )
        ),
        totalContributions = Some(money.value),
        memberLumpSumReceived = Some(MemberLumpSumReceived(money.value, money.value)),
        transfersOut = List(
          TransfersOut(
            schemeName = schemeName,
            dateOfTransfer = localDate,
            transferSchemeType = PensionSchemeType.RegisteredPS("456")
          )
        ),
        benefitsSurrendered = Some(
          PensionSurrender(
            totalSurrendered = 12.34,
            dateOfSurrender = LocalDate.of(2022, 12, 12),
            surrenderReason = "some reason"
          )
        ),
        pensionAmountReceived = Some(12.34)
      )
    ),
    employerContributionsDetails = SectionDetails(made = true, completed = true),
    transfersInCompleted = false,
    transfersOutCompleted = false,
    unallocatedContribsMade = true,
    unallocatedContribAmount = Some(money.value),
    memberContributionMade = true,
    lumpSumReceived = true,
    benefitsSurrenderedDetails = SectionDetails(made = true, completed = true),
    pensionReceived = true
  )

  private val index = refineMV[Max300.Refined](1)
  private val employerContribsIndex = refineMV[Max50.Refined](1)
  private val transfersInIndex = refineMV[Max5.Refined](1)
  private val transfersOutIndex = refineMV[Max5.Refined](1)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
    .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
    .unsafeSet(MemberStatus(srn, index), MemberState.Active)
    // employer contributions
    .unsafeSet(EmployerContributionsPage(srn), true)
    .unsafeSet(EmployerNamePage(srn, index, employerContribsIndex), employerName)
    .unsafeSet(EmployerTypeOfBusinessPage(srn, index, employerContribsIndex), IdentityType.UKCompany)
    .unsafeSet(EmployerCompanyCrnPage(srn, index, employerContribsIndex), ConditionalYesNo(crn.asRight[String]))
    .unsafeSet(TotalEmployerContributionPage(srn, index, employerContribsIndex), money)
    .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
    .unsafeSet(EmployerContributionsCompleted(srn, index, employerContribsIndex), SectionCompleted)
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
    .unsafeSet(EmployerContributionsMemberListPage(srn), true)
    .unsafeSet(EmployerContributionsProgress(srn, index, employerContribsIndex), SectionJourneyStatus.Completed)
    // transfers in
    .unsafeSet(TransfersInSectionCompleted(srn, index, transfersInIndex), SectionCompleted)
    .unsafeSet(TransfersInJourneyStatus(srn), SectionStatus.InProgress)
    .unsafeSet(TransferringSchemeNamePage(srn, index, transfersInIndex), schemeName)
    .unsafeSet(WhenWasTransferReceivedPage(srn, index, transfersInIndex), localDate)
    .unsafeSet(TotalValueTransferPage(srn, index, transfersInIndex), money)
    .unsafeSet(DidTransferIncludeAssetPage(srn, index, transfersInIndex), true)
    .unsafeSet(TransferringSchemeTypePage(srn, index, transfersInIndex), PensionSchemeType.RegisteredPS("123"))
    .unsafeSet(MemberContributionsPage(srn), true)
    .unsafeSet(MemberContributionsListPage(srn), true)
    .unsafeSet(TotalMemberContributionPage(srn, index), money)
    .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
    .unsafeSet(TransferReceivedMemberListPage(srn), false)
    .unsafeSet(ReportAnotherTransferInPage(srn, index, transfersInIndex), false)
    // pcls
    .unsafeSet(PensionCommencementLumpSumPage(srn), true)
    .unsafeSet(PclsMemberListPage(srn), true)
    .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index), PensionCommencementLumpSum(money, money))
    // transfers out
    .unsafeSet(SchemeTransferOutPage(srn), true)
    .unsafeSet(TransferOutMemberListPage(srn), false)
    .unsafeSet(TransfersOutJourneyStatus(srn), SectionStatus.InProgress)
    .unsafeSet(TransfersOutCompletedPage(srn, index, transfersOutIndex), SectionCompleted)
    .unsafeSet(ReceivingSchemeNamePage(srn, index, transfersOutIndex), schemeName)
    .unsafeSet(WhenWasTransferMadePage(srn, index, transfersOutIndex), localDate)
    .unsafeSet(ReceivingSchemeTypePage(srn, index, transfersOutIndex), PensionSchemeType.RegisteredPS("456"))
    // pension surrender
    .unsafeSet(SurrenderedBenefitsCompletedPage(srn, index), SectionCompleted)
    .unsafeSet(SurrenderedBenefitsJourneyStatus(srn), SectionStatus.Completed)
    .unsafeSet(SurrenderedBenefitsMemberListPage(srn), true)
    .unsafeSet(SurrenderedBenefitsPage(srn), true)
    .unsafeSet(SurrenderedBenefitsAmountPage(srn, index), Money(12.34))
    .unsafeSet(WhenDidMemberSurrenderBenefitsPage(srn, index), LocalDate.of(2022, 12, 12))
    .unsafeSet(WhyDidMemberSurrenderBenefitsPage(srn, index), "some reason")
    // pension payments
    .unsafeSet(PensionPaymentsReceivedPage(srn), true)
    .unsafeSet(TotalAmountPensionPaymentsPage(srn, index), Money(12.34))
    .unsafeSet(PensionPaymentsJourneyStatus(srn), SectionStatus.Completed)
    .unsafeSet(MemberPensionPaymentsListPage(srn), true)

  "MemberPaymentsTransformer - To Etmp" - {
    "should return empty List when userAnswer is empty" in {
      val result = memberPaymentsTransformer.transformToEtmp(srn, defaultUserAnswers)
      result shouldMatchTo None
    }

    "should return member payments" in {
      val result = memberPaymentsTransformer.transformToEtmp(srn, userAnswers)
      result shouldMatchTo Some(memberPayments)
    }
  }

  "MemberPaymentsTransformer - From Etmp" - {
    "should return correct user answers" in {
      val result = memberPaymentsTransformer.transformFromEtmp(defaultUserAnswers, srn, memberPayments)
      result shouldMatchTo Try(userAnswers)
    }
  }
}
