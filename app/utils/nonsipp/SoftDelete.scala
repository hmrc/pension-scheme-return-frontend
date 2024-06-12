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
import config.Refined.{Max300, Max5, Max50}
import controllers.PSRController
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer._
import models.requests.psr._
import pages.nonsipp.memberpensionpayments._
import pages.nonsipp.receivetransfer.TransfersInSectionCompleted.TransfersInSectionCompletedUserAnswersOps
import play.api.libs.json.Reads
import pages.nonsipp.membersurrenderedbenefits._
import models.{IdentityType, UserAnswers}
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import models.requests.DataRequest
import pages.nonsipp.employercontributions.EmployerContributionsProgress.EmployerContributionsUserAnswersOps
import models.softdelete.SoftDeletedMember
import pages.nonsipp.employercontributions._
import pages.nonsipp.membercontributions._
import queries.{Gettable, Removable}
import pages.nonsipp.memberreceivedpcls.{
  PclsMemberListPage,
  PensionCommencementLumpSumAmountPage,
  PensionCommencementLumpSumPage
}
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsCompleted.SurrenderedBenefitsUserAnswersOps
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import cats.syntax.apply._
import cats.syntax.option._
import pages.nonsipp.membertransferout._

import scala.util.{Failure, Try}

trait SoftDelete { _: PSRController =>

  protected def softDeleteMember(srn: Srn, index: Max300)(implicit req: DataRequest[_]): Try[UserAnswers] =
    softDeleteMember(srn, index, req.userAnswers)

  /**
   * Moves member payments user answers to a "soft deleted" array and hard deletes the original answers
   *
   * - Only remove completed flags when the member is soft deleted
   * - Return None when something has gone wrong - todo: change to type that can store the error for logging
   * - When hard deleting members after moving them to the soft delete group, the function checks
   *   to see if any section journeys still exist (e.g. employer contributions).
   *   If they don't, delete that sections completed flag.
   * - For the member payments section, the function checks to see if the initial section page has been completed:
   *   - if true (user wants to add to the section): check to see if any completed flags exist for the section.
   *                                                 If none, delete all flags for this section so the task list shows "Not Started"
   *   - if false: check if any members still exist, if they don't, remove all flags for this section so the task list shows "Not Started"
   */
  protected def softDeleteMember(srn: Srn, index: Max300, userAnswers: UserAnswers): Try[UserAnswers] = {
    val ua = userAnswers
    def get[A: Reads](f: (Srn, Max300) => Gettable[A]): Option[A] = ua.get(f(srn, index))

    val memberDetails: Option[MemberPersonalDetails] = (
      get(MemberDetailsPage),
      get(NoNINOPage).some,
      get(MemberDetailsNinoPage).map(_.value).some
    ).mapN(
      (nameDob, noNino, nino) => MemberPersonalDetails(nameDob.firstName, nameDob.lastName, nino, noNino, nameDob.dob)
    )

    val memberDetailsPages: List[Removable[_]] = List(
      MemberDetailsPage(srn, index),
      DoesMemberHaveNinoPage(srn, index),
      NoNINOPage(srn, index),
      MemberDetailsNinoPage(srn, index),
      MemberDetailsCompletedPage(srn, index)
    )

    val employerContributionsPages: Max50 => List[Removable[_]] = secondaryIndex =>
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

    val transfersInPages: Max5 => List[Removable[_]] = secondaryIndex =>
      List(
        TransfersInSectionCompleted(srn, index, secondaryIndex),
        TransferringSchemeNamePage(srn, index, secondaryIndex),
        WhenWasTransferReceivedPage(srn, index, secondaryIndex),
        TotalValueTransferPage(srn, index, secondaryIndex),
        DidTransferIncludeAssetPage(srn, index, secondaryIndex),
        TransferringSchemeTypePage(srn, index, secondaryIndex),
        ReportAnotherTransferInPage(srn, index, secondaryIndex)
      )

    val transfersOutPages: Max5 => List[Removable[_]] = secondaryIndex =>
      List(
        TransfersOutSectionCompleted(srn, index, secondaryIndex),
        ReceivingSchemeNamePage(srn, index, secondaryIndex),
        WhenWasTransferMadePage(srn, index, secondaryIndex),
        ReceivingSchemeTypePage(srn, index, secondaryIndex),
        ReportAnotherTransferOutPage(srn, index, secondaryIndex)
      )

    val surrenderedBenefitsPages: List[Removable[_]] = List(
      SurrenderedBenefitsCompletedPage(srn, index),
      SurrenderedBenefitsAmountPage(srn, index),
      WhenDidMemberSurrenderBenefitsPage(srn, index),
      WhyDidMemberSurrenderBenefitsPage(srn, index)
    )

    val memberContributionsPages: List[Removable[_]] = List(
      TotalMemberContributionPage(srn, index)
    )

    val memberPensionPaymentsPages: List[Removable[_]] = List(
      TotalAmountPensionPaymentsPage(srn, index)
    )

    val memberCommencementLumpSumPages: List[Removable[_]] = List(
      PensionCommencementLumpSumAmountPage(srn, index)
    )

    val employerContributions: List[EmployerContributions] = ua
      .employerContributionsCompleted(srn, index)
      .foldLeft(List.empty[EmployerContributions])(
        (acc, secondaryIndex) => {
          def get[A: Reads](f: (Srn, Max300, Max50) => Gettable[A]): Option[A] = ua.get(f(srn, index, secondaryIndex))
          (
            get(EmployerNamePage),
            get(EmployerTypeOfBusinessPage),
            get(TotalEmployerContributionPage)
          ).flatMapN {
              (employerName, employerTypeOfBusiness, totalEmployerContribution) =>
                val employerType: Option[EmployerType] = employerTypeOfBusiness match {
                  case IdentityType.Individual => None
                  case IdentityType.UKCompany =>
                    get(EmployerCompanyCrnPage).map(v => EmployerType.UKCompany(v.value.map(_.value)))
                  case IdentityType.UKPartnership =>
                    get(PartnershipEmployerUtrPage).map(v => EmployerType.UKPartnership(v.value.map(_.value)))
                  case IdentityType.Other =>
                    get(OtherEmployeeDescriptionPage).map(EmployerType.Other)
                }

                employerType.map(
                  empType =>
                    EmployerContributions(
                      employerName = employerName,
                      employerType = empType,
                      totalTransferValue = totalEmployerContribution.value
                    )
                )
            }
            .fold(List.empty[EmployerContributions])(acc :+ _)
        }
      )

    val transfersIn: List[TransfersIn] = ua
      .transfersInSectionCompleted(srn, index)
      .foldLeft(List.empty[TransfersIn])(
        (acc, secondaryIndex) => {
          def get[A: Reads](f: (Srn, Max300, Max5) => Gettable[A]): Option[A] = ua.get(f(srn, index, secondaryIndex))
          (
            get(TransferringSchemeNamePage),
            get(WhenWasTransferReceivedPage),
            get(TransferringSchemeTypePage),
            get(TotalValueTransferPage).map(_.value),
            get(DidTransferIncludeAssetPage)
          ).mapN(TransfersIn.apply).fold(List.empty[TransfersIn])(acc :+ _)
        }
      )

    val transfersOut: List[TransfersOut] = ua
      .transfersOutSectionCompleted(srn, index)
      .foldLeft(List.empty[TransfersOut])(
        (acc, secondaryIndex) => {
          def get[A: Reads](f: (Srn, Max300, Max5) => Gettable[A]): Option[A] = ua.get(f(srn, index, secondaryIndex))
          (
            get(ReceivingSchemeNamePage),
            get(WhenWasTransferMadePage),
            get(ReceivingSchemeTypePage)
          ).mapN(TransfersOut.apply).fold(List.empty[TransfersOut])(acc :+ _)
        }
      )

    val surrenderedBenefits: Option[SurrenderedBenefits] = (
      get(SurrenderedBenefitsAmountPage).map(_.value),
      get(WhenDidMemberSurrenderBenefitsPage),
      get(WhyDidMemberSurrenderBenefitsPage)
    ).mapN(SurrenderedBenefits.apply)

    val memberLumpSumReceived: Option[MemberLumpSumReceived] = get(PensionCommencementLumpSumAmountPage.apply).map(
      lumpSum =>
        MemberLumpSumReceived(
          lumpSumAmount = lumpSum.lumpSumAmount.value,
          designatedPensionAmount = lumpSum.designatedPensionAmount.value
        )
    )

    val softDeletedMember = (
      get(MemberPsrVersionPage).some,
      memberDetails,
      employerContributions.some,
      transfersIn.some,
      transfersOut.some,
      surrenderedBenefits.some,
      memberLumpSumReceived.some,
      get(TotalMemberContributionPage).some,
      get(TotalAmountPensionPaymentsPage.apply).some
    ).mapN(SoftDeletedMember.apply)

    val existingSoftDeletedMembers = userAnswers.get(SoftDeletedMembers(srn)).getOrElse(Nil)

    softDeletedMember.fold[Try[UserAnswers]](
      Failure[UserAnswers] {
        new Exception(
          "Failure when building soft deleted member. Checks:" +
            s"memberDetails empty = ${memberDetails.isEmpty}\n" +
            s"employer contributions completed = ${userAnswers.employerContributionsCompleted(srn, index).size}\n" +
            s"employerContributions size = ${employerContributions.size}\n" +
            s"transfersIn empty = ${transfersIn.size}\n" +
            s"transfersOut size = ${transfersOut.size}\n"
        )
      }: Try[UserAnswers]
    )(
      sdm =>
        userAnswers
          .set(SoftDeletedMembers(srn), existingSoftDeletedMembers :+ sdm)
          .remove(memberDetailsPages)
          .flatMap { ua =>
            ua.employerContributionsCompleted(srn, index)
              .foldLeft(Try(ua))((acc, secondaryIndex) => acc.remove(employerContributionsPages(secondaryIndex)))
              .when(_.get(EmployerContributionsPage(srn)))(
                ifTrue = _.removeWhen(!EmployerContributionsProgress.exist(srn, _))(
                  EmployerContributionsSectionStatus(srn),
                  EmployerContributionsPage(srn)
                ),
                ifFalse = _.removeWhen(_.membersDetails(srn).isEmpty)(
                  EmployerContributionsSectionStatus(srn),
                  EmployerContributionsPage(srn)
                )
              )
          }
          .flatMap(
            ua =>
              ua.transfersInSectionCompleted(srn, index)
                .foldLeft(Try(ua))((acc, secondaryIndex) => acc.remove(transfersInPages(secondaryIndex)))
                .when(_.get(DidSchemeReceiveTransferPage(srn)))(
                  ifTrue = _.removeWhen(!TransfersInSectionCompleted.exists(srn, _))(
                    TransfersInJourneyStatus(srn),
                    DidSchemeReceiveTransferPage(srn)
                  ),
                  ifFalse = _.removeWhen(_.membersDetails(srn).isEmpty)(
                    TransfersInJourneyStatus(srn),
                    DidSchemeReceiveTransferPage(srn)
                  )
                )
          )
          .flatMap(
            ua =>
              ua.transfersOutSectionCompleted(srn, index)
                .foldLeft(Try(ua))((acc, secondaryIndex) => acc.remove(transfersOutPages(secondaryIndex)))
                .when(_.get(SchemeTransferOutPage(srn)))(
                  ifTrue = _.removeWhen(!TransfersOutSectionCompleted.exists(srn, _))(
                    TransfersOutJourneyStatus(srn),
                    SchemeTransferOutPage(srn)
                  ),
                  ifFalse = _.removeWhen(_.membersDetails(srn).isEmpty)(
                    TransfersOutJourneyStatus(srn),
                    SchemeTransferOutPage(srn)
                  )
                )
          )
          .flatMap { ua =>
            ua.remove(surrenderedBenefitsPages)
              .when(_.get(SurrenderedBenefitsPage(srn)))(
                ifTrue = _.removeWhen(_.surrenderedBenefitsCompleted(srn).isEmpty)(
                  SurrenderedBenefitsJourneyStatus(srn),
                  SurrenderedBenefitsPage(srn)
                ),
                ifFalse = _.removeWhen(_.membersDetails(srn).isEmpty)(
                  SurrenderedBenefitsJourneyStatus(srn),
                  SurrenderedBenefitsPage(srn)
                )
              )
          }
          .flatMap { ua =>
            ua.remove(memberCommencementLumpSumPages)
              .when(_.get(PensionCommencementLumpSumPage(srn)))(
                ifTrue = _.removeWhen(_.map(PensionCommencementLumpSumAmountPage.all(srn)).isEmpty)(
                  PensionCommencementLumpSumPage(srn),
                  PclsMemberListPage(srn)
                ),
                ifFalse = _.removeWhen(_.membersDetails(srn).isEmpty)(
                  PensionCommencementLumpSumPage(srn),
                  PclsMemberListPage(srn)
                )
              )
          }
          .flatMap { ua =>
            ua.remove(memberPensionPaymentsPages)
              .when(_.get(PensionPaymentsReceivedPage(srn)))(
                ifTrue = _.removeWhen(_.map(TotalAmountPensionPaymentsPage.all(srn)).isEmpty)(
                  MemberPensionPaymentsListPage(srn),
                  PensionPaymentsReceivedPage(srn)
                ),
                ifFalse = _.removeWhen(_.membersDetails(srn).isEmpty)(
                  MemberPensionPaymentsListPage(srn),
                  PensionPaymentsReceivedPage(srn)
                )
              )
          }
          .flatMap { ua =>
            ua.remove(memberContributionsPages)
              .when(_.get(MemberContributionsPage(srn)))(
                ifTrue = _.removeWhen(_.map(AllTotalMemberContributionPages(srn)).isEmpty)(
                  MemberContributionsPage(srn),
                  MemberContributionsListPage(srn)
                ),
                ifFalse = _.removeWhen(_.membersDetails(srn).isEmpty)(
                  MemberContributionsPage(srn),
                  MemberContributionsListPage(srn)
                )
              )
          }
          .flatMap {
            _.removeWhen(_.membersDetails(srn).isEmpty)(
              UnallocatedEmployerContributionsPage(srn),
              UnallocatedEmployerAmountPage(srn)
            )
          }
          .remove(MemberStatus(srn, index))
    )
  }
}
