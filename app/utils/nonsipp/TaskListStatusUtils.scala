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

import pages.nonsipp.employercontributions.{EmployerContributionsPage, EmployerContributionsSectionStatus}
import pages.nonsipp.otherassetsdisposal.{
  OtherAssetsDisposalCompleted,
  OtherAssetsDisposalPage,
  OtherAssetsDisposalProgress
}
import models.ConditionalYesNo._
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld._
import config.Refined.OneTo5000
import models.SchemeId.Srn
import pages.nonsipp.landorproperty._
import pages.nonsipp.receivetransfer.{DidSchemeReceiveTransferPage, TransfersInJourneyStatus}
import pages.nonsipp.landorpropertydisposal.{
  HowWasPropertyDisposedOfPagesTaskListStatus,
  LandOrPropertyDisposalPage,
  LandPropertyDisposalCompletedPages
}
import pages.nonsipp.sharesdisposal._
import play.api.libs.json.{JsObject, JsPath}
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsCompleted, SurrenderedBenefitsPage}
import models._
import pages.nonsipp.loansmadeoroutstanding._
import pages.nonsipp.bonds._
import pages.nonsipp.memberdetails._
import pages.nonsipp.totalvaluequotedshares.{QuotedSharesManagedFundsHeldPage, TotalValueQuotedSharesPage}
import pages.nonsipp.membercontributions._
import pages.nonsipp.accountingperiod.Paths.accountingPeriodDetails
import pages.nonsipp.memberreceivedpcls.{PensionCommencementLumpSumAmountPage, PensionCommencementLumpSumPage}
import pages.nonsipp.memberpensionpayments.{PensionPaymentsReceivedPage, TotalAmountPensionPaymentsPage}
import eu.timepit.refined.refineV
import viewmodels.models.TaskListStatus._
import pages.nonsipp.schemedesignatory.Paths.schemeDesignatory
import pages.nonsipp.common.IdentityTypes
import pages.nonsipp.membertransferout.{SchemeTransferOutPage, TransfersOutSectionCompleted}
import pages.nonsipp.moneyborrowed.{LenderNamePages, MoneyBorrowedPage, WhySchemeBorrowedMoneyPages}
import pages.nonsipp.bondsdisposal.{BondsDisposalCompleted, BondsDisposalPage, BondsDisposalProgress}
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import viewmodels.models.{SectionCompleted, SectionStatus, TaskListStatus}

object TaskListStatusUtils {

  def getBasicDetailsTaskListStatus(
    checkReturnDates: Option[Boolean],
    accountingPeriods: Option[List[DateRange]],
    activeBankAccount: Option[Boolean],
    whyNoBankAccount: Option[String],
    howManyMembers: Option[SchemeMemberNumbers]
  ): TaskListStatus.TaskListStatus with Serializable =
    (checkReturnDates, accountingPeriods, activeBankAccount, whyNoBankAccount, howManyMembers) match {
      case (Some(true), None, Some(true), None, Some(_)) => Recorded
      case (Some(true), None, Some(false), Some(_), Some(_)) => Recorded
      case (Some(false), Some(dateRangeList), Some(true), None, Some(_)) if dateRangeList.nonEmpty => Recorded
      case (Some(false), Some(dateRangeList), Some(false), Some(_), Some(_)) if dateRangeList.nonEmpty => Recorded
      case (_, _, _, _, _) => InProgress
    }

  def getFinancialDetailsTaskListStatus(
    howMuchCash: Option[MoneyInPeriod],
    valueOfAssets: Option[MoneyInPeriod],
    feesCommissionsWagesSalaries: Option[Money]
  ): TaskListStatus =
    (howMuchCash, valueOfAssets, feesCommissionsWagesSalaries) match {
      case (None, None, None) => NotStarted
      case (Some(_), Some(_), Some(_)) => Recorded
      case (_, _, _) => InProgress
    }

  def getMembersTaskListStatus(userAnswers: UserAnswers, srn: Srn): TaskListStatus = {
    val membersDetailsPages = userAnswers.get(MembersDetailsPages(srn))
    val ninoPages = userAnswers.get(MemberDetailsNinoPages(srn))
    val noNinoPages = userAnswers.get(NoNinoPages(srn))
    (membersDetailsPages, ninoPages, noNinoPages) match {
      case (None, _, _) => NotStarted
      case (Some(_), None, None) => InProgress
      case (Some(memberDetails), _, _) =>
        if (memberDetails.isEmpty) {
          NotStarted
        } else {
          val countMemberDetails = memberDetails.size
          val countCompletedMembers = userAnswers.get(MembersDetailsCompletedPages(srn)).getOrElse(Map.empty).size
          if (countMemberDetails > countCompletedMembers) {
            InProgress
          } else {
            Recorded(countCompletedMembers, "members")
          }
        }
    }
  }

  def getLoansTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereLoans = userAnswers.get(LoansMadeOrOutstandingPage(srn))
    val numRecorded = userAnswers.get(OutstandingArrearsOnLoanPages(srn)).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val firstPages = userAnswers.get(IdentityTypes(srn, IdentitySubject.LoanRecipient))
    val lastPages = userAnswers.get(OutstandingArrearsOnLoanPages(srn))
    val incompleteIndex: Int = getIncompleteIndex(firstPages, lastPages)

    val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => firstQuestionPageUrl,
      index =>
        controllers.nonsipp.common.routes.IdentityTypeController
          .onPageLoad(srn, index, NormalMode, IdentitySubject.LoanRecipient)
          .url
    )

    val someReportedCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => listPageUrl,
      index =>
        controllers.nonsipp.common.routes.IdentityTypeController
          .onPageLoad(srn, index, NormalMode, IdentitySubject.LoanRecipient)
          .url
    )

    (wereLoans, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, inProgressCalculatedUrl)
      case (Some(true), _) => (Recorded(numRecorded, "loans"), someReportedCalculatedUrl)
      // Todo: check if smart navigation is required when 1+ have been reported - if not, use listPageUrl instead.
    }
  }

  def getNotStartedOrCannotStartYetStatus(userAnswers: UserAnswers, srn: Srn): TaskListStatus =
    getMembersTaskListStatus(userAnswers, srn) match {
      case TaskListStatus.InProgress =>
        userAnswers.get(MembersDetailsCompletedPages(srn)) match {
          case Some(completed) => TaskListStatus.NotStarted
          case None => TaskListStatus.UnableToStart
        }
      case TaskListStatus.Completed => TaskListStatus.NotStarted
      case TaskListStatus.Recorded(_, _) => TaskListStatus.NotStarted
      case TaskListStatus.UnableToStart => TaskListStatus.UnableToStart
      case TaskListStatus.NotStarted => TaskListStatus.UnableToStart
    }

  def getEmployerContributionStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val status = userAnswers
      .get(EmployerContributionsSectionStatus(srn))
      .fold[TaskListStatus](getNotStartedOrCannotStartYetStatus(userAnswers, srn)) {
        case SectionStatus.InProgress => TaskListStatus.InProgress
        case SectionStatus.Completed => TaskListStatus.Completed
      }
    val wereContributions = userAnswers.get(EmployerContributionsPage(srn))
    val link = (status, wereContributions) match {
      case (TaskListStatus.InProgress, Some(true)) =>
        controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
          .onPageLoad(srn, 1, NormalMode)
          .url
      case (TaskListStatus.Completed, Some(true)) =>
        controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
          .onPageLoad(srn, 1, NormalMode)
          .url
      case _ =>
        controllers.nonsipp.employercontributions.routes.EmployerContributionsController
          .onPageLoad(srn, NormalMode)
          .url

    }
    (status, link)
  }

  def getTransferInStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val status = userAnswers.get(TransfersInJourneyStatus(srn))
    val wereTransfersIn = userAnswers.get(DidSchemeReceiveTransferPage(srn))
    (wereTransfersIn, status) match {
      case (None, _) =>
        (
          getNotStartedOrCannotStartYetStatus(userAnswers, srn),
          controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController
            .onPageLoad(srn, NormalMode)
            .url
        )
      case (Some(false), _) =>
        (
          Completed,
          controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController
            .onPageLoad(srn, NormalMode)
            .url
        )
      case (Some(true), None) =>
        (
          InProgress,
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoad(srn, 1, NormalMode)
            .url
        )
      case (Some(true), Some(SectionStatus.InProgress)) =>
        (
          InProgress,
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoad(srn, 1, NormalMode)
            .url
        )
      case (Some(true), Some(SectionStatus.Completed)) =>
        (
          Completed,
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoad(srn, 1, NormalMode)
            .url
        )
    }
  }

  def getTransferOutStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereTransfersIn = userAnswers.get(SchemeTransferOutPage(srn))
    val numReported =
      userAnswers.map(TransfersOutSectionCompleted.all(srn)).flatten(_._2).count(_._2 == SectionCompleted)

    val firstQuestionPageUrl =
      controllers.nonsipp.membertransferout.routes.SchemeTransferOutController
        .onPageLoad(srn, NormalMode)
        .url

    val memberListPageUrl =
      controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (wereTransfersIn, numReported) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numReported, "transfers"), memberListPageUrl)
    }
  }

  def getSurrenderedBenefitsStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereSurrenderedBenefits = userAnswers.get(SurrenderedBenefitsPage(srn))
    val numReported = userAnswers.get(SurrenderedBenefitsCompleted.all(srn)).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsController
        .onPageLoad(srn, NormalMode)
        .url

    val memberListPageUrl =
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (wereSurrenderedBenefits, numReported) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numReported, "surrenders"), memberListPageUrl)
    }
  }

  def getMemberContributionStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereMemberContributions = userAnswers.get(MemberContributionsPage(srn))
    val numRecorded = userAnswers.get(AllTotalMemberContributionPages(srn)).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.membercontributions.routes.MemberContributionsController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.membercontributions.routes.MemberContributionListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (wereMemberContributions, numRecorded) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "contributions"), listPageUrl)
    }
  }

  def getPclsStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val werePcls = userAnswers.get(PensionCommencementLumpSumPage(srn))
    val numRecorded = userAnswers.get(PensionCommencementLumpSumAmountPage.all(srn)).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (werePcls, numRecorded) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "pcls"), listPageUrl)
    }
  }

  def getPensionPaymentsStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val werePensionPayments = userAnswers.get(PensionPaymentsReceivedPage(srn))
    val numRecorded = userAnswers
      .get(TotalAmountPensionPaymentsPage.all(srn))
      .getOrElse(Map.empty)
      .count({ case (memberIndex, amount) => !amount.isZero })

    val firstQuestionPageUrl =
      controllers.nonsipp.memberpensionpayments.routes.PensionPaymentsReceivedController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (werePensionPayments, numRecorded) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "payments"), listPageUrl)
    }
  }

  def getUnallocatedContributionsStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereUnallocatedContributions = userAnswers.get(UnallocatedEmployerContributionsPage(srn))
    val amount = userAnswers.get(UnallocatedEmployerAmountPage(srn))

    val firstQuestionPageUrl =
      controllers.nonsipp.memberpayments.routes.UnallocatedEmployerContributionsController
        .onPageLoad(srn, NormalMode)
        .url

    val cyaPageUrl =
      controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
        .onPageLoad(srn, NormalMode)
        .url

    (wereUnallocatedContributions, amount) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), None) => (InProgress, firstQuestionPageUrl)
      case (Some(true), Some(_)) => (Recorded, cyaPageUrl)
    }
  }

  def getLandOrPropertyTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereLandOrProperties = userAnswers.get(LandOrPropertyHeldPage(srn))
    val numRecorded = userAnswers.get(LandOrPropertyCompleted.all(srn)).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val firstPages = userAnswers.get(LandPropertyInUKPages(srn))
    val lastPages = userAnswers.get(LandOrPropertyTotalIncomePages(srn))
    val incompleteIndex: Int = getIncompleteIndex(firstPages, lastPages)

    val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => firstQuestionPageUrl,
      index =>
        controllers.nonsipp.landorproperty.routes.LandPropertyInUKController
          .onPageLoad(srn, index, NormalMode)
          .url
    )

    val someReportedCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => listPageUrl,
      index =>
        controllers.nonsipp.landorproperty.routes.LandPropertyInUKController
          .onPageLoad(srn, index, NormalMode)
          .url
    )

    (wereLandOrProperties, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, inProgressCalculatedUrl)
      case (Some(true), _) => (Recorded(numRecorded, "landOrProperties"), someReportedCalculatedUrl)
      // Todo: check if smart navigation is required when 1+ have been reported - if not, use listPageUrl instead.
    }
  }

  def getLandOrPropertyDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val atLeastOneCompleted =
      userAnswers.get(LandPropertyDisposalCompletedPages(srn)).exists(_.values.exists(_.values.nonEmpty))
    val started = userAnswers.get(LandOrPropertyDisposalPage(srn)).contains(true)
    val completedNoDisposals = userAnswers.get(LandOrPropertyDisposalPage(srn)).contains(false)
    val completedPage =
      userAnswers.map(LandPropertyDisposalCompletedPages(srn)).view.mapValues(_.count(_ => true)).values.sum
    val inProgressStartedPage =
      userAnswers.map(HowWasPropertyDisposedOfPagesTaskListStatus(srn)).view.mapValues(_.count(_ => true)).values.sum

    val initialDisposalUrl = controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val inProgressDisposalUrl =
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalAddressListController
        .onPageLoad(srn, page = 1)
        .url

    val disposalListPage = controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    if (inProgressStartedPage > completedPage) (TaskListStatus.InProgress, inProgressDisposalUrl)
    else if (atLeastOneCompleted) (TaskListStatus.Completed, disposalListPage)
    else if (completedNoDisposals) (TaskListStatus.Completed, initialDisposalUrl)
    else if (started) (TaskListStatus.InProgress, inProgressDisposalUrl)
    else (TaskListStatus.NotStarted, initialDisposalUrl)
  }

  def getBorrowingTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereBorrowings = userAnswers.get(MoneyBorrowedPage(srn))
    val numRecorded = userAnswers.get(WhySchemeBorrowedMoneyPages(srn)).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val firstPages = userAnswers.get(LenderNamePages(srn))
    val lastPages = userAnswers.get(WhySchemeBorrowedMoneyPages(srn))
    val incompleteIndex: Int = getIncompleteIndex(firstPages, lastPages)

    val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => firstQuestionPageUrl,
      index => controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, index, NormalMode).url
    )

    val someReportedCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => listPageUrl,
      index => controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, index, NormalMode).url
    )

    (wereBorrowings, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, inProgressCalculatedUrl)
      case (Some(true), _) => (Recorded(numRecorded, "borrowings"), someReportedCalculatedUrl)
      // Todo: check if smart navigation is required when 1+ have been reported - if not, use listPageUrl instead.
    }
  }

  private def getIncompleteIndex[A, B](
    firstPages: Option[Map[String, A]],
    lastPages: Option[Map[String, B]]
  ): Int =
    (firstPages, lastPages) match {
      case (None, _) => 1
      case (Some(_), None) => 1
      case (Some(first), Some(last)) =>
        if (first.isEmpty) {
          1
        } else {
          val firstIndexes = first.map(_._1.toInt)
          val lastIndexes = last.map(_._1.toInt).toList

          val filtered = firstIndexes.filter(lastIndexes.indexOf(_) < 0)
          if (filtered.isEmpty) {
            0
          } else {
            filtered.head + 1
          }
        }
    }

  def getSharesTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereSharesReported = userAnswers.get(DidSchemeHoldAnySharesPage(srn))
    val numReported = userAnswers.get(SharesCompleted.all(srn)).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.shares.routes.SharesListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val firstPages = userAnswers.get(TypeOfSharesHeldPages(srn))
    val lastPages = userAnswers.map(SharesCompleted.all(srn))
    val incompleteIndex: Int = getIncompleteIndex(firstPages, Some(lastPages))

    val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => firstQuestionPageUrl,
      index => controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, index, NormalMode).url
    )

    val someReportedCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => listPageUrl,
      index => controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, index, NormalMode).url
    )

    (wereSharesReported, numReported) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, inProgressCalculatedUrl)
      case (Some(true), _) => (Recorded(numReported, "shares"), someReportedCalculatedUrl)
      // Todo: check if smart navigation is required when 1+ have been reported - if not, use listPageUrl instead.
    }
  }

  def getSharesDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val sharesDisposalsMade = userAnswers.get(SharesDisposalPage(srn))
    val numReported = userAnswers.map(SharesDisposalProgress.all(srn)).flatten(_._2).count(_._2.completed)

    val firstQuestionPageUrl = controllers.nonsipp.sharesdisposal.routes.SharesDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val disposalsListPageUrl = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    (sharesDisposalsMade, numReported) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numReported, "disposals"), disposalsListPageUrl)
    }
  }

  def getQuotedSharesTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val quotedSharesManagedFundsHeld = userAnswers.get(QuotedSharesManagedFundsHeldPage(srn))
    val totalValueQuotedShares = userAnswers.get(TotalValueQuotedSharesPage(srn))

    val firstQuestionPageUrl =
      controllers.nonsipp.totalvaluequotedshares.routes.QuotedSharesManagedFundsHeldController
        .onPageLoad(srn, NormalMode)
        .url

    val cyaPageUrl =
      controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
        .onPageLoad(srn)
        .url

    (quotedSharesManagedFundsHeld, totalValueQuotedShares) match {
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), None) => (InProgress, firstQuestionPageUrl)
      case (_, Some(amount)) =>
        if (amount == Money(0)) (Recorded(0, ""), firstQuestionPageUrl) else (Recorded, cyaPageUrl)
      // The condition above is necessary because there is no boolean field in ETMP where we can store the answer to
      // the first question page, so a value of 0.00 stored in ETMP indicates that no Quoted Shares were reported.
      case _ => (NotStarted, firstQuestionPageUrl)
    }
  }

  def getBondsTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val defaultLink =
      controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController
        .onPageLoad(srn, NormalMode)
        .url
    val bondsListPageUrl =
      controllers.nonsipp.bonds.routes.BondsListController
        .onPageLoad(srn, 1, NormalMode)
        .url
    val hadBondsPage = userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn))
    val bondStatusPage = userAnswers.get(BondsJourneyStatus(srn))
    val firstPages = userAnswers.get(NameOfBondsPages(srn))
    val lastPages = userAnswers.map(BondsCompleted.all(srn))
    val incompleteIndex: Int = getIncompleteIndex(firstPages, Some(lastPages))
    val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => bondsListPageUrl,
      index => controllers.nonsipp.bonds.routes.NameOfBondsController.onPageLoad(srn, index, NormalMode).url
    )

    (hadBondsPage, bondStatusPage) match {
      case (None, _) => (NotStarted, defaultLink)
      case (Some(false), _) => (Completed, defaultLink)
      case (Some(true), None) => (InProgress, inProgressCalculatedUrl)
      case (Some(true), Some(SectionStatus.Completed)) => (Completed, bondsListPageUrl)
      case (Some(true), Some(SectionStatus.InProgress)) => (InProgress, inProgressCalculatedUrl)
    }
  }

  def getBondsDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {

    val didSchemeDisposeBonds = userAnswers.get(BondsDisposalPage(srn))

    val anyJourneysCompleted =
      userAnswers
        .map(BondsDisposalProgress.all(srn))
        .values
        .exists(_.values.nonEmpty)

    val sectionCompleted: Boolean = userAnswers.get(BondsDisposalCompleted(srn)).fold(false)(_ => true)

    val initialDisposalUrl = controllers.nonsipp.bondsdisposal.routes.BondsDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val disposalListPage = controllers.nonsipp.bondsdisposal.routes.BondsDisposalListController
      .onPageLoad(srn, page = 1, NormalMode)
      .url

    val disposalCompletedListPage = controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    didSchemeDisposeBonds match {
      case None => (TaskListStatus.NotStarted, initialDisposalUrl)
      case Some(false) => (TaskListStatus.Completed, initialDisposalUrl)
      case Some(true) if sectionCompleted => (TaskListStatus.Completed, disposalCompletedListPage)
      case Some(true) if anyJourneysCompleted => (TaskListStatus.InProgress, disposalCompletedListPage)
      case Some(true) => (TaskListStatus.InProgress, disposalListPage)
    }
  }

  def getOtherAssetsTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val defaultLink =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
        .onPageLoad(srn, NormalMode)
        .url
    val assetsListPageUrl =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val hadAssetsPage = userAnswers.get(OtherAssetsHeldPage(srn))
    val assetsStatusPage = userAnswers.get(OtherAssetsJourneyStatus(srn))
    val firstPages = userAnswers.get(WhatIsOtherAssetPages(srn))
    val lastPages = userAnswers.map(OtherAssetsCompleted.all(srn))
    val incompleteIndex: Int = getIncompleteIndex(firstPages, Some(lastPages))
    val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => assetsListPageUrl,
      index =>
        controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController.onPageLoad(srn, index, NormalMode).url
    )

    (hadAssetsPage, assetsStatusPage) match {
      case (None, _) => (NotStarted, defaultLink)
      case (Some(false), _) => (Completed, defaultLink)
      case (Some(true), None) => (InProgress, inProgressCalculatedUrl)
      case (Some(true), Some(SectionStatus.Completed)) => (Completed, assetsListPageUrl)
      case (Some(true), Some(SectionStatus.InProgress)) => (InProgress, inProgressCalculatedUrl)
    }
  }

  def getOtherAssetsDisposalTaskListStatusAndLink(
    userAnswers: UserAnswers,
    srn: Srn
  ): (TaskListStatus, String) = {

    val didSchemeDisposePage = controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val assetsToDisposePage = controllers.nonsipp.otherassetsdisposal.routes.StartReportingAssetsDisposalController
      .onPageLoad(srn, page = 1)
      .url

    val reportedDisposalsPage = controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    val anyJourneysCompleted = userAnswers
      .map(OtherAssetsDisposalProgress.all(srn))
      .values
      .exists(_.values.exists(_.completed))

    val allJourneysCompleted = userAnswers
      .map(OtherAssetsDisposalProgress.all(srn))
      .values
      .forall(_.values.forall(_.completed))

    val (status, link) =
      (userAnswers.get(OtherAssetsDisposalPage(srn)), userAnswers.get(OtherAssetsDisposalCompleted(srn))) match {
        // No user answers provided for this section
        case (None, _) => (NotStarted, didSchemeDisposePage)
        // User answered "No" for "Did scheme dispose of any other assets...?"
        case (Some(false), _) => (TaskListStatus.Completed, didSchemeDisposePage)
        // No complete journeys & 1 incomplete journey
        case (Some(true), None) if !anyJourneysCompleted => (TaskListStatus.InProgress, assetsToDisposePage)
        // 1 or more complete journeys & 1 or more incomplete journeys
        case (Some(true), None) if !allJourneysCompleted => (TaskListStatus.InProgress, reportedDisposalsPage)
        // 1 or more complete journeys & 0 incomplete journeys
        case (Some(true), None) if allJourneysCompleted => (TaskListStatus.Completed, reportedDisposalsPage)
        // User answered "No" for "Do you need to report another asset disposal?"
        case (Some(true), Some(_)) => (TaskListStatus.Completed, reportedDisposalsPage)

        /*
        // The case shown below is logically equivalent to the last 2 cases shown above, and could replace them,
        // resulting in less code, but also less clarity, which I think is the most important thing right now. As such,
        // I've shown both options, but commented this one out for now.
        case (Some(true), _) => (TaskListStatus.Completed, reportedDisposalsPage)
       */
      }

    (status, link)
  }

  def getCompletedOrUpdatedTaskListStatus(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    path: JsPath,
    toExclude: Option[String] = None
  ): TaskListStatus = {
    val c = currentUA.get(path).getOrElse(JsObject.empty).as[JsObject] - toExclude.getOrElse("")
    val p = previousUA.get(path).getOrElse(JsObject.empty).as[JsObject] - toExclude.getOrElse("")
    if (c == p) {
      Completed
    } else {
      Updated
    }
  }

  def getFinancialDetailsCompletedOrUpdated(currentUA: UserAnswers, previousUA: UserAnswers): TaskListStatus =
    if (currentUA.get(schemeDesignatory \ "totalAssetValue") ==
        previousUA.get(schemeDesignatory \ "totalAssetValue")
      &&
      currentUA.get(schemeDesignatory \ "totalPayments") ==
        previousUA.get(schemeDesignatory \ "totalPayments")
      &&
      currentUA.get(schemeDesignatory \ "totalCash") ==
        previousUA.get(schemeDesignatory \ "totalCash")) {
      Completed
    } else {
      Updated
    }

  def getBasicDetailsCompletedOrUpdated(currentUA: UserAnswers, previousUA: UserAnswers): TaskListStatus = {
    val accountingPeriodsSame = currentUA.get(accountingPeriodDetails \ "accountingPeriods") == previousUA.get(
      accountingPeriodDetails \ "accountingPeriods"
    )

    val designatoryCurrent = currentUA
      .get(schemeDesignatory)
      .get
      .as[JsObject] - "totalAssetValue" - "totalPayments" - "totalCash" - "recordVersion"
    val designatoryPrevious = previousUA
      .get(schemeDesignatory)
      .get
      .as[JsObject] - "totalAssetValue" - "totalPayments" - "totalCash" - "recordVersion"
    val schemeDesignatoriesSame = designatoryCurrent == designatoryPrevious

    if (accountingPeriodsSame && schemeDesignatoriesSame) {
      Completed
    } else {
      Updated
    }
  }

  def userAnswersUnchangedAllSections(currentUA: UserAnswers, previousUA: UserAnswers): Boolean = {
    (getBasicDetailsCompletedOrUpdated(currentUA, previousUA) == Completed) &&
    (getFinancialDetailsCompletedOrUpdated(currentUA, previousUA) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberdetails.Paths.personalDetails,
      Some("safeToHardDelete")
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.employercontributions.Paths.memberEmpContribution
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberpayments.Paths.membersPayments \ "unallocatedContribAmount"
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.membercontributions.Paths.memberDetails \ "totalMemberContribution"
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.receivetransfer.Paths.memberTransfersIn
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.membertransferout.Paths.memberTransfersOut
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberreceivedpcls.Paths.memberDetails \ "memberLumpSumReceived"
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberpensionpayments.Paths.memberDetails \ "pensionAmountReceived"
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.membersurrenderedbenefits.Paths.memberPensionSurrender
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.loansmadeoroutstanding.Paths.loans
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.moneyborrowed.Paths.borrowing
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.shares.Paths.shareTransactions,
      Some("disposedSharesTransaction")
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.sharesdisposal.Paths.disposedSharesTransaction
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.landorproperty.Paths.landOrPropertyTransactions,
      Some("disposedPropertyTransaction")
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.landorpropertydisposal.Paths.disposalPropertyTransaction
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.bonds.Paths.bondTransactions,
      Some("bondsDisposed")
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.bondsdisposal.Paths.bondsDisposed
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.totalvaluequotedshares.Paths.quotedShares \ "totalValueQuotedShares"
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.otherassetsheld.Paths.otherAssetsTransactions,
      Some("assetsDisposed")
    ) == Completed) &&
    (getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.otherassetsdisposal.Paths.assetsDisposed
    ) == Completed)
  }
}
