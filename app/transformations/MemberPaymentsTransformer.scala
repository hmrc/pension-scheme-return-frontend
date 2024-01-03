/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.syntax.traverse._
import com.google.inject.Singleton
import config.Refined.{Max300, Max50}
import models.SchemeId.Srn
import models.UserAnswers.implicits._
import models.requests.psr._
import models.{ConditionalYesNo, Crn, IdentityType, Money, NameDOB, UserAnswers, Utr}
import pages.nonsipp.employercontributions._
import pages.nonsipp.memberdetails.MembersDetailsPages._
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberpayments.{
  EmployerContributionsPage,
  UnallocatedEmployerAmountPage,
  UnallocatedEmployerContributionsPage
}
import pages.nonsipp.receivetransfer.TransfersInJourneyStatus
import uk.gov.hmrc.domain.Nino
import viewmodels.models.{MemberState, SectionCompleted, SectionStatus}

import javax.inject.Inject
import scala.util.Try

@Singleton()
class MemberPaymentsTransformer @Inject()(
  transfersInTransformer: TransfersInTransformer
) extends Transformer {

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
        } yield MemberDetails(
          personalDetails = buildMemberPersonalDetails(srn, index, memberDetails, userAnswers),
          employerContributions = employerContributions,
          transfersIn = transfersIn
        )
    }

    memberDetails match {
      case Nil => None
      case list =>
        Some(
          MemberPayments(
            memberDetails = list,
            employerContributionsCompleted = userAnswers.get(EmployerContributionsSectionStatus(srn)).exists {
              case SectionStatus.InProgress => false
              case SectionStatus.Completed => true
            },
            transfersInCompleted = userAnswers.get(TransfersInJourneyStatus(srn)).exists {
              case SectionStatus.InProgress => false
              case SectionStatus.Completed => true
            },
            memberContributionMade = false,
            unallocatedContribsMade = userAnswers.get(UnallocatedEmployerContributionsPage(srn)).getOrElse(false),
            unallocatedContribAmount = userAnswers.get(UnallocatedEmployerAmountPage(srn)) match {
              case Some(x) => Some(x.value)
              case None => None
            }
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
                  memberPayments.employerContributionsCompleted,
                  memberDetails.employerContributions
                ) ++ transfersInTransformer.transformFromEtmp(
                srn,
                index,
                memberDetails.transfersIn,
                memberPayments.transfersInCompleted
              )

            pages.foldLeft(ua)((userAnswers, f) => f(userAnswers))
        }
    } yield ua3

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
      keysToIndex[Max50.Refined](userAnswers.map(EmployerContributionsCompletedForMember(srn, index)))

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

  private def employerContributionsPages(
    srn: Srn,
    index: Max300,
    employerContributionsCompleted: Boolean,
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
        List[Try[UserAnswers] => Try[UserAnswers]](
          _.set(EmployerNamePage(srn, index, secondaryIndex), employerContributions.employerName),
          _.set(
            TotalEmployerContributionPage(srn, index, secondaryIndex),
            Money(employerContributions.totalTransferValue)
          ),
          _.set(EmployerContributionsCompleted(srn, index, secondaryIndex), SectionCompleted)
        ) ++ List[Try[UserAnswers] => Try[UserAnswers]](
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
        )
    } ++ List[Try[UserAnswers] => Try[UserAnswers]](
      _.set(EmployerContributionsPage(srn), employerContributionsList.nonEmpty),
      _.set(EmployerContributionsMemberListPage(srn), employerContributionsCompleted),
      _.set(
        EmployerContributionsSectionStatus(srn),
        if (employerContributionsCompleted) SectionStatus.Completed else SectionStatus.InProgress
      )
    )
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
