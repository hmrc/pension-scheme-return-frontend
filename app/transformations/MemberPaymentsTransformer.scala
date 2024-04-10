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
import config.Refined.{Max300, Max50}
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer.TransfersInJourneyStatus
import pages.nonsipp.memberpensionpayments._
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsJourneyStatus, SurrenderedBenefitsPage}
import models._
import cats.syntax.traverse._
import pages.nonsipp.employercontributions._
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
import models.requests.psr._
import models.UserAnswers.implicits._
import pages.nonsipp.membertransferout.TransfersOutJourneyStatus
import pages.nonsipp.memberdetails.MembersDetailsPages._
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import viewmodels.models._

import scala.util.Try

import javax.inject.Inject

@Singleton()
class MemberPaymentsTransformer @Inject()(
  transfersInTransformer: TransfersInTransformer,
  transfersOutTransformer: TransfersOutTransformer,
  pensionSurrenderTransformer: PensionSurrenderTransformer
) extends Transformer {

  private val noUpdate: List[Try[UserAnswers] => Try[UserAnswers]] = Nil

  def transformToEtmp(srn: Srn, userAnswers: UserAnswers): Option[MemberPayments] = {

    val refinedMemberDetails: List[(Max300, NameDOB)] = userAnswers
      .membersDetails(srn)
      .zipWithIndex
      .flatMap {
        case (details, index) =>
          refineIndex[Max300.Refined](index).map(_ -> details).toList
      }

    val memberDetails: List[MemberDetails] = refinedMemberDetails.flatMap {
      case (index, memberDetails) =>
        for {
          employerContributions <- buildEmployerContributions(srn, index, userAnswers)
          transfersIn <- transfersInTransformer.transformToEtmp(srn, index, userAnswers)
          transfersOut <- transfersOutTransformer.transformToEtmp(srn, index, userAnswers)
          benefitsSurrendered = pensionSurrenderTransformer.transformToEtmp(srn, index, userAnswers)
        } yield MemberDetails(
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

    val benefitsSurrenderedDetails: UserAnswers => SectionDetails = ua =>
      SectionDetails(
        made = ua.get(SurrenderedBenefitsPage(srn)).getOrElse(false),
        completed = ua.get(SurrenderedBenefitsJourneyStatus(srn)).exists {
          case SectionStatus.InProgress => false
          case SectionStatus.Completed => true
        }
      )

    memberDetails match {
      case Nil => None
      case list =>
        Some(
          MemberPayments(
            memberDetails = list,
            employerContributionsDetails = SectionDetails(
              made = userAnswers.get(EmployerContributionsPage(srn)).getOrElse(false),
              completed = userAnswers.get(EmployerContributionsSectionStatus(srn)).exists {
                case SectionStatus.InProgress => false
                case SectionStatus.Completed => true
              }
            ),
            transfersInCompleted = userAnswers.get(TransfersInJourneyStatus(srn)).exists {
              case SectionStatus.InProgress => false
              case SectionStatus.Completed => true
            },
            transfersOutCompleted = userAnswers.get(TransfersOutJourneyStatus(srn)).exists {
              case SectionStatus.InProgress => false
              case SectionStatus.Completed => true
            },
            unallocatedContribsMade = userAnswers.get(UnallocatedEmployerContributionsPage(srn)).getOrElse(false),
            unallocatedContribAmount = userAnswers.get(UnallocatedEmployerAmountPage(srn)).map(_.value),
            memberContributionMade = userAnswers.get(MemberContributionsPage(srn)).getOrElse(false),
            lumpSumReceived = userAnswers.get(PensionCommencementLumpSumPage(srn)).getOrElse(false),
            pensionReceived = userAnswers.get(PensionPaymentsReceivedPage(srn)).getOrElse(false),
            benefitsSurrenderedDetails = benefitsSurrenderedDetails(userAnswers)
          )
        )
    }
  }

  def transformFromEtmp(userAnswers: UserAnswers, srn: Srn, memberPayments: MemberPayments): Try[UserAnswers] =
    for {
      ua1 <- userAnswers.set(UnallocatedEmployerContributionsPage(srn), memberPayments.unallocatedContribsMade)
      ua2 <- memberPayments.unallocatedContribAmount match {
        case Some(value) => ua1.set(UnallocatedEmployerAmountPage(srn), Money(value))
        case None => Try(ua1)
      }
      ua3 <- memberPayments.memberDetails.zipWithIndex
        .traverse { case (memberDetails, index) => refineIndex[Max300.Refined](index).map(memberDetails -> _) }
        .getOrElse(Nil)
        .foldLeft(Try(ua2)) {
          case (ua, (memberDetails, index)) =>
            val pages: List[Try[UserAnswers] => Try[UserAnswers]] =
              memberPersonalDetailsPages(srn, index, memberDetails.personalDetails) ++
                employerContributionsPages(
                  srn,
                  index,
                  memberPayments.employerContributionsDetails,
                  memberDetails.employerContributions
                ) ++ transfersInTransformer.transformFromEtmp(
                srn,
                index,
                memberDetails.transfersIn,
                memberPayments.transfersInCompleted
              ) ++ transfersOutTransformer.transformFromEtmp(
                srn,
                index,
                memberDetails.transfersOut,
                memberPayments.transfersInCompleted
              ) ++ memberContributionsPages(
                srn,
                index,
                memberPayments.memberContributionMade,
                memberDetails.totalContributions
              ) ++ memberLumpSumReceivedPages(
                srn,
                index,
                memberPayments.lumpSumReceived,
                memberDetails.memberLumpSumReceived
              ) ++ memberDetails.benefitsSurrendered.fold(noUpdate)(
                benefitsSurrendered =>
                  pensionSurrenderTransformer.transformFromEtmp(
                    srn,
                    index,
                    benefitsSurrendered,
                    memberPayments.benefitsSurrenderedDetails
                  )
              ) ++ memberDetails.pensionAmountReceived.fold(noUpdate)(
                pensionAmountReceivedPages(srn, index, _)
              )

            pages.foldLeft(ua)((userAnswers, f) => f(userAnswers))
        }
      emptyTotalContributionNotExist = !memberPayments.memberDetails.exists(_.totalContributions.isEmpty)
      ua4 <- ua3.set(MemberContributionsListPage(srn), emptyTotalContributionNotExist)

      emptyMemberLumpSumReceivedNotExist = !memberPayments.memberDetails.exists(_.memberLumpSumReceived.isEmpty)
      ua5 <- ua4.set(PclsMemberListPage(srn), emptyMemberLumpSumReceivedNotExist)

      ua6 <- pensionAmountReceivedStatus(memberPayments).fold(Try(ua5))(
        status =>
          ua5
            .set(PensionPaymentsJourneyStatus(srn), status)
            .set(MemberPensionPaymentsListPage(srn), if (status.isCompleted) true else false)
      )

      pensionPaymentsReceived = !memberPayments.memberDetails.forall(_.pensionAmountReceived.contains(0))

      ua7 <- ua6.set(PensionPaymentsReceivedPage(srn), pensionPaymentsReceived)
    } yield ua7

  private def buildMemberPersonalDetails(
    srn: Srn,
    index: Max300,
    nameDob: NameDOB,
    userAnswers: UserAnswers
  ): MemberPersonalDetails = {
    val maybeNino = userAnswers.get(MemberDetailsNinoPage(srn, index)).map(_.value)
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
      _.set(MemberStatus(srn, index), MemberState.Active)
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
    employerContributionsDetails: SectionDetails,
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
        ) ++ Option
          .when[UserAnswers.Compose](employerContributionsDetails.completed)(
            _.set(EmployerContributionsProgress(srn, index, secondaryIndex), SectionJourneyStatus.Completed)
          )
          .toList
    } ++ List[Try[UserAnswers] => Try[UserAnswers]](
      _.set(EmployerContributionsPage(srn), employerContributionsDetails.made),
      _.set(EmployerContributionsMemberListPage(srn), employerContributionsDetails.completed),
      _.set(
        EmployerContributionsSectionStatus(srn),
        if (employerContributionsDetails.completed) SectionStatus.Completed else SectionStatus.InProgress
      )
    )
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

  private def pensionAmountReceivedPages(
    srn: Srn,
    index: Max300,
    pensionReceived: Double
  ): List[Try[UserAnswers] => Try[UserAnswers]] =
    List(
      _.set(TotalAmountPensionPaymentsPage(srn, index), Money(pensionReceived))
    )

  private def pensionAmountReceivedStatus(memberPayments: MemberPayments): Option[SectionStatus] = {
    val pensionAmountReceived = memberPayments.memberDetails.map(_.pensionAmountReceived)

    if (pensionAmountReceived.nonEmpty) {
      if (pensionAmountReceived.exists(_.exists(_ == 0))) {
        Some(SectionStatus.InProgress)
      } else {
        Some(SectionStatus.Completed)
      }
    } else {
      None
    }
  }

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
