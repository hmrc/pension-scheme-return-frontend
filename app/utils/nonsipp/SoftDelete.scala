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

import pages.nonsipp.memberdetails._
import pages.nonsipp.membertransferout.TransfersOutSectionCompleted.TransfersOutSectionCompletedUserAnswersOps
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer._
import models.requests.psr._
import pages.nonsipp.memberpensionpayments._
import pages.nonsipp.receivetransfer.TransfersInSectionCompleted.TransfersInSectionCompletedUserAnswersOps
import play.api.libs.json.Reads
import pages.nonsipp.membersurrenderedbenefits._
import models.{IdentityType, UserAnswers}
import pages.nonsipp.membertransferout._
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import models.requests.DataRequest
import pages.nonsipp.employercontributions.EmployerContributionsProgress.EmployerContributionsUserAnswersOps
import models.softdelete.SoftDeletedMember
import pages.nonsipp.employercontributions._
import pages.nonsipp.membercontributions._
import queries.{Gettable, Removable}
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsCompleted.SurrenderedBenefitsUserAnswersOps
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import cats.syntax.apply._
import config.RefinedTypes.{Max300, Max5, Max50}
import controllers.PSRController

import scala.util.{Failure, Success, Try}

trait SoftDelete extends PSRController {

  protected def softDeleteMember(srn: Srn, index: Max300)(implicit req: DataRequest[?]): Try[UserAnswers] =
    softDeleteMember(srn, index, req.userAnswers)

  /**
   * Moves member payments user answers to a "soft deleted" array and hard deletes the original answers
   *
   * Member is only soft deleted when a NewMember flag is NOT set against a member index. This is set when:
   *   - a member is newly added in a journey (manual or upload), prior to declaration
   *   - during GET from ETMP in the member transformer (new members are identified)
   *
   *   - Only remove completed flags when the member is soft deleted
   *   - Return None when something has gone wrong - todo: change to type that can store the error for logging
   *   - When hard deleting members after moving them to the soft delete group, the function checks to see if any
   *     section journeys still exist (e.g. employer contributions). If they don't, delete that sections completed flag.
   *   - For the member payments section, the function checks to see if the initial section page has been completed:
   *     - if true (user wants to add to the section): check to see if any completed flags exist for the section. If
   *       none, delete all flags for this section so the task list shows "Not Started"
   *     - if false: check if any members still exist, if they don't, remove all flags for this section so the task list
   *       shows "Not Started"
   */
  protected def softDeleteMember(srn: Srn, index: Max300, userAnswers: UserAnswers): Try[UserAnswers] = {
    val ua = userAnswers
    def get[A: Reads](f: (Srn, Max300) => Gettable[A]): Option[A] = ua.get(f(srn, index))

    val memberDetails: Try[MemberPersonalDetails] = (
      get(MemberDetailsPage.apply),
      get(NoNINOPage.apply),
      get(MemberDetailsNinoPage.apply).map(_.value)
    ) match {
      case (None, _, _) => Failure(new Exception("Missing member details when trying to soft delete member"))
      case (_, None, None) =>
        Failure(new Exception("Missing both NINO or no NINO reason when trying to soft delete member"))
      case (_, Some(_), Some(_)) =>
        Failure(new Exception("Both NINO and no NINO reason present when trying to soft delete member"))
      case (Some(nameDob), noNino, nino) =>
        Success(MemberPersonalDetails(nameDob.firstName, nameDob.lastName, nino, noNino, nameDob.dob))
    }

    val memberDetailsPages: List[Removable[?]] = List(
      MemberDetailsPage(srn, index),
      DoesMemberHaveNinoPage(srn, index),
      NoNINOPage(srn, index),
      MemberDetailsNinoPage(srn, index),
      MemberDetailsCompletedPage(srn, index)
    )

    val employerContributionsPages: Max50 => List[Removable[?]] = secondaryIndex =>
      List(
        EmployerContributionsProgress(srn, index, secondaryIndex),
        EmployerNamePage(srn, index, secondaryIndex),
        EmployerTypeOfBusinessPage(srn, index, secondaryIndex),
        EmployerCompanyCrnPage(srn, index, secondaryIndex),
        PartnershipEmployerUtrPage(srn, index, secondaryIndex),
        OtherEmployeeDescriptionPage(srn, index, secondaryIndex),
        TotalEmployerContributionPage(srn, index, secondaryIndex),
        EmployerContributionsCompleted(srn, index, secondaryIndex)
      )

    val transfersInPages: Max5 => List[Removable[?]] = secondaryIndex =>
      List(
        TransfersInSectionCompleted(srn, index, secondaryIndex),
        TransferringSchemeNamePage(srn, index, secondaryIndex),
        WhenWasTransferReceivedPage(srn, index, secondaryIndex),
        TotalValueTransferPage(srn, index, secondaryIndex),
        DidTransferIncludeAssetPage(srn, index, secondaryIndex),
        TransferringSchemeTypePage(srn, index, secondaryIndex),
        ReportAnotherTransferInPage(srn, index, secondaryIndex),
        ReceiveTransferProgress(srn, index, secondaryIndex)
      )

    val transfersOutPages: Max5 => List[Removable[?]] = secondaryIndex =>
      List(
        TransfersOutSectionCompleted(srn, index, secondaryIndex),
        ReceivingSchemeNamePage(srn, index, secondaryIndex),
        WhenWasTransferMadePage(srn, index, secondaryIndex),
        ReceivingSchemeTypePage(srn, index, secondaryIndex),
        ReportAnotherTransferOutPage(srn, index, secondaryIndex),
        MemberTransferOutProgress(srn, index, secondaryIndex)
      )

    val surrenderedBenefitsPages: List[Removable[?]] = List(
      SurrenderedBenefitsCompletedPage(srn, index),
      SurrenderedBenefitsAmountPage(srn, index),
      WhenDidMemberSurrenderBenefitsPage(srn, index),
      WhyDidMemberSurrenderBenefitsPage(srn, index),
      MemberSurrenderedBenefitsProgress(srn, index)
    )

    val memberContributionsPages: List[Removable[?]] = List(
      TotalMemberContributionPage(srn, index)
    )

    val memberPensionPaymentsPages: List[Removable[?]] = List(
      TotalAmountPensionPaymentsPage(srn, index)
    )

    val memberCommencementLumpSumPages: List[Removable[?]] = List(
      PensionCommencementLumpSumAmountPage(srn, index)
    )

    val employerContributions: List[EmployerContributions] = ua
      .employerContributionsCompleted(index)
      .foldLeft(List.empty[EmployerContributions]) { (acc, secondaryIndex) =>
        def get[A: Reads](f: (Srn, Max300, Max50) => Gettable[A]): Option[A] = ua.get(f(srn, index, secondaryIndex))
        (
          get(EmployerNamePage.apply),
          get(EmployerTypeOfBusinessPage.apply),
          get(TotalEmployerContributionPage.apply)
        ).flatMapN { (employerName, employerTypeOfBusiness, totalEmployerContribution) =>
          val employerType: Option[EmployerType] = employerTypeOfBusiness match {
            case IdentityType.Individual => None
            case IdentityType.UKCompany =>
              get(EmployerCompanyCrnPage.apply).map(v => EmployerType.UKCompany(v.value.map(_.value)))
            case IdentityType.UKPartnership =>
              get(PartnershipEmployerUtrPage.apply).map(v => EmployerType.UKPartnership(v.value.map(_.value)))
            case IdentityType.Other =>
              get(OtherEmployeeDescriptionPage.apply).map(EmployerType.Other.apply)
          }

          employerType.map(empType =>
            EmployerContributions(
              employerName = employerName,
              employerType = empType,
              totalTransferValue = totalEmployerContribution.value
            )
          )
        }.fold(List.empty[EmployerContributions])(acc :+ _)
      }

    val transfersIn: List[TransfersIn] = ua
      .transfersInSectionCompleted(srn, index)
      .foldLeft(List.empty[TransfersIn]) { (acc, secondaryIndex) =>
        def get[A: Reads](f: (Srn, Max300, Max5) => Gettable[A]): Option[A] = ua.get(f(srn, index, secondaryIndex))
        (
          get(TransferringSchemeNamePage.apply),
          get(WhenWasTransferReceivedPage.apply),
          get(TransferringSchemeTypePage.apply),
          get(TotalValueTransferPage.apply).map(_.value),
          get(DidTransferIncludeAssetPage.apply)
        ).mapN(TransfersIn.apply).fold(List.empty[TransfersIn])(acc :+ _)
      }

    val transfersOut: List[TransfersOut] = ua
      .transfersOutSectionCompleted(srn, index)
      .foldLeft(List.empty[TransfersOut]) { (acc, secondaryIndex) =>
        def get[A: Reads](f: (Srn, Max300, Max5) => Gettable[A]): Option[A] = ua.get(f(srn, index, secondaryIndex))
        (
          get(ReceivingSchemeNamePage.apply),
          get(WhenWasTransferMadePage.apply),
          get(ReceivingSchemeTypePage.apply)
        ).mapN(TransfersOut.apply).fold(List.empty[TransfersOut])(acc :+ _)
      }

    val surrenderedBenefits: Option[SurrenderedBenefits] = (
      get(SurrenderedBenefitsAmountPage.apply).map(_.value),
      get(WhenDidMemberSurrenderBenefitsPage.apply),
      get(WhyDidMemberSurrenderBenefitsPage.apply)
    ).mapN(SurrenderedBenefits.apply)

    val memberLumpSumReceived: Option[MemberLumpSumReceived] =
      get(PensionCommencementLumpSumAmountPage.apply).map(lumpSum =>
        MemberLumpSumReceived(
          lumpSumAmount = lumpSum.lumpSumAmount.value,
          designatedPensionAmount = lumpSum.designatedPensionAmount.value
        )
      )

    val memberToDelete: Try[SoftDeletedMember] = memberDetails.map(
      SoftDeletedMember(
        memberPSRVersion = None, // do not set member PSR version when soft deleting
        _,
        employerContributions,
        transfersIn,
        transfersOut,
        surrenderedBenefits,
        memberLumpSumReceived,
        get(TotalMemberContributionPage.apply),
        get(TotalAmountPensionPaymentsPage.apply)
      )
    )

    memberToDelete.flatMap(member =>
      userAnswers
        .setWhen(userAnswers.get(SafeToHardDelete(srn, index)).isEmpty)(
          SoftDeletedMembers(srn), {
            val existingSoftDeletedMembers = userAnswers.get(SoftDeletedMembers(srn)).getOrElse(Nil)
            existingSoftDeletedMembers :+ member
          }
        )
        .remove(memberDetailsPages)
        .flatMap { ua =>
          ua.employerContributionsCompleted(index)
            .foldLeft(Try(ua))((acc, secondaryIndex) => acc.remove(employerContributionsPages(secondaryIndex)))
            .when(_.get(EmployerContributionsPage(srn)))(
              ifTrue = _.removeOnlyWhen(!EmployerContributionsProgress.exist(_))(EmployerContributionsPage(srn)),
              ifFalse = _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(EmployerContributionsPage(srn))
            )
        }
        .flatMap(ua =>
          ua.transfersInSectionCompleted(srn, index)
            .foldLeft(Try(ua))((acc, secondaryIndex) => acc.remove(transfersInPages(secondaryIndex)))
            .when(_.get(DidSchemeReceiveTransferPage(srn)))(
              ifTrue = _.removeOnlyWhen(!TransfersInSectionCompleted.exists(_))(
                DidSchemeReceiveTransferPage(srn)
              ),
              ifFalse = _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(DidSchemeReceiveTransferPage(srn))
            )
        )
        .flatMap(ua =>
          ua.transfersOutSectionCompleted(srn, index)
            .foldLeft(Try(ua))((acc, secondaryIndex) => acc.remove(transfersOutPages(secondaryIndex)))
            .when(_.get(SchemeTransferOutPage(srn)))(
              ifTrue = _.removeOnlyWhen(!TransfersOutSectionCompleted.exists(_))(
                SchemeTransferOutPage(srn)
              ),
              ifFalse = _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(SchemeTransferOutPage(srn))
            )
        )
        .flatMap { ua =>
          ua.removeOnly(surrenderedBenefitsPages)
            .when(_.get(SurrenderedBenefitsPage(srn)))(
              ifTrue = _.removeOnlyWhen(_.surrenderedBenefitsCompleted().isEmpty)(SurrenderedBenefitsPage(srn)),
              ifFalse = _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(SurrenderedBenefitsPage(srn))
            )
        }
        .flatMap { ua =>
          ua.removeOnly(memberCommencementLumpSumPages)
            .when(_.get(PensionCommencementLumpSumPage(srn)))(
              ifTrue = _.removeOnlyWhen(_.map(PensionCommencementLumpSumAmountPage.all()).isEmpty)(
                PensionCommencementLumpSumPage(srn)
              ),
              ifFalse = _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(
                PensionCommencementLumpSumPage(srn)
              )
            )
        }
        .flatMap { ua =>
          ua.removeOnly(memberPensionPaymentsPages)
            .when(_.get(PensionPaymentsReceivedPage(srn)))(
              ifTrue = _.removeOnlyWhen(_.map(TotalAmountPensionPaymentsPage.all()).isEmpty)(
                PensionPaymentsReceivedPage(srn)
              ),
              ifFalse = _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(PensionPaymentsReceivedPage(srn))
            )
        }
        .flatMap { ua =>
          ua.removeOnly(memberContributionsPages)
            .when(_.get(MemberContributionsPage(srn)))(
              ifTrue = _.removeOnlyWhen(_.map(AllTotalMemberContributionPages(srn)).isEmpty)(
                MemberContributionsPage(srn)
              ),
              ifFalse = _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(
                MemberContributionsPage(srn)
              )
            )
        }
        .flatMap {
          _.removeOnlyWhen(_.membersDetails(srn).isEmpty)(
            UnallocatedEmployerContributionsPage(srn),
            UnallocatedEmployerAmountPage(srn)
          )
        }
        .remove(MemberStatus(srn, index))
        .remove(MemberPsrVersionPage(srn, index))
    )
  }
}
