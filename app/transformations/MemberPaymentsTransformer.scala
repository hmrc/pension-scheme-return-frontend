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

import com.google.inject.Singleton
import pages.nonsipp.memberdetails.MembersDetailsPage._
import config.Refined.{Max300, Max50}
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments._
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsJourneyStatus, SurrenderedBenefitsPage}
import models._
import pages.nonsipp.membertransferout.{SchemeTransferOutPage, TransferOutMemberListPage, TransfersOutJourneyStatus}
import models.softdelete.SoftDeletedMember
import cats.syntax.traverse._
import pages.nonsipp.employercontributions._
import utils.Diff
import pages.nonsipp.memberdetails._
import pages.nonsipp.membercontributions.{
  MemberContributionsListPage,
  MemberContributionsPage,
  TotalMemberContributionPage
}
import pages.nonsipp.memberreceivedpcls.{
  PclsMemberListPage,
  PensionCommencementLumpSumAmountPage,
  PensionCommencementLumpSumPage
}
import pages.nonsipp.memberpensionpayments.Paths.membersPayments
import pages.nonsipp.receivetransfer.{
  DidSchemeReceiveTransferPage,
  TransferReceivedMemberListPage,
  TransfersInJourneyStatus
}
import models.requests.psr._
import models.UserAnswers.implicits._
import pages.nonsipp.{FbStatus, FbVersionPage}
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.memberpayments._
import config.Refined.Max300.Refined
import viewmodels.models._

import scala.util.{Success, Try}

import javax.inject.Inject

@Singleton()
class MemberPaymentsTransformer @Inject()(
  transfersInTransformer: TransfersInTransformer,
  transfersOutTransformer: TransfersOutTransformer,
  pensionSurrenderTransformer: PensionSurrenderTransformer,
  pensionAmountReceivedTransformer: PensionAmountReceivedTransformer
) extends Transformer {

  private val noUpdate: List[UserAnswers.Compose] = Nil

  private val logger = Logger(getClass)

  def transformToEtmp(
    srn: Srn,
    userAnswers: UserAnswers,
    initialUA: UserAnswers,
    previousVersionUA: Option[UserAnswers] = None
  ): Option[MemberPayments] = {

    for {
      currentMemberDetails <- buildMemberDetails(srn, userAnswers)
      initialMemberDetails <- buildMemberDetails(srn, initialUA)
      maybePreviousMemberDetails <- previousVersionUA.traverse(buildMemberDetails(srn, _))
    } yield {

      val softDeletedMembers: List[MemberDetails] = userAnswers.get(SoftDeletedMembers(srn)).toList.flatten.map {
        softDeletedMember =>
          MemberDetails(
            state = MemberState.Deleted,
            memberPSRVersion = softDeletedMember.memberPSRVersion,
            personalDetails = softDeletedMember.memberDetails,
            employerContributions = softDeletedMember.employerContributions,
            transfersIn = softDeletedMember.transfersIn,
            transfersOut = softDeletedMember.transfersOut,
            totalContributions = softDeletedMember.totalMemberContribution.map(_.value),
            memberLumpSumReceived = softDeletedMember.memberLumpSumReceived,
            benefitsSurrendered = softDeletedMember.pensionSurrendered,
            pensionAmountReceived = softDeletedMember.totalAmountPensionPaymentsPage.map(_.value)
          )
      }

      val benefitsSurrenderedDetails: UserAnswers => SectionDetails = ua =>
        SectionDetails(
          made = ua.get(SurrenderedBenefitsPage(srn)).getOrElse(false),
          completed = ua.get(SurrenderedBenefitsJourneyStatus(srn)).exists {
            case SectionStatus.InProgress => false
            case SectionStatus.Completed => true
          }
        )

      val psrState: Option[PSRStatus] = userAnswers.get(FbStatus(srn))

      /**
       * if member state is CHANGED, check if the status was set in this session by comparing to initial UserAnswers (ETMP GET snapshot)
       * if it was, check if there is a previous version of the member. If previous member state exists:
       * - stay CHANGED
       * - else change to NEW as the member has not been part of a declaration yet
       */
      val memberDetailsWithCorrectState = currentMemberDetails.map {
        _ -> psrState -> maybePreviousMemberDetails match {
          // new member created and changed while in interim Submitted state
          case (((index, currentMemberDetail), Some(Submitted)), None)
              if currentMemberDetail.state.changed && currentMemberDetail.memberPSRVersion.isEmpty =>
            logger.info(
              s"PSR is Submitted - Member $index state is Changed but no member PSR version and no previous version - newly added member, setting to New"
            )
            (index, currentMemberDetail.copy(state = MemberState.New))
          // member created before first submission and changed before first POST since first submission (interim Submitted state)
          case (((index, currentMemberDetail), Some(Submitted)), None) if currentMemberDetail.state.changed =>
            logger.info(
              s"PSR is Submitted - Member $index state is Changed but has a member PSR version and no previous version - existing member, leaving status as Changed"
            )
            (index, currentMemberDetail)
          case (((index, currentMemberDetail), _), _)
              if currentMemberDetail.state.changed && initialMemberDetails.get(index).exists(_.state.changed) =>
            logger.info(
              s"Member $index state is Changed but initial UA shows the same member as Changed - leaving status as Changed"
            )
            (index, currentMemberDetail)
          case (((index, currentMemberDetail), _), Some(previousMemberDetails))
              if currentMemberDetail.state.changed && previousMemberDetails.contains(index) =>
            logger.info(
              s"Member $index state is Changed and previous version member details shows the same member exists - leaving status as Changed"
            )
            (index, currentMemberDetail)
          case (((index, currentMemberDetail), Some(Compiled)), Some(previousMemberDetails))
              if currentMemberDetail.state.changed && previousMemberDetails.contains(index) =>
            logger.info(
              s"PSR is Compiled - Member $index state is Changed and previous version member details shows the same member exists - leaving status as Changed"
            )
            (index, currentMemberDetail)
          case (((index, currentMemberDetail), state), previousMemberDetails) =>
            logger.info(
              s"Member $index state has not matched any of the previous statements. member state (${currentMemberDetail.state}), PSR state ($state), previous version member details exists (${previousMemberDetails.nonEmpty}) - setting state to New"
            )
            (index, currentMemberDetail.copy(state = MemberState.New))
        }
      }

      val normalise = (memberDetails: MemberDetails) => {
        memberDetails.copy(
          memberLumpSumReceived = if (memberDetails.memberLumpSumReceived.exists(_.isZero)) {
            None
          } else {
            memberDetails.memberLumpSumReceived
          },
          pensionAmountReceived =
            if (memberDetails.pensionAmountReceived.contains(0.0)) None
            else memberDetails.pensionAmountReceived,
          totalContributions =
            if (memberDetails.totalContributions.contains(0.0)) None
            else memberDetails.totalContributions
        )
      }

      // Omit memberPSRVersion if member has changed
      // Check for empty member payment sections before comparing members
      // (this is because we send empty records in certain sections for members)
      //
      // Note: This will be changing soon when we re-design statuses
      val memberDetailsWithCorrectVersion: List[MemberDetails] = memberDetailsWithCorrectState.map {
        case (index, currentMemberDetail) =>
          initialMemberDetails.get(index) match {
            case None =>
              currentMemberDetail.copy(memberPSRVersion = None)
            case Some(initialMemberDetail) =>
              val normalisedInitialMember = normalise(initialMemberDetail)
              val normalisedCurrentMember = normalise(currentMemberDetail)
              val same = normalisedInitialMember == normalisedCurrentMember
              if (!same) {
                logger.info(s"member $index has changed, removing memberPSRVersion")
                if (logger.isDebugEnabled) {
                  logger.debug(Diff(normalisedInitialMember, normalisedCurrentMember).mkString(" - "))
                }
              }
              currentMemberDetail.copy(
                memberPSRVersion = if (same) currentMemberDetail.memberPSRVersion else None
              )
          }
      }.toList

      (memberDetailsWithCorrectVersion, softDeletedMembers) match {
        case (Nil, Nil) => None
        case _ =>
          val sameMemberPayments: Boolean = userAnswers.sameAs(initialUA, membersPayments, Omitted.membersPayments: _*)
          if (!sameMemberPayments) {
            logger.info(s"member payments has changed, removing recordVersion")
          }
          Some(
            MemberPayments(
              recordVersion = Option.when(sameMemberPayments)(
                userAnswers.get(MemberPaymentsRecordVersionPage(srn)).get
              ),
              memberDetails = memberDetailsWithCorrectVersion ++ softDeletedMembers,
              employerContributionsDetails = SectionDetails(
                made = userAnswers.get(EmployerContributionsPage(srn)).getOrElse(false),
                completed = userAnswers.get(EmployerContributionsSectionStatus(srn)).exists {
                  case SectionStatus.InProgress => false
                  case SectionStatus.Completed => true
                }
              ),
              transfersInMade = userAnswers.get(DidSchemeReceiveTransferPage(srn)),
              transfersOutMade = userAnswers.get(SchemeTransferOutPage(srn)),
              unallocatedContribsMade = userAnswers.get(UnallocatedEmployerContributionsPage(srn)),
              unallocatedContribAmount = userAnswers.get(UnallocatedEmployerAmountPage(srn)).map(_.value),
              memberContributionMade = userAnswers.get(MemberContributionsPage(srn)),
              lumpSumReceived = userAnswers.get(PensionCommencementLumpSumPage(srn)),
              pensionReceived = pensionAmountReceivedTransformer.transformToEtmp(srn, userAnswers),
              benefitsSurrenderedDetails = benefitsSurrenderedDetails(userAnswers)
            )
          )
      }
    }
  }.left
    .map(logger.error(_))
    .toOption
    .flatten // todo change return type to accept Failure type (e.g. Either / Try), logging for the time being

  private def buildMemberDetails(srn: Srn, userAnswers: UserAnswers): Either[String, Map[Max300, MemberDetails]] = {

    val refinedMemberDetails: Map[Max300, NameDOB] = userAnswers
      .membersDetails(srn)
      .flatMap { case (index, value) => refineIndex[Refined](index).map(_ -> value) }

    refinedMemberDetails.toList
      .traverse {
        case (index, memberDetails) =>
          val employerContributions = buildEmployerContributions(srn, index, userAnswers).getOrElse(Nil)
          val benefitsSurrendered = pensionSurrenderTransformer.transformToEtmp(srn, index, userAnswers)
          val transfersIn = transfersInTransformer.transformToEtmp(srn, index, userAnswers).getOrElse(Nil)
          val transfersOut = transfersOutTransformer.transformToEtmp(srn, index, userAnswers).getOrElse(Nil)
          for {
            memberState <- userAnswers
              .get(MemberStatus(srn, index))
              .toRight(s"MemberStatus not found for member $index")
          } yield {
            index -> MemberDetails(
              state = memberState,
              memberPSRVersion = userAnswers.get(MemberPsrVersionPage(srn, index)),
              personalDetails = buildMemberPersonalDetails(srn, index, memberDetails, userAnswers),
              employerContributions = employerContributions,
              transfersIn = transfersIn,
              totalContributions = userAnswers.get(TotalMemberContributionPage(srn, index)).map(_.value),
              memberLumpSumReceived = buildMemberLumpSumReceived(srn, index, userAnswers),
              transfersOut = transfersOut,
              benefitsSurrendered = benefitsSurrendered,
              pensionAmountReceived = userAnswers.get(TotalAmountPensionPaymentsPage(srn, index)).map(_.value)
            )
          }
      }
      .map(_.toMap)
  }

  def transformFromEtmp(
    userAnswers: UserAnswers,
    previousVersionUA: Option[UserAnswers],
    srn: Srn,
    memberPayments: MemberPayments,
    fetchingPreviousVersion: Boolean = false
  ): Try[UserAnswers] =
    for {
      ua0 <- memberPayments.recordVersion.fold(Try(userAnswers))(
        userAnswers.set(MemberPaymentsRecordVersionPage(srn), _)
      )
      // Only set UnallocatedEmployerContributionsPage when unallocatedContribsMade exists
      // If empty, this infers that the section hasn't been started yet
      ua1 <- memberPayments.unallocatedContribsMade.fold(Try(ua0))(
        unallocatedContribsMade =>
          ua0.set(
            UnallocatedEmployerContributionsPage(srn),
            unallocatedContribsMade
          )
      )
      ua2 <- memberPayments.unallocatedContribAmount match {
        case Some(value) => ua1.set(UnallocatedEmployerAmountPage(srn), Money(value))
        case None => Try(ua1)
      }
      ua3 <- memberPayments.memberDetails
        .filter(_.state.active)
        .zipWithIndex
        .traverse { case (memberDetails, index) => refineIndex[Max300.Refined](index).map(memberDetails -> _) }
        .getOrElse(Nil)
        .foldLeft(Try(ua2)) {
          case (ua, (memberDetails, index)) =>
            val pages: List[UserAnswers.Compose] =
              memberPersonalDetailsPages(srn, index, memberDetails.personalDetails) ++
                employerContributionsPages(
                  srn,
                  index,
                  memberDetails.employerContributions
                ) ++ transfersInTransformer.transformFromEtmp(
                srn,
                index,
                memberDetails.transfersIn
              ) ++ transfersOutTransformer.transformFromEtmp(
                srn,
                index,
                memberDetails.transfersOut
              ) ++ memberPayments.memberContributionMade.fold(noUpdate)(
                memberContributionsPages(srn, index, _, memberDetails.totalContributions)
              ) ++ memberPayments.lumpSumReceived.fold(noUpdate)(
                memberLumpSumReceivedPages(srn, index, _, memberDetails.memberLumpSumReceived)
              ) ++ memberDetails.benefitsSurrendered.fold(noUpdate)(
                pensionSurrenderTransformer.transformFromEtmp(srn, index, _)
              ) ++ memberDetails.pensionAmountReceived.fold(noUpdate)(
                pensionAmountReceivedTransformer.transformFromEtmp(srn, index, _)
              ) :+ (
                (triedUA: Try[UserAnswers]) =>
                  memberDetails.memberPSRVersion.fold(triedUA)(triedUA.set(MemberPsrVersionPage(srn, index), _))
                ) :+ (
                triedUA =>
                  triedUA.set(
                    MemberStatus(srn, index),
                    memberDetails.state
                  )
                )

            pages.foldLeft(ua)((userAnswers, f) => f(userAnswers))
        }

      // Section wide user answers (this includes initial pages, completion pages, section status pages and pages that don't require an index)
      ua3_1 = ua3.compose(
        pensionSurrenderTransformer.transformFromEtmp(srn, memberPayments.benefitsSurrenderedDetails) ++
          pensionAmountReceivedTransformer.transformFromEtmp(
            srn,
            memberPayments.pensionReceived
          )
      )

      employerContributionsNotStarted = (
        !memberPayments.employerContributionsDetails.made
          && memberPayments.memberDetails.forall(_.employerContributions.isEmpty)
          && !memberPayments.employerContributionsDetails.completed
      )

      ua3_2 <- employerContributionsNotStarted match {
        case true => ua3_1
        case false =>
          ua3_1
            .set(
              EmployerContributionsPage(srn),
              // If 1 or more Employer Contributions have been made, then this answer must be set to true / Yes, even if
              // the value in ETMP is false - this is used as a workaround to indicate the section is In Progress.
              if (memberPayments.memberDetails.exists(_.employerContributions.nonEmpty)) true
              else memberPayments.employerContributionsDetails.made
            )
            .set(
              EmployerContributionsSectionStatus(srn),
              if (memberPayments.employerContributionsDetails.completed) SectionStatus.Completed
              else SectionStatus.InProgress
            )
            .set(
              EmployerContributionsMemberListPage(srn),
              memberPayments.employerContributionsDetails.completed
            )
      }

      // Transfers In section-wide user answers
      ua3_3 <- memberPayments.transfersInMade match {
        case Some(true) =>
          ua3_2
            .set(DidSchemeReceiveTransferPage(srn), true)
            .set(TransferReceivedMemberListPage(srn), true) // temporary E2E workaround
            .set(TransfersInJourneyStatus(srn), SectionStatus.Completed) // temporary E2E workaround
        case Some(false) =>
          ua3_2
            .set(DidSchemeReceiveTransferPage(srn), false)
            .set(TransferReceivedMemberListPage(srn), true) // temporary E2E workaround
            .set(TransfersInJourneyStatus(srn), SectionStatus.Completed) // temporary E2E workaround
        case None => Try(ua3_2)
      }

      // Transfers Out section-wide user answers
      ua3_4 <- memberPayments.transfersOutMade match {
        case Some(true) =>
          ua3_3
            .set(SchemeTransferOutPage(srn), true)
            .set(TransferOutMemberListPage(srn), true) // temporary E2E workaround
            .set(TransfersOutJourneyStatus(srn), SectionStatus.Completed) // temporary E2E workaround
        case Some(false) =>
          ua3_3
            .set(SchemeTransferOutPage(srn), false)
            .set(TransferOutMemberListPage(srn), true) // temporary E2E workaround
            .set(TransfersOutJourneyStatus(srn), SectionStatus.Completed) // temporary E2E workaround
        case None => Try(ua3_3)
      }

      // temporary E2E workaround
      memberTotalContributionExists = memberPayments.memberDetails.exists(_.totalContributions.nonEmpty)
      ua4 <- ua3_4.set(MemberContributionsListPage(srn), memberTotalContributionExists)

      // temporary E2E workaround
      memberLumpSumReceivedExists = memberPayments.memberDetails.exists(_.memberLumpSumReceived.nonEmpty)
      ua5 <- ua4.set(PclsMemberListPage(srn), memberLumpSumReceivedExists)

      // new members can be safely hard deleted - don't run when fetching previous user answers as there is no point
      newMembers <- if (!fetchingPreviousVersion) {
        identifyNewMembers(srn, ua5, previousVersionUA)
      } else {
        Success(Nil)
      }

      ua5_1 <- newMembers.foldLeft(Try(ua5)) {
        case (ua, index) =>
          logger.info(s"New member identified at index $index")
          ua.set(SafeToHardDelete(srn, index))
      }

      ua8 <- ua5_1.set(
        SoftDeletedMembers(srn),
        memberPayments.memberDetails
          .filter(_.state == MemberState.Deleted)
          .map(
            memberDetails =>
              SoftDeletedMember(
                memberPSRVersion = memberDetails.memberPSRVersion,
                memberDetails = memberDetails.personalDetails,
                employerContributions = memberDetails.employerContributions,
                transfersIn = memberDetails.transfersIn,
                transfersOut = memberDetails.transfersOut,
                pensionSurrendered = memberDetails.benefitsSurrendered,
                memberLumpSumReceived = memberDetails.memberLumpSumReceived,
                totalMemberContribution = memberDetails.totalContributions.map(Money(_)),
                totalAmountPensionPaymentsPage = memberDetails.pensionAmountReceived.map(Money(_))
              )
          )
      )
    } yield ua8

  /**
   * Checks UserAnswers to see if any members are newly added (have never been through a declaration or have been newly added in this version prior to a declaration)
   * - if member has no version, this is before the first submission and they are all new.
   * - if member has a version, check previous user answers:
   *   - if no previous user answers, check if the member psr version is the same as the fb version:
   *     - if the same, its been added in the latest version so it's new.
   *     - if they are different, they are not new members.
   *   - if there are previous user answers, compare members:
   *    - if they are the same, they are not new members.
   *    - if they are different, check if they were added in the latest version of the return (memberStatus is NEW, psrStatus is Compiled and return’s fbVersion == memberPSRVersion)
   *      - if so, they are changed members.
   *      - if not, they are new members.
   *
   * fetchingPreviousVersion - used to indicate that the PSR retrieval service is calling this method when fetching the previous version
   *                         - this is required as when this happens, there won't be a previousVersionUA yet
   *                         - we can't rely on the absence of previousVersionUA to determine this as the PSR connector can return None if not found and in some cases it is required to be there
   */
  private def identifyNewMembers(
    srn: Srn,
    ua: UserAnswers,
    previousVersionUA: Option[UserAnswers]
  ): Try[List[Max300]] =
    buildMemberDetails(srn, ua).left.map(new Exception(_)).toTry.flatMap { currentMemberDetails =>
      val psrStatus = ua.get(FbStatus(srn))
      val psrVersion = ua.get(FbVersionPage(srn))
      logger.info(
        s"""[identifyNewMembers] identifying members that are safe to hard delete with PSR version $psrVersion, psrStatus ${psrStatus} and previous version useranswers are ${previousVersionUA
          .fold("empty")(_ => "non-empty")}"""
      )
      previousVersionUA match {
        case Some(previousUA) =>
          logger.info(s"[identifyNewMembers] Previous PSR version found for srn $srn")
          buildMemberDetails(srn, previousUA).left.map(new Exception(_)).toTry.flatMap { previousMemberDetails =>
            Success(
              currentMemberDetails.toList.flatMap {
                case (index, currentMemberDetail) if previousMemberDetails.get(index).contains(currentMemberDetail) =>
                  None
                case (_, currentMemberDetail) if currentMemberDetail.state.changed => None
                case (index, currentMemberDetail) if currentMemberDetail.state._new && psrStatus.exists(_.isCompiled) =>
                  Some(index)
                case _ => None
              }
            )
          }
        // No previous version UserAnswers so this must be either pre-first submission or just after the first submission
        case None =>
          logger.info(s"[identifyNewMembers] Previous PSR version NOT found for srn $srn")
          val (membersWithNoVersions, membersWithVersions): (List[Max300], List[(Max300, String)]) =
            currentMemberDetails.toList.partitionMap {
              case (index, memberDetails) if memberDetails.memberPSRVersion.isEmpty => Left(index)
              case (index, MemberDetails(_, Some(version), _, _, _, _, _, _, _, _)) => Right(index -> version)
            }

          val versionedMembersWithETMPStatus = membersWithVersions.map {
            case (index, memberVersion) => (index, memberVersion, ua.get(MemberStatus(srn, index)))
          }

          Success(
            versionedMembersWithETMPStatus.flatMap {
              // Added in current version of the submission but return has just been submitted
              case (index, memberVersion, Some(MemberState.New))
                  if ua.get(FbVersionPage(srn)).contains(memberVersion) &&
                    ua.get(FbStatus(srn)).exists(_.isSubmitted) =>
                logger.info(
                  s"[identifyNewMembers] member at index $index is part of a freshly made submission. Not safe to hard delete"
                )
                None
              // Added in current version of the submission, safe to soft delete
              case (index, memberVersion, Some(MemberState.New | MemberState.Changed))
                  if ua.get(FbVersionPage(srn)).contains(memberVersion) && psrStatus.exists(_.isCompiled) =>
                logger.info(
                  s"[identifyNewMembers] member at index $index was added in the current version of the submission without a previous version. safe to hard delete"
                )
                Some(index)
              case _ => None
              // members with no versions have never been part of an ETMP POST so safe to hard delete
            } ++ membersWithNoVersions
          )
      }
    }

  private def buildMemberPersonalDetails(
    srn: Srn,
    index: Max300,
    nameDob: NameDOB,
    userAnswers: UserAnswers
  ): MemberPersonalDetails = {
    val maybeNino = userAnswers.get(MemberDetailsNinoPage(srn, index)).map(_.value.filterNot(_.isWhitespace))
    val maybeNoNinoReason = userAnswers.get(NoNINOPage(srn, index))

    MemberPersonalDetails(
      firstName = nameDob.firstName,
      lastName = nameDob.lastName,
      nino = maybeNino,
      reasonNoNINO = maybeNoNinoReason,
      dateOfBirth = nameDob.dob
    )
  }

  private def memberPersonalDetailsPages(
    srn: Srn,
    index: Max300,
    personalDetails: MemberPersonalDetails
  ): List[Try[UserAnswers] => Try[UserAnswers]] =
    List(
      _.set(
        MemberDetailsPage(srn, index),
        NameDOB(
          personalDetails.firstName,
          personalDetails.lastName,
          personalDetails.dateOfBirth
        )
      ),
      _.set(DoesMemberHaveNinoPage(srn, index), personalDetails.nino.nonEmpty),
      ua =>
        personalDetails.reasonNoNINO
          .map(noNinoReason => ua.set(NoNINOPage(srn, index), noNinoReason))
          .getOrElse(ua),
      ua =>
        personalDetails.nino
          .map(nino => ua.set(MemberDetailsNinoPage(srn, index), Nino(nino)))
          .getOrElse(ua),
      _.set(MemberDetailsCompletedPage(srn, index), SectionCompleted)
    )

  private def buildEmployerContributions(
    srn: Srn,
    index: Max300,
    userAnswers: UserAnswers
  ): Option[List[EmployerContributions]] = {
    val secondaryIndexes =
      keysToIndex[Max50.Refined](userAnswers.map(EmployerContributionsCompleted.all(srn, index)))

    secondaryIndexes.traverse(
      secondaryIndex =>
        for {
          employerName <- userAnswers.get(EmployerNamePage(srn, index, secondaryIndex))
          identityType <- userAnswers.get(EmployerTypeOfBusinessPage(srn, index, secondaryIndex))
          employerType <- toEmployerType(srn, identityType, index, secondaryIndex, userAnswers)
          total <- userAnswers.get(TotalEmployerContributionPage(srn, index, secondaryIndex))
        } yield EmployerContributions(
          employerName,
          employerType,
          totalTransferValue = total.value
        )
    )
  }

  private def buildMemberLumpSumReceived(
    srn: Srn,
    index: Max300,
    userAnswers: UserAnswers
  ): Option[MemberLumpSumReceived] =
    for {
      pensionCommencementLumpSum <- userAnswers.get(PensionCommencementLumpSumAmountPage(srn, index))
    } yield MemberLumpSumReceived(
      pensionCommencementLumpSum.lumpSumAmount.value,
      pensionCommencementLumpSum.designatedPensionAmount.value
    )

  private def employerContributionsPages(
    srn: Srn,
    index: Max300,
    employerContributionsList: List[EmployerContributions]
  ): List[Try[UserAnswers] => Try[UserAnswers]] = {
    val secondaryIndexes: List[(Max50, EmployerContributions)] = employerContributionsList.zipWithIndex
      .traverse {
        case (employerContributions, index) =>
          refineIndex[Max50.Refined](index).map(_ -> employerContributions)
      }
      .toList
      .flatten

    secondaryIndexes.flatMap {
      case (secondaryIndex, employerContributions) =>
        List[UserAnswers.Compose](
          _.set(EmployerNamePage(srn, index, secondaryIndex), employerContributions.employerName),
          _.set(
            TotalEmployerContributionPage(srn, index, secondaryIndex),
            Money(employerContributions.totalTransferValue)
          ),
          _.set(EmployerContributionsCompleted(srn, index, secondaryIndex), SectionCompleted)
        ) ++ List[UserAnswers.Compose](
          employerContributions.employerType match {
            case EmployerType.UKCompany(idOrReason) =>
              _.set(EmployerTypeOfBusinessPage(srn, index, secondaryIndex), IdentityType.UKCompany)
                .set(EmployerCompanyCrnPage(srn, index, secondaryIndex), ConditionalYesNo(idOrReason.map(Crn(_))))
            case EmployerType.UKPartnership(idOrReason) =>
              _.set(EmployerTypeOfBusinessPage(srn, index, secondaryIndex), IdentityType.UKPartnership)
                .set(PartnershipEmployerUtrPage(srn, index, secondaryIndex), ConditionalYesNo(idOrReason.map(Utr(_))))
            case EmployerType.Other(description) =>
              _.set(EmployerTypeOfBusinessPage(srn, index, secondaryIndex), IdentityType.Other)
                .set(OtherEmployeeDescriptionPage(srn, index, secondaryIndex), description)
          }
        ) ++ List[UserAnswers.Compose](
          _.set(EmployerContributionsProgress(srn, index, secondaryIndex), SectionJourneyStatus.Completed)
        )
    }
  }

  private def memberContributionsPages(
    srn: Srn,
    index: Max300,
    memberContributionMade: Boolean,
    memberTotalContributions: Option[Double]
  ): List[Try[UserAnswers] => Try[UserAnswers]] =
    List(
      ua =>
        memberTotalContributions
          .map(t => ua.set(TotalMemberContributionPage(srn, index), Money(t)))
          .getOrElse(ua),
      _.set(MemberContributionsPage(srn), memberContributionMade)
    )

  private def memberLumpSumReceivedPages(
    srn: Srn,
    index: Max300,
    lumpSumReceived: Boolean,
    memberLumpSumReceived: Option[MemberLumpSumReceived]
  ): List[Try[UserAnswers] => Try[UserAnswers]] =
    List(
      ua =>
        memberLumpSumReceived
          .map(t => {
            ua.set(
              PensionCommencementLumpSumAmountPage(srn, index),
              PensionCommencementLumpSum(Money(t.lumpSumAmount), Money(t.designatedPensionAmount))
            )
          })
          .getOrElse(ua),
      _.set(PensionCommencementLumpSumPage(srn), lumpSumReceived)
    )

  private def toEmployerType(
    srn: Srn,
    identityType: IdentityType,
    index: Max300,
    secondaryIndex: Max50,
    userAnswers: UserAnswers
  ): Option[EmployerType] =
    identityType match {
      case IdentityType.Individual => None
      case IdentityType.UKCompany =>
        userAnswers
          .get(EmployerCompanyCrnPage(srn, index, secondaryIndex))
          .map(v => EmployerType.UKCompany(v.value.map(_.value)))
      case IdentityType.UKPartnership =>
        userAnswers
          .get(PartnershipEmployerUtrPage(srn, index, secondaryIndex))
          .map(v => EmployerType.UKPartnership(v.value.map(_.value)))
      case IdentityType.Other =>
        userAnswers
          .get(OtherEmployeeDescriptionPage(srn, index, secondaryIndex))
          .map(EmployerType.Other)
    }
}
