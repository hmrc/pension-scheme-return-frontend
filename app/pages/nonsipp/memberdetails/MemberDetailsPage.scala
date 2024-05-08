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

package pages.nonsipp.memberdetails

import utils.RefinedUtils.RefinedIntOps
import pages.nonsipp.employercontributions.{Paths => _, _}
import play.api.mvc.Result
import models.SchemeId.Srn
import pages.nonsipp.receivetransfer.{Paths => _, _}
import play.api.libs.json.JsPath
import pages.nonsipp.membersurrenderedbenefits.{Paths => _, _}
import models.{NameDOB, UserAnswers}
import pages.nonsipp.membertransferout.{Paths => _, _}
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import play.api.mvc.Results.Redirect
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
import queries.{Gettable, Removable}
import config.Refined.{Max300, OneTo5, OneTo50}
import pages.QuestionPage
import pages.nonsipp.memberpensionpayments.{
  MemberPensionPaymentsListPage,
  PensionPaymentsReceivedPage,
  TotalAmountPensionPaymentsPage
}
import eu.timepit.refined.refineV

import scala.util.Try

case class MemberDetailsPage(srn: Srn, index: Max300) extends QuestionPage[NameDOB] {

  // won't work with a get all pages as Map as the arrayIndex must be a string
  override def path: JsPath = Paths.personalDetails \ toString \ index.arrayIndex.toString

  override def cleanup(value: Option[NameDOB], userAnswers: UserAnswers): Try[UserAnswers] =
    (value, userAnswers.get(this)) match {
      case (None, _) =>
        //deletion
        userAnswers.remove(pages(srn, userAnswers) ::: paymentsPages(srn, index, userAnswers))
      case _ => Try(userAnswers)
    }

  private def pages(srn: Srn, userAnswers: UserAnswers): List[Removable[_]] =
    List(
      DoesMemberHaveNinoPage(srn, index),
      MemberStatus(srn, index)
    )

  private def paymentsPages(srn: Srn, index: Max300, userAnswers: UserAnswers): List[Removable[_]] = {
    val memberMap = userAnswers.map(MembersDetailsPages(srn))
    val maxIndex: Either[Result, Int] = memberMap.keys
      .map(_.toInt)
      .maxOption
      .map(Right(_))
      .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

    val optionList: List[Option[NameDOB]] = maxIndex match {
      case Right(index) =>
        (0 to index).toList.map { index =>
          memberMap.get(index.toString)
        }
      case Left(_) => List.empty
    }

    val totalMembers = optionList.flatten.size

    val employerNamePageIndexes = userAnswers.get(EmployerNamePages(srn, index)) match {
      case Some(valueMap) =>
        valueMap.keys.map(_.toInt + 1).toList.map(refineV[OneTo50](_)).collect {
          case Right(refinedIndex) => refinedIndex
        }
      case None => List()
    }

    val transferSchemeNamePageIndexes = userAnswers.get(TransferringSchemeNamePages(srn, index)) match {
      case Some(valueMap) =>
        valueMap.keys.map(_.toInt + 1).toList.map(refineV[OneTo5](_)).collect {
          case Right(refinedIndex) => refinedIndex
        }
      case None => List()
    }

    val receivingSchemeNamePageIndexes = userAnswers.get(ReceivingSchemeNamePages(srn, index)) match {
      case Some(valueMap) =>
        valueMap.keys.map(_.toInt + 1).toList.map(refineV[OneTo5](_)).collect {
          case Right(refinedIndex) => refinedIndex
        }
      case None => List()
    }

    val employerContributionsPages = employerNamePageIndexes.map(
      secondaryIndex => EmployerNamePage(srn, index, secondaryIndex)
    ) ::: employerNamePageIndexes.map(secondaryIndex => EmployerContributionsProgress(srn, index, secondaryIndex))

    val memberContributionsPages = List(TotalMemberContributionPage(srn, index))

    val transfersInPages = transferSchemeNamePageIndexes.map(
      secondaryIndex => TransferringSchemeNamePage(srn, index, secondaryIndex)
    )

    val transfersOutPages = receivingSchemeNamePageIndexes.map(
      secondaryIndex => ReceivingSchemeNamePage(srn, index, secondaryIndex)
    )

    val pclsPages = List(PensionCommencementLumpSumAmountPage(srn, index))

    val pensionPaymentsPages = List(TotalAmountPensionPaymentsPage(srn, index))

    val surrenderedBenefitsPages = List(SurrenderedBenefitsAmountPage(srn, index))

    val finalRecordDeletionPages = List(
      PensionCommencementLumpSumPage(srn),
      PclsMemberListPage(srn),
      EmployerContributionsPage(srn),
      UnallocatedEmployerContributionsPage(srn),
      UnallocatedEmployerAmountPage(srn),
      MemberContributionsPage(srn),
      DidSchemeReceiveTransferPage(srn),
      SchemeTransferOutPage(srn),
      PensionPaymentsReceivedPage(srn),
      SurrenderedBenefitsPage(srn),
      EmployerContributionsSectionStatus(srn),
      MemberContributionsListPage(srn),
      TransfersInJourneyStatus(srn),
      TransfersOutJourneyStatus(srn),
      TransferOutMemberListPage(srn),
      MemberPensionPaymentsListPage(srn),
      SurrenderedBenefitsJourneyStatus(srn),
      SurrenderedBenefitsMemberListPage(srn)
    )

    if (totalMembers > 1) {
      employerContributionsPages ::: memberContributionsPages ::: transfersInPages :::
        transfersOutPages ::: pclsPages ::: pensionPaymentsPages ::: surrenderedBenefitsPages
    } else {
      employerContributionsPages ::: memberContributionsPages ::: transfersInPages :::
        transfersOutPages ::: pclsPages ::: pensionPaymentsPages ::: surrenderedBenefitsPages ::: finalRecordDeletionPages
    }

  }

  override def toString: String = "nameDob"
}

case class MembersDetailsPages(srn: Srn) extends Gettable[Map[String, NameDOB]] with Removable[Map[String, NameDOB]] {

  override def path: JsPath = Paths.personalDetails \ toString

  override def toString: String = "nameDob"
}

object MembersDetailsPages {
  implicit class MembersDetailsOps(ua: UserAnswers) {
    def membersDetails(srn: Srn): Map[String, NameDOB] = ua.map(MembersDetailsPages(srn))
  }
}
