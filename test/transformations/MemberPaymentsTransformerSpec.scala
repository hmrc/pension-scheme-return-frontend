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

import org.scalatest.matchers.must.Matchers
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
import config.Refined.{Max300, Max5, Max50}
import models.requests.psr._
import utils.UserAnswersUtils.UserAnswersOps
import pages.nonsipp.membersurrenderedbenefits._
import models._
import pages.nonsipp.membertransferout._
import models.softdelete.SoftDeletedMember
import pages.nonsipp.employercontributions._
import pages.nonsipp.memberdetails._
import org.scalatest.freespec.AnyFreeSpec
import pages.nonsipp.membercontributions._
import pages.nonsipp.memberreceivedpcls._
import viewmodels.models.MemberState.Deleted
import controllers.TestValues
import cats.implicits.catsSyntaxEitherId
import pages.nonsipp.receivetransfer._
import pages.nonsipp.memberpensionpayments._
import eu.timepit.refined.refineMV
import pages.nonsipp.{FbStatus, FbVersionPage}
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino
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
  private val pensionAmountReceivedTransformer = new PensionAmountReceivedTransformer()

  private val memberPaymentsTransformer =
    new MemberPaymentsTransformer(
      transfersInTransformer,
      transfersOutTransformer,
      pensionSurrenderTransformer,
      pensionAmountReceivedTransformer
    )

  private val index = refineMV[Max300.Refined](1)
  private val employerContribsIndex = refineMV[Max50.Refined](1)
  private val transfersInIndex = refineMV[Max5.Refined](1)
  private val transfersOutIndex = refineMV[Max5.Refined](1)

  // Test data: all sections of Member Payments have payments made
  private val activeMemberAllSections = MemberDetails(
    state = MemberState.New,
    memberPSRVersion = Some("001"),
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
      SurrenderedBenefits(
        totalSurrendered = 12.34,
        dateOfSurrender = LocalDate.of(2022, 12, 12),
        surrenderReason = "some reason"
      )
    ),
    pensionAmountReceived = Some(12.34)
  )

  private val deletedMemberAllSections = activeMemberAllSections.copy(state = MemberState.Deleted)

  private val memberPaymentsAllSections = MemberPayments(
    recordVersion = Some("001"),
    memberDetails = List(
      activeMemberAllSections,
      deletedMemberAllSections
    ),
    employerContributionsDetails = SectionDetails(made = true, completed = true),
    transfersInMade = Some(true),
    transfersOutMade = Some(true),
    unallocatedContribsMade = Some(true),
    unallocatedContribAmount = Some(money.value),
    memberContributionMade = Some(true),
    lumpSumReceived = Some(true),
    benefitsSurrenderedDetails = SectionDetails(made = true, completed = true),
    pensionReceived = SectionDetails(made = true, completed = true)
  )

  private val softDeletedMemberAllSections = SoftDeletedMember(
    memberPSRVersion = Some("001"),
    memberDetails = MemberPersonalDetails(
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
    totalMemberContribution = Some(money),
    memberLumpSumReceived = Some(MemberLumpSumReceived(money.value, money.value)),
    transfersOut = List(
      TransfersOut(
        schemeName = schemeName,
        dateOfTransfer = localDate,
        transferSchemeType = PensionSchemeType.RegisteredPS("456")
      )
    ),
    pensionSurrendered = Some(
      SurrenderedBenefits(
        totalSurrendered = 12.34,
        dateOfSurrender = LocalDate.of(2022, 12, 12),
        surrenderReason = "some reason"
      )
    ),
    totalAmountPensionPaymentsPage = Some(Money(12.34))
  )

  private val userAnswersAllSections = defaultUserAnswers
    .unsafeSet(MemberPaymentsRecordVersionPage(srn), "001")
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
    .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
    .unsafeSet(MemberDetailsCompletedPage(srn, index), SectionCompleted)
    .unsafeSet(MemberPsrVersionPage(srn, index), "001")
    .unsafeSet(MemberStatus(srn, index), MemberState.New)
    // employer contributions
    .unsafeSet(EmployerContributionsPage(srn), true)
    .unsafeSet(EmployerNamePage(srn, index, employerContribsIndex), employerName)
    .unsafeSet(EmployerTypeOfBusinessPage(srn, index, employerContribsIndex), IdentityType.UKCompany)
    .unsafeSet(EmployerCompanyCrnPage(srn, index, employerContribsIndex), ConditionalYesNo(crn.asRight[String]))
    .unsafeSet(TotalEmployerContributionPage(srn, index, employerContribsIndex), money)
    .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
    .unsafeSet(EmployerContributionsCompleted(srn, index, employerContribsIndex), SectionCompleted)
    .unsafeSet(EmployerContributionsMemberListPage(srn), true)
    .unsafeSet(EmployerContributionsProgress(srn, index, employerContribsIndex), SectionJourneyStatus.Completed)
    // unallocated employer contributions
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
    // member contributions
    .unsafeSet(MemberContributionsPage(srn), true)
    .unsafeSet(MemberContributionsListPage(srn), true)
    .unsafeSet(TotalMemberContributionPage(srn, index), money)
    // transfers in
    .unsafeSet(TransfersInSectionCompleted(srn, index, transfersInIndex), SectionCompleted)
    .unsafeSet(TransfersInJourneyStatus(srn), SectionStatus.Completed)
    .unsafeSet(TransferringSchemeNamePage(srn, index, transfersInIndex), schemeName)
    .unsafeSet(WhenWasTransferReceivedPage(srn, index, transfersInIndex), localDate)
    .unsafeSet(TotalValueTransferPage(srn, index, transfersInIndex), money)
    .unsafeSet(DidTransferIncludeAssetPage(srn, index, transfersInIndex), true)
    .unsafeSet(TransferringSchemeTypePage(srn, index, transfersInIndex), PensionSchemeType.RegisteredPS("123"))
    .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
    .unsafeSet(TransferReceivedMemberListPage(srn), true)
    .unsafeSet(ReportAnotherTransferInPage(srn, index, transfersInIndex), false)
    // pcls
    .unsafeSet(PensionCommencementLumpSumPage(srn), true)
    .unsafeSet(PclsMemberListPage(srn), true)
    .unsafeSet(PensionCommencementLumpSumAmountPage(srn, index), PensionCommencementLumpSum(money, money))
    // transfers out
    .unsafeSet(SchemeTransferOutPage(srn), true)
    .unsafeSet(TransferOutMemberListPage(srn), true)
    .unsafeSet(TransfersOutJourneyStatus(srn), SectionStatus.Completed)
    .unsafeSet(TransfersOutSectionCompleted(srn, index, transfersOutIndex), SectionCompleted)
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
    // soft deleted
    .unsafeSet(SoftDeletedMembers(srn), List(softDeletedMemberAllSections))

  // Test data: no sections of Member Payments have payments made
  private val activeMemberNoSections = MemberDetails(
    state = MemberState.New,
    memberPSRVersion = Some("001"),
    personalDetails = MemberPersonalDetails(
      firstName = memberDetails.firstName,
      lastName = memberDetails.lastName,
      nino = Some(nino.value),
      reasonNoNINO = None,
      dateOfBirth = memberDetails.dob
    ),
    employerContributions = List.empty,
    transfersIn = List.empty,
    totalContributions = None,
    memberLumpSumReceived = None,
    transfersOut = List.empty,
    benefitsSurrendered = None,
    pensionAmountReceived = None
  )

  private val deletedMemberNoSections = activeMemberNoSections.copy(state = MemberState.Deleted)

  private val memberPaymentsNoSections = MemberPayments(
    recordVersion = Some("001"),
    memberDetails = List(
      activeMemberNoSections,
      deletedMemberNoSections
    ),
    employerContributionsDetails = SectionDetails(made = false, completed = true),
    transfersInMade = Some(false),
    transfersOutMade = Some(false),
    unallocatedContribsMade = Some(false),
    unallocatedContribAmount = None,
    memberContributionMade = Some(false),
    lumpSumReceived = Some(false),
    benefitsSurrenderedDetails = SectionDetails(made = false, completed = true),
    pensionReceived = SectionDetails(made = false, completed = true)
  )

  private val emptyMemberPayments = MemberPayments(
    recordVersion = None,
    memberDetails = Nil,
    employerContributionsDetails = SectionDetails(made = false, completed = false),
    transfersInMade = None,
    transfersOutMade = None,
    unallocatedContribsMade = None,
    unallocatedContribAmount = None,
    memberContributionMade = None,
    lumpSumReceived = None,
    benefitsSurrenderedDetails = SectionDetails(made = false, completed = false),
    pensionReceived = SectionDetails(made = false, completed = false)
  )

  private val softDeletedMemberNoSections = SoftDeletedMember(
    memberPSRVersion = Some("001"),
    memberDetails = MemberPersonalDetails(
      firstName = memberDetails.firstName,
      lastName = memberDetails.lastName,
      nino = Some(nino.value),
      reasonNoNINO = None,
      dateOfBirth = memberDetails.dob
    ),
    employerContributions = List.empty,
    transfersIn = List.empty,
    totalMemberContribution = None,
    memberLumpSumReceived = None,
    transfersOut = List.empty,
    pensionSurrendered = None,
    totalAmountPensionPaymentsPage = None
  )

  private val userAnswersNoSections = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
    .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
    .unsafeSet(MemberDetailsCompletedPage(srn, index), SectionCompleted)
    .unsafeSet(MemberPsrVersionPage(srn, index), "001")
    .unsafeSet(MemberPaymentsRecordVersionPage(srn), "001")
    .unsafeSet(MemberStatus(srn, index), MemberState.New)
    // employer contributions
    .unsafeSet(EmployerContributionsPage(srn), false)
    .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
    .unsafeSet(EmployerContributionsMemberListPage(srn), true)
    // unallocated employer contributions
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), false)
    // member contributions
    .unsafeSet(MemberContributionsPage(srn), false)
    .unsafeSet(MemberContributionsListPage(srn), false)
    // transfers in
    .unsafeSet(TransfersInJourneyStatus(srn), SectionStatus.Completed)
    .unsafeSet(DidSchemeReceiveTransferPage(srn), false)
    .unsafeSet(TransferReceivedMemberListPage(srn), true)
    // pcls
    .unsafeSet(PensionCommencementLumpSumPage(srn), false)
    .unsafeSet(PclsMemberListPage(srn), false)
    // transfers out
    .unsafeSet(SchemeTransferOutPage(srn), false)
    .unsafeSet(TransferOutMemberListPage(srn), true)
    .unsafeSet(TransfersOutJourneyStatus(srn), SectionStatus.Completed)
    // pension surrender
    .unsafeSet(SurrenderedBenefitsJourneyStatus(srn), SectionStatus.Completed)
    .unsafeSet(SurrenderedBenefitsPage(srn), false)
    .unsafeSet(SurrenderedBenefitsMemberListPage(srn), true)
    // pension payments
    .unsafeSet(PensionPaymentsReceivedPage(srn), false)
    .unsafeSet(PensionPaymentsJourneyStatus(srn), SectionStatus.Completed)
    .unsafeSet(MemberPensionPaymentsListPage(srn), true)
    // soft deleted
    .unsafeSet(SoftDeletedMembers(srn), List(softDeletedMemberNoSections))

  "MemberPaymentsTransformer - To Etmp" - {
    "should return empty List when userAnswer is empty" in {
      val result = memberPaymentsTransformer.transformToEtmp(srn, defaultUserAnswers, defaultUserAnswers)
      result shouldMatchTo None
    }

    "should return member payments without recordVersion when initial UA and current UA are different" in {
      val result = memberPaymentsTransformer.transformToEtmp(srn, userAnswersAllSections, emptyUserAnswers)
      val memberDetails = memberPaymentsAllSections.memberDetails
      result shouldMatchTo Some(
        memberPaymentsAllSections
          .copy(
            recordVersion = None,
            memberDetails = memberDetails.map(md => if (md.state == Deleted) md else md.copy(memberPSRVersion = None))
          )
      )
    }

    "should return member payments with memberPsrVersion when initial UA contains completion status semantics but initial UA and current UA are same" in {
      val initial = userAnswersAllSections
        .remove(
          PensionCommencementLumpSumAmountPage(srn, refineMV(1))
        )
        .get

      val current = userAnswersAllSections.unsafeSet(
        PensionCommencementLumpSumAmountPage(srn, refineMV(1)),
        PensionCommencementLumpSum(Money.zero, Money.zero)
      )
      val result = memberPaymentsTransformer.transformToEtmp(srn, current, initial)
      result shouldMatchTo Some(
        memberPaymentsAllSections.copy(
          recordVersion = None, // todo: this is a bug and should not be removed, there is a ticket to address member payments comparisons at the case class level instead of json
          memberDetails = List(
            activeMemberAllSections.copy(
              memberLumpSumReceived = Some(MemberLumpSumReceived.zero)
            ),
            deletedMemberAllSections
          )
        )
      )
    }

    "should return member payments with recordVersion/memberPsrVersion when initial UA and current UA are same" in {
      val result = memberPaymentsTransformer.transformToEtmp(srn, userAnswersAllSections, userAnswersAllSections)
      result shouldMatchTo Some(memberPaymentsAllSections)
    }

    "should return member payments with nino spaces trimmed" in {
      val ninoWithSpaces = Nino("SC 13 51 60 C")
      val userAnswersWithNinoSpaces = userAnswersAllSections
        .unsafeSet(MemberDetailsNinoPage(srn, index), ninoWithSpaces)

      val result = memberPaymentsTransformer.transformToEtmp(srn, userAnswersWithNinoSpaces, userAnswersAllSections)
      result.get.memberDetails(0).personalDetails.nino.value shouldMatchTo ninoWithSpaces.value.replace(" ", "")
    }

    "Member state" - {
      "should return member state New when member was just added" in {
        val userAnswersNewMember = defaultUserAnswers
          .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
          .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
          .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
          .unsafeSet(MemberDetailsCompletedPage(srn, index), SectionCompleted)
          .unsafeSet(MemberStatus(srn, index), MemberState.New)

        val expected = emptyMemberPayments.copy(
          memberDetails = List(
            activeMemberNoSections.copy(memberPSRVersion = None)
          )
        )

        val result =
          memberPaymentsTransformer.transformToEtmp(srn, userAnswersNewMember, initialUA = defaultUserAnswers)
        result shouldMatchTo Some(expected)
      }

      "should return member state New when PSR state is Submitted and member was just added before first POST since submitted" in {
        val userAnswersNewMember = defaultUserAnswers
          .unsafeSet(FbStatus(srn), Submitted)
          .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
          .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
          .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
          .unsafeSet(MemberDetailsCompletedPage(srn, index), SectionCompleted)
          .unsafeSet(MemberStatus(srn, index), MemberState.New)

        val expected = emptyMemberPayments.copy(
          memberDetails = List(
            activeMemberNoSections.copy(
              memberPSRVersion = None,
              state = MemberState.New
            )
          )
        )

        val result =
          memberPaymentsTransformer.transformToEtmp(
            srn,
            userAnswersNewMember,
            initialUA = defaultUserAnswers,
            previousVersionUA = None
          )

        result shouldMatchTo Some(expected)
      }
    }
  }

  "MemberPaymentsTransformer - From Etmp" - {
    "should return correct user answers" in {
      val result = memberPaymentsTransformer.transformFromEtmp(defaultUserAnswers, None, srn, memberPaymentsAllSections)
      result shouldMatchTo Try(userAnswersAllSections)
    }

    "should return Completed for Employer Contributions if no contributions are added" in {
      val result = memberPaymentsTransformer.transformFromEtmp(defaultUserAnswers, None, srn, memberPaymentsNoSections)
      result shouldMatchTo Try(userAnswersNoSections)
    }

    "SafeToHardDelete" - {

      "should set SafeToHardDelete when member version and PSR version are missing - this member was added before the first POST to ETMP" in {

        val newMember = memberPaymentsNoSections.copy(
          memberDetails = List(
            activeMemberNoSections.copy(
              state = MemberState.New,
              memberPSRVersion = None
            )
          )
        )

        val result = memberPaymentsTransformer.transformFromEtmp(defaultUserAnswers, None, srn, newMember).get
        result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.New)
        result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo Some(Flag)
      }

      "PSR status is Compiled" - {

        "should set SafeToHardDelete when member version is the same as PSR version and MemberState is New -" +
          "this member was added in the current version" in {
          val previousUserAnswers = defaultUserAnswers
            .unsafeSet(FbVersionPage(srn), "001")
            .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
            .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
            .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
            .unsafeSet(MemberStatus(srn, index), MemberState.New)
            .unsafeSet(MemberPsrVersionPage(srn, index), "001")

          val userAnswers = defaultUserAnswers
            .unsafeSet(FbVersionPage(srn), "002")
            .unsafeSet(FbStatus(srn), Compiled)

          val existingMember = memberPaymentsNoSections.copy(
            memberDetails = List(
              activeMemberNoSections.copy(
                state = MemberState.New,
                memberPSRVersion = Some("002")
              )
            )
          )

          val result =
            memberPaymentsTransformer.transformFromEtmp(userAnswers, Some(previousUserAnswers), srn, existingMember).get

          result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.New)
          result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo Some(Flag)
        }

        "should set SafeToHardDelete when member version is the same as PSR version -" +
          "this member was added in the current version and submitted to ETMP prior to the first submission" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(FbVersionPage(srn), "001")
            .unsafeSet(FbStatus(srn), Compiled)

          val newMember = memberPaymentsNoSections.copy(
            memberDetails = List(
              activeMemberNoSections.copy(
                state = MemberState.New,
                memberPSRVersion = Some("001")
              )
            )
          )

          val result = memberPaymentsTransformer.transformFromEtmp(userAnswers, None, srn, newMember).get
          result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.New)
          result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo Some(Flag)
        }

        "should set SafeToHardDelete when member version is the same as fb version and MemberStatus is Changed-" +
          "this member was added in the current version and submitted to ETMP prior to the first submission so Changed status doesn't matter" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(FbVersionPage(srn), "001")
            .unsafeSet(FbStatus(srn), Compiled)

          val newMember = memberPaymentsNoSections.copy(
            memberDetails = List(
              activeMemberNoSections.copy(
                state = MemberState.Changed,
                memberPSRVersion = Some("001")
              )
            )
          )

          val result = memberPaymentsTransformer.transformFromEtmp(userAnswers, None, srn, newMember).get
          result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.Changed)
          result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo Some(Flag)
        }

        "with previous user answers" - {

          "should NOT set SafeToHardDelete when member version is not the same as PSR version and MemberState is New -" +
            "this member was added in the previous submission" in {
            val previousUserAnswers = defaultUserAnswers
              .unsafeSet(FbVersionPage(srn), "001")
              .unsafeSet(FbStatus(srn), Submitted)
              .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
              .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
              .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
              .unsafeSet(MemberStatus(srn, index), MemberState.New)
              .unsafeSet(MemberPsrVersionPage(srn, index), "001")

            val userAnswers = defaultUserAnswers
              .unsafeSet(FbVersionPage(srn), "002")
              .unsafeSet(FbStatus(srn), Compiled)

            val existingMember = memberPaymentsNoSections.copy(
              memberDetails = List(
                activeMemberNoSections.copy(
                  state = MemberState.New,
                  memberPSRVersion = Some("001")
                )
              )
            )

            val result =
              memberPaymentsTransformer
                .transformFromEtmp(userAnswers, Some(previousUserAnswers), srn, existingMember)
                .get

            result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.New)
            result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo None
          }

          "should NOT set SafeToHardDelete when member version is the same as PSR version and MemberState is Changed -" +
            "this member has been a part of a declaration" in {
            val previousUserAnswers = defaultUserAnswers
              .unsafeSet(FbVersionPage(srn), "001")
              .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
              .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
              .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
              .unsafeSet(MemberStatus(srn, index), MemberState.New)
              .unsafeSet(MemberPsrVersionPage(srn, index), "001")

            val userAnswers = defaultUserAnswers
              .unsafeSet(FbVersionPage(srn), "002")
              .unsafeSet(FbStatus(srn), Compiled)

            val existingMember = memberPaymentsNoSections.copy(
              memberDetails = List(
                activeMemberNoSections.copy(
                  state = MemberState.Changed,
                  memberPSRVersion = Some("002")
                )
              )
            )

            val result =
              memberPaymentsTransformer
                .transformFromEtmp(userAnswers, Some(previousUserAnswers), srn, existingMember)
                .get

            result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.Changed)
            result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo None
          }
        }
      }

      "PSR status is Submitted" - {

        "should NOT set SafeToHardDelete when member and PSR version are 001 and MemberStatus is New -" +
          "this member was added in the previous version but since the state is Submitted," +
          "a change must first be sent to ETMP to update the version to current + 1 and state to Compiled" in {

          val userAnswers = defaultUserAnswers
            .unsafeSet(FbVersionPage(srn), "001")
            .unsafeSet(FbStatus(srn), Submitted)

          val newMember = memberPaymentsNoSections.copy(
            memberDetails = List(
              activeMemberNoSections.copy(
                state = MemberState.New,
                memberPSRVersion = Some("001")
              )
            )
          )

          val result = memberPaymentsTransformer.transformFromEtmp(userAnswers, None, srn, newMember).get
          result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.New)
          result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo None
        }

        "should NOT set SafeToHardDelete when member and PSR version are 001 and MemberStatus is Changed -" +
          "this member was added in the previous version but since the state is Submitted," +
          "a change must first be sent to ETMP to update the version to current + 1 and state to Compiled" in {

          val userAnswers = defaultUserAnswers
            .unsafeSet(FbVersionPage(srn), "001")
            .unsafeSet(FbStatus(srn), Submitted)

          val newMember = memberPaymentsNoSections.copy(
            memberDetails = List(
              activeMemberNoSections.copy(
                state = MemberState.Changed,
                memberPSRVersion = Some("001")
              )
            )
          )

          val result = memberPaymentsTransformer.transformFromEtmp(userAnswers, None, srn, newMember).get
          result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.Changed)
          result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo None
        }

        "should NOT set SafeToHardDelete when member and PSR version are the same and greater than 001 and MemberStatus is New -" +
          "this member was added in the previous version but since the state is Submitted," +
          "a change must first be sent to ETMP to update the version to current + 1 and state to Compiled" in {

          val userAnswers = defaultUserAnswers
            .unsafeSet(FbVersionPage(srn), "002")
            .unsafeSet(FbStatus(srn), Submitted)

          val newMember = memberPaymentsNoSections.copy(
            memberDetails = List(
              activeMemberNoSections.copy(
                state = MemberState.New,
                memberPSRVersion = Some("002")
              )
            )
          )

          val result = memberPaymentsTransformer.transformFromEtmp(userAnswers, None, srn, newMember).get
          result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.New)
          result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo None
        }

        "with previous user answers" - {
          "should NOT set SafeToHardDelete when member and PSR version are the same and MemberStatus is New -" +
            "this member was added in the previous version but since the state is Submitted," +
            "a change must first be sent to ETMP to update the version to current + 1 and state to Compiled" in {

            val previousUserAnswers = defaultUserAnswers
              .unsafeSet(FbVersionPage(srn), "002")
              .unsafeSet(FbStatus(srn), Compiled)
              .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
              .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
              .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
              .unsafeSet(MemberStatus(srn, index), MemberState.New)
              .unsafeSet(MemberPsrVersionPage(srn, index), "002")

            val userAnswers = defaultUserAnswers
              .unsafeSet(FbVersionPage(srn), "002")
              .unsafeSet(FbStatus(srn), Submitted)

            val memberPayments = memberPaymentsNoSections.copy(
              memberDetails = List(
                activeMemberNoSections.copy(
                  state = MemberState.New,
                  memberPSRVersion = Some("002")
                )
              )
            )

            val result = memberPaymentsTransformer
              .transformFromEtmp(userAnswers, Some(previousUserAnswers), srn, memberPayments)
              .get
            result.get(MemberStatus(srn, refineMV(1))) shouldMatchTo Some(MemberState.New)
            result.get(SafeToHardDelete(srn, refineMV(1))) shouldMatchTo None
          }
        }
      }
    }
  }
}
