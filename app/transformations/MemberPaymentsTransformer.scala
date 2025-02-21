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

import pages.nonsipp.memberdetails._
import com.google.inject.Singleton
import pages.nonsipp.memberdetails.MembersDetailsPage._
import config.RefinedTypes.{Max300, Max50}
import pages.nonsipp.memberpensionpayments._
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsPage
import models._
import pages.nonsipp.membertransferout.SchemeTransferOutPage
import models.softdelete.SoftDeletedMember
import cats.syntax.traverse._
import pages.nonsipp.employercontributions._
import pages.nonsipp.membercontributions.{MemberContributionsPage, TotalMemberContributionPage}
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import config.RefinedTypes.Max300.Refined
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments.Paths.membersPayments
import pages.nonsipp.receivetransfer.DidSchemeReceiveTransferPage
import models.requests.psr._
import models.UserAnswers.implicits._
import pages.nonsipp.{FbStatus, FbVersionPage}
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.memberpayments._
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
            prePopulated = None,
            state = MemberState.Deleted,
            memberPSRVersion = softDeletedMember.memberPSRVersion,
            personalDetails = softDeletedMember.memberDetails.copy(
              nino = softDeletedMember.memberDetails.nino.map(_.filterNot(_.isWhitespace))
            ),
            employerContributions = softDeletedMember.employerContributions,
            transfersIn = softDeletedMember.transfersIn,
            transfersOut = softDeletedMember.transfersOut,
            totalContributions = softDeletedMember.totalMemberContribution.map(_.value),
            memberLumpSumReceived = softDeletedMember.memberLumpSumReceived,
            benefitsSurrendered = softDeletedMember.pensionSurrendered,
            pensionAmountReceived = softDeletedMember.totalAmountPensionPaymentsPage.map(_.value)
          )
      }

      val psrState: Option[PSRStatus] = userAnswers.get(FbStatus(srn))

      /**
       * if member state is CHANGED, check if the status was set in this session by comparing to initial UserAnswers (ETMP GET snapshot)
       * if it was, check if there is a previous version of the member. If previous member state exists:
       * - stay CHANGED
       * - else change to NEW as the member has not been part of a declaration yet
       */
      val memberDetailsWithCorrectState: Map[Max300, MemberDetails] = currentMemberDetails.map {
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

      val normalise: MemberDetails => MemberDetails = (memberDetails: MemberDetails) => {
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
      val memberDetailsWithCorrectVersion: List[MemberDetails] = memberDetailsWithCorrectState.toSeq
        .sortBy(_._1.value)(Ordering[Int])
        .map {
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
//                // we can still use this on localhost if needed:
//                if (logger.isDebugEnabled) {
//                  logger.debug(Diff(normalisedInitialMember, normalisedCurrentMember).mkString(" - "))
//                }
                }
                currentMemberDetail.copy(
                  memberPSRVersion = if (same) currentMemberDetail.memberPSRVersion else None
                )
            }
        }
        .toList

      (memberDetailsWithCorrectVersion, softDeletedMembers) match {
        case (Nil, Nil) => None
        case _ =>
          val sameMemberPayments: Boolean = userAnswers.sameAs(initialUA, membersPayments, Omitted.membersPayments: _*)
          if (!sameMemberPayments) {
            logger.info(s"member payments has changed, removing recordVersion")
          }
          Some(
            MemberPayments(
              checked = userAnswers.get(MembersDetailsChecked(srn)),
              recordVersion = Option
                .when(sameMemberPayments)(
                  userAnswers.get(MemberPaymentsRecordVersionPage(srn))
                )
                .flatten,
              memberDetails = memberDetailsWithCorrectVersion ++ softDeletedMembers,
              employerContributionMade = userAnswers.get(EmployerContributionsPage(srn)),
              transfersInMade = userAnswers.get(DidSchemeReceiveTransferPage(srn)),
              transfersOutMade = userAnswers.get(SchemeTransferOutPage(srn)),
              unallocatedContribsMade = userAnswers.get(UnallocatedEmployerContributionsPage(srn)),
              unallocatedContribAmount = userAnswers.get(UnallocatedEmployerAmountPage(srn)).map(_.value),
              memberContributionMade = userAnswers.get(MemberContributionsPage(srn)),
              lumpSumReceived = userAnswers.get(PensionCommencementLumpSumPage(srn)),
              pensionReceived = userAnswers.get(PensionPaymentsReceivedPage(srn)),
              surrenderMade = userAnswers.get(SurrenderedBenefitsPage(srn))
            )
          )
      }
    }
  } match {
    case Left(error) => throw new RuntimeException(s"error occurred: $error")
    case Right(value) => value
  }

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
              prePopulated = None,
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

      // Section wide user answers (this includes initial pages that doesn't require an index)
      ua3_1 <- memberPayments.pensionReceived.fold(Try(ua3))(ua3.set(PensionPaymentsReceivedPage(srn), _))
      ua3_2 <- memberPayments.employerContributionMade.fold(Try(ua3_1))(ua3_1.set(EmployerContributionsPage(srn), _))
      ua3_3 <- memberPayments.transfersInMade.fold(Try(ua3_2))(ua3_2.set(DidSchemeReceiveTransferPage(srn), _))
      ua3_4 <- memberPayments.transfersOutMade.fold(Try(ua3_3))(ua3_3.set(SchemeTransferOutPage(srn), _))
      ua3_5 <- memberPayments.surrenderMade.fold(Try(ua3_4))(ua3_4.set(SurrenderedBenefitsPage(srn), _))
      ua3_6 <- memberPayments.checked.fold(Try(ua3_5))(ua3_4.set(MembersDetailsChecked(srn), _))

      // new members can be safely hard deleted - don't run when fetching previous user answers as there is no point
      newMembers <- if (!fetchingPreviousVersion) {
        identifyNewMembers(srn, ua3_6, previousVersionUA)
      } else {
        Success(Nil)
      }

      ua4 <- newMembers.foldLeft(Try(ua3_6)) {
        case (ua, index) =>
          logger.info(s"New member identified at index $index")
          ua.set(SafeToHardDelete(srn, index))
      }

      ua5 <- ua4.set(
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
    } yield ua5

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
   *      - if not, do the same check but check psrStatus is Submitted (memberStatus is NEW, psrStatus is Submitted and return’s fbVersion == memberPSRVersion)
   *        - if so, this means the member is changed
   *          (this is because psrStatus is only Submitted directly after a PSR declaration submission and the user has logged out / in before making another POST to ETMP)
   *        - if not, they are new members.
   *
   * fetchingPreviousVersion - used to indicate that the PSR retrieval service is calling this method when fetching the previous version
   *                         - this is required as when this happens, there won't be a previousVersionUA yet
   *                         - we can't rely on the absence of previousVersionUA to determine this as the PSR connector can return None if not found and in some cases it is required to be there
   *
   * Bug: We check members against the previous version by index, this is an issue because we keep members in index whether we delete them or not
   *      but when we POST members to ETMP, we group non-deleted members first and then append the deleted members to the end, changing the order
   *      we only do this because we need to save the indexes in our cache for active members as we use Refined types to constrain the number of members
   *      we have a ticket on the backlog to fix this (remove Refined type and just have an int)
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
        s"""[identifyNewMembers] identifying members that are safe to hard delete with PSR version $psrVersion, psrStatus $psrStatus and previous version useranswers are ${previousVersionUA
          .fold("empty")(_ => "non-empty")}"""
      )

      previousVersionUA match {
        case Some(previousUA) =>
          logger.info(s"[identifyNewMembers] Previous PSR version found for srn $srn")
          buildMemberDetails(srn, previousUA).left
            .map { err =>
              logger.warn(s"[identifyNewMembers] Building member details for srn $srn has failed - error: $err")
              new Exception(err)
            }
            .toTry
            .map { _ =>
              currentMemberDetails.toList.flatMap {
                case (index, currentMemberDetail) if currentMemberDetail.state.changed =>
                  logger.info(s"[identifyNewMembers] Member at index $index is changed - NOT safe to hard delete")
                  None
                case (index, currentMemberDetail)
                    if currentMemberDetail.state._new
                      && psrStatus.exists(_.isSubmitted)
                      && currentMemberDetail.memberPSRVersion.exists(psrVersion.contains) =>
                  logger.info(
                    s"[identifyNewMembers] Member at index $index is new but PSR status is Submitted - NOT safe to hard delete"
                  )
                  None
                case (index, currentMemberDetail)
                    if currentMemberDetail.state._new
                      && psrStatus.exists(_.isCompiled)
                      && currentMemberDetail.memberPSRVersion.exists(version => !psrVersion.contains(version)) =>
                  logger.info(
                    s"[identifyNewMembers] Member at index $index is new and PSR status is Compiled but member PSR version is NOT the same as record PSR version - NOT safe to hard delete"
                  )
                  None
                case (index, currentMemberDetail) if currentMemberDetail.state._new && psrStatus.exists(_.isCompiled) =>
                  logger.info(
                    s"[identifyNewMembers] Member at index $index is new and PSR status is Compiled. PSR status is $psrVersion and member PSR version is ${currentMemberDetail.memberPSRVersion} - safe to hard delete"
                  )
                  Some(index)
                case _ => None
              }
            }
        // No previous version UserAnswers so this must be either pre-first submission or just after the first submission
        case None =>
          logger.info(s"[identifyNewMembers] Previous PSR version NOT found for srn $srn")
          val (membersWithNoVersions, membersWithVersions): (List[Max300], List[(Max300, String)]) =
            currentMemberDetails.toList.partitionMap {
              case (index, memberDetails) if memberDetails.memberPSRVersion.isEmpty => Left(index)
              case (index, MemberDetails(None, _, Some(version), _, _, _, _, _, _, _, _)) => Right(index -> version)
            }

          val versionedMembersWithETMPStatus = membersWithVersions.map {
            case (index, memberVersion) => (index, memberVersion, ua.get(MemberStatus(srn, index)))
          }

          Success(
            versionedMembersWithETMPStatus.flatMap {
              // Added in current version of the submission but return has just been submitted, not safe to hard delete
              case (index, memberVersion, Some(MemberState.New))
                  if psrVersion.contains(memberVersion) && psrStatus.exists(_.isSubmitted) =>
                logger.info(
                  s"[identifyNewMembers] member at index $index is part of a freshly made submission. Not safe to hard delete"
                )
                None
              // Added in current version of the submission, safe to hard delete
              case (index, memberVersion, Some(MemberState.New | MemberState.Changed))
                  if psrVersion.contains(memberVersion) && psrStatus.exists(_.isCompiled) =>
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
