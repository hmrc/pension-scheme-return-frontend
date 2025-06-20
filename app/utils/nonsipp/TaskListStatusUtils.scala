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

import pages.nonsipp.employercontributions.{EmployerContributionsCompleted, EmployerContributionsPage}
import pages.nonsipp.otherassetsdisposal.{OtherAssetsDisposalPage, OtherAssetsDisposalProgress}
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld._
import models.SchemeId.Srn
import pages.nonsipp.landorproperty._
import pages.nonsipp.receivetransfer.{DidSchemeReceiveTransferPage, TransfersInSectionCompleted}
import pages.nonsipp.landorpropertydisposal.{LandOrPropertyDisposalPage, LandOrPropertyDisposalProgress}
import pages.nonsipp.memberpensionpayments.{PensionPaymentsReceivedPage, TotalAmountPensionPaymentsPage}
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
import viewmodels.models.TaskListStatus._
import pages.nonsipp.schemedesignatory.Paths.schemeDesignatory
import utils.nonsipp.check._
import pages.nonsipp.membertransferout.{SchemeTransferOutPage, TransfersOutSectionCompleted}
import pages.nonsipp.moneyborrowed.{MoneyBorrowedPage, MoneyBorrowedProgress}
import pages.nonsipp.bondsdisposal.{BondsDisposalPage, BondsDisposalProgress}
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus, TaskListStatus}

object TaskListStatusUtils {

  def getBasicDetailsTaskListStatus(
    checkReturnDates: Option[Boolean],
    accountingPeriods: Option[List[DateRange]],
    activeBankAccount: Option[Boolean],
    whyNoBankAccount: Option[String],
    howManyMembers: Option[SchemeMemberNumbers]
  ): TaskListStatus =
    (checkReturnDates, accountingPeriods, activeBankAccount, whyNoBankAccount, howManyMembers) match {
      case (Some(true), _, Some(true), None, Some(_)) => Recorded
      case (Some(true), _, Some(false), Some(_), Some(_)) => Recorded
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

  def getMembersTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val membersDetailsPages = userAnswers.get(MembersDetailsPages(srn))
    val numRecorded =
      userAnswers.get(MemberDetailsManualProgress.all()).getOrElse(Map.empty).count(_._2.completed)

    val firstQuestionPageUrl =
      controllers.nonsipp.memberdetails.routes.PensionSchemeMembersController
        .onPageLoad(srn)
        .url

    val listPageUrl =
      controllers.nonsipp.memberdetails.routes.SchemeMembersListController
        .onPageLoad(srn, 1, ManualOrUpload.Manual)
        .url

    val getInProgressUrl = userAnswers
      .map(MemberDetailsManualProgress.all())
      .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
      .getOrElse(firstQuestionPageUrl)

    val getInProgressOrListPageUrl = userAnswers
      .map(MemberDetailsManualProgress.all())
      .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
      .getOrElse(listPageUrl)

    val checked = userAnswers.get(MembersDetailsChecked(srn))

    (membersDetailsPages, numRecorded, checked) match {
      case (_, _, Some(false)) => (Check, listPageUrl)
      case (None, _, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(memberDetails), 0, _) if memberDetails.isEmpty =>
        (NotStarted, firstQuestionPageUrl) // Last member removed
      case (Some(_), 0, _) => (InProgress, getInProgressUrl)
      case (Some(_), _, _) => (Recorded(numRecorded, "members"), getInProgressOrListPageUrl)
    }
  }

  def getEmployerContributionStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereEmployerContributions = userAnswers.get(EmployerContributionsPage(srn))
    val numRecorded =
      userAnswers.map(EmployerContributionsCompleted.all()).flatten(using _._2).count(_._2 == SectionCompleted)

    val firstQuestionPageUrl =
      controllers.nonsipp.employercontributions.routes.EmployerContributionsController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (wereEmployerContributions, numRecorded) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "contributions"), listPageUrl)
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

  def getMemberContributionStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereMemberContributions = userAnswers.get(MemberContributionsPage(srn))
    val numRecorded = userAnswers
      .get(AllTotalMemberContributionPages(srn))
      .getOrElse(Map.empty)
      .count { case (memberIndex, amount) => !amount.isZero }

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

  def getTransferInStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereTransfersIn = userAnswers.get(DidSchemeReceiveTransferPage(srn))
    val numRecorded =
      userAnswers.map(TransfersInSectionCompleted.all()).flatten(using _._2).count(_._2 == SectionCompleted)

    val firstQuestionPageUrl =
      controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (wereTransfersIn, numRecorded) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "transfers"), listPageUrl)
    }
  }

  def getTransferOutStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereTransfersOut = userAnswers.get(SchemeTransferOutPage(srn))
    val numRecorded =
      userAnswers.map(TransfersOutSectionCompleted.all()).flatten(using _._2).count(_._2 == SectionCompleted)

    val firstQuestionPageUrl =
      controllers.nonsipp.membertransferout.routes.SchemeTransferOutController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (wereTransfersOut, numRecorded) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "transfers"), listPageUrl)
    }
  }

  def getPclsStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val werePcls = userAnswers.get(PensionCommencementLumpSumPage(srn))
    val numRecorded = userAnswers
      .get(PensionCommencementLumpSumAmountPage.all())
      .getOrElse(Map.empty)
      .count { case (memberIndex, amount) => !amount.isZero }

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
      .get(TotalAmountPensionPaymentsPage.all())
      .getOrElse(Map.empty)
      .count { case (memberIndex, amount) => !amount.isZero }

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

  def getSurrenderedBenefitsStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereSurrenderedBenefits = userAnswers.get(SurrenderedBenefitsPage(srn))
    val numRecorded = userAnswers.get(SurrenderedBenefitsCompleted.all()).getOrElse(Map.empty).size

    val firstQuestionPageUrl =
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsController
        .onPageLoad(srn, NormalMode)
        .url

    val memberListPageUrl =
      controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    (wereSurrenderedBenefits, numRecorded) match {
      case (None, _) => (getNotStartedOrCannotStartYetStatus(userAnswers, srn), firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "surrenders"), memberListPageUrl)
    }
  }

  def getLoansTaskListStatusAndLink(
    userAnswers: UserAnswers,
    srn: Srn,
    isPrePop: Boolean
  ): (TaskListStatus, String) =
    if (isPrePop && LoansCheckStatusUtils.checkLoansSection(userAnswers, srn)) {
      (
        Check,
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
          .onPageLoad(srn, 1, NormalMode)
          .url
      )
    } else {
      val wereLoans = userAnswers.get(LoansMadeOrOutstandingPage(srn))
      val numRecorded = userAnswers.get(LoanCompleted.all()).getOrElse(Map.empty).size

      val firstQuestionPageUrl =
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
          .onPageLoad(srn, NormalMode)
          .url

      val listPageUrl =
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
          .onPageLoad(srn, 1, NormalMode)
          .url

      val getInProgressUrl = userAnswers
        .map(LoansProgress.all())
        .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
        .getOrElse(firstQuestionPageUrl)

      (wereLoans, numRecorded) match {
        case (None, _) => (NotStarted, firstQuestionPageUrl)
        case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
        case (Some(true), 0) => (InProgress, getInProgressUrl)
        case (Some(true), _) => (Recorded(numRecorded, "loans"), listPageUrl)
      }
    }

  def getBorrowingTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereBorrowings = userAnswers.get(MoneyBorrowedPage(srn))
    val numRecorded = userAnswers.get(MoneyBorrowedProgress.all()).getOrElse(Map.empty).count(_._2.completed)

    val firstQuestionPageUrl =
      controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
        .onPageLoad(srn, 1, NormalMode)
        .url

    val inProgressUrl = userAnswers
      .map(MoneyBorrowedProgress.all())
      .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
      .getOrElse(firstQuestionPageUrl)

    (wereBorrowings, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, inProgressUrl)
      case (Some(true), _) => (Recorded(numRecorded, "borrowings"), listPageUrl)
    }
  }

  def getSharesTaskListStatusAndLink(
    userAnswers: UserAnswers,
    srn: Srn,
    isPrePop: Boolean
  ): (TaskListStatus, String) =
    if (isPrePop && SharesCheckStatusUtils.checkSharesSection(userAnswers, srn)) {
      (
        Check,
        controllers.nonsipp.shares.routes.SharesListController
          .onPageLoad(srn, 1, NormalMode)
          .url
      )
    } else {
      val wereShares = userAnswers.get(DidSchemeHoldAnySharesPage(srn))
      val numRecorded = userAnswers.get(SharesProgress.all()).getOrElse(Map.empty).count(_._2.completed)

      val firstQuestionPageUrl =
        controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController
          .onPageLoad(srn, NormalMode)
          .url

      val listPageUrl =
        controllers.nonsipp.shares.routes.SharesListController
          .onPageLoad(srn, 1, NormalMode)
          .url

      val inProgressUrl = userAnswers
        .map(SharesProgress.all())
        .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
        .getOrElse(firstQuestionPageUrl)

      (wereShares, numRecorded) match {
        case (None, _) => (NotStarted, firstQuestionPageUrl)
        case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
        case (Some(true), 0) => (InProgress, inProgressUrl)
        case (Some(true), _) => (Recorded(numRecorded, "shares"), listPageUrl)
      }
    }

  def getSharesDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val sharesDisposalsMade = userAnswers.get(SharesDisposalPage(srn))
    val numRecorded = userAnswers.map(SharesDisposalProgress.all()).flatten(using _._2).count(_._2.completed)

    val firstQuestionPageUrl = controllers.nonsipp.sharesdisposal.routes.SharesDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val disposalsListPageUrl = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    (sharesDisposalsMade, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "disposals"), disposalsListPageUrl)
    }
  }

  def getLandOrPropertyTaskListStatusAndLink(
    userAnswers: UserAnswers,
    srn: Srn,
    isPrePop: Boolean
  ): (TaskListStatus, String) =
    if (isPrePop && LandOrPropertyCheckStatusUtils.checkLandOrPropertySection(userAnswers, srn)) {
      (
        Check,
        controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
          .onPageLoad(srn, 1, NormalMode)
          .url
      )
    } else {
      val wereLandOrPropertiesHeld = userAnswers.get(LandOrPropertyHeldPage(srn))
      val numRecorded = userAnswers.map(LandOrPropertyProgress.all()).count(_._2.completed)

      val firstQuestionPageUrl =
        controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController
          .onPageLoad(srn, NormalMode)
          .url

      val listPageUrl =
        controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
          .onPageLoad(srn, 1, NormalMode)
          .url

      val inProgressUrl = userAnswers
        .map(LandOrPropertyProgress.all())
        .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
        .getOrElse(firstQuestionPageUrl)

      (wereLandOrPropertiesHeld, numRecorded) match {
        case (None, _) => (NotStarted, firstQuestionPageUrl)
        case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
        case (Some(true), 0) => (InProgress, inProgressUrl)
        case (Some(true), _) => (Recorded(numRecorded, "landOrProperties"), listPageUrl)
      }
    }

  def getLandOrPropertyDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereLandOrPropertyDisposals = userAnswers.get(LandOrPropertyDisposalPage(srn))
    val numRecorded = userAnswers
      .map(LandOrPropertyDisposalProgress.all())
      .values
      .flatMap(_.values)
      .toList
      .count(_.completed)

    val firstQuestionPageUrl =
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
        .onPageLoad(srn, page = 1)
        .url

    (wereLandOrPropertyDisposals, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "disposals"), listPageUrl)
    }
  }

  def getBondsTaskListStatusAndLink(
    userAnswers: UserAnswers,
    srn: Srn,
    isPrePop: Boolean
  ): (TaskListStatus, String) =
    if (isPrePop && BondsCheckStatusUtils.checkBondsSection(userAnswers, srn)) {
      (
        Check,
        controllers.nonsipp.bonds.routes.BondsListController
          .onPageLoad(srn, 1, NormalMode)
          .url
      )
    } else {
      val wereBonds = userAnswers.get(UnregulatedOrConnectedBondsHeldPage(srn))
      val numRecorded = userAnswers
        .get(BondsProgress.all())
        .getOrElse(Map.empty)
        .values
        .count(_.completed)

      val firstQuestionPageUrl =
        controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController
          .onPageLoad(srn, NormalMode)
          .url

      val listPageUrl =
        controllers.nonsipp.bonds.routes.BondsListController
          .onPageLoad(srn, 1, NormalMode)
          .url

      val inProgressUrl = userAnswers
        .map(BondsProgress.all())
        .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
        .getOrElse(firstQuestionPageUrl)

      (wereBonds, numRecorded) match {
        case (None, _) => (NotStarted, firstQuestionPageUrl)
        case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
        case (Some(true), 0) => (InProgress, inProgressUrl)
        case (Some(true), _) => (Recorded(numRecorded, "bonds"), listPageUrl)
      }
    }

  def getBondsDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereBondsDisposals = userAnswers.get(BondsDisposalPage(srn))
    val numRecorded = userAnswers.map(BondsDisposalProgress.all()).flatten(using _._2).count(_._2.completed)

    val firstQuestionPageUrl =
      controllers.nonsipp.bondsdisposal.routes.BondsDisposalController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
        .onPageLoad(srn, page = 1)
        .url

    (wereBondsDisposals, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "disposals"), listPageUrl)
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
      // the first question page, so a value of 0.00 stored in ETMP indicates that no Quoted Shares were recorded.
      case _ => (NotStarted, firstQuestionPageUrl)
    }
  }

  def getOtherAssetsTaskListStatusAndLink(
    userAnswers: UserAnswers,
    srn: Srn,
    isPrePop: Boolean
  ): (TaskListStatus, String) =
    if (isPrePop && OtherAssetsCheckStatusUtils.checkOtherAssetsSection(userAnswers, srn)) {
      (
        Check,
        controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
          .onPageLoad(srn, 1, NormalMode)
          .url
      )
    } else {
      val wereOtherAssets = userAnswers.get(OtherAssetsHeldPage(srn))
      val numRecorded = userAnswers.get(OtherAssetsProgress.all()).getOrElse(Map.empty).count(_._2.completed)

      val firstQuestionPageUrl =
        controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
          .onPageLoad(srn, NormalMode)
          .url

      val listPageUrl =
        controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
          .onPageLoad(srn, 1, NormalMode)
          .url

      val inProgressUrl = userAnswers
        .map(OtherAssetsProgress.all())
        .collectFirst { case (_, SectionJourneyStatus.InProgress(url)) => url }
        .getOrElse(firstQuestionPageUrl)

      (wereOtherAssets, numRecorded) match {
        case (None, _) => (NotStarted, firstQuestionPageUrl)
        case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
        case (Some(true), 0) => (InProgress, inProgressUrl)
        case (Some(true), _) => (Recorded(numRecorded, "otherAssets"), listPageUrl)
      }
    }

  def getOtherAssetsDisposalTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val wereOtherAssetsDisposals = userAnswers.get(OtherAssetsDisposalPage(srn))
    val numRecorded = userAnswers.map(OtherAssetsDisposalProgress.all()).flatten(using _._2).count(_._2.completed)

    val firstQuestionPageUrl =
      controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController
        .onPageLoad(srn, NormalMode)
        .url

    val listPageUrl =
      controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
        .onPageLoad(srn, page = 1)
        .url

    (wereOtherAssetsDisposals, numRecorded) match {
      case (None, _) => (NotStarted, firstQuestionPageUrl)
      case (Some(false), _) => (Recorded(0, ""), firstQuestionPageUrl)
      case (Some(true), 0) => (InProgress, firstQuestionPageUrl)
      case (Some(true), _) => (Recorded(numRecorded, "disposals"), listPageUrl)
    }
  }

  private def getNotStartedOrCannotStartYetStatus(userAnswers: UserAnswers, srn: Srn): TaskListStatus =
    getMembersTaskListStatusAndLink(userAnswers, srn)._1 match {
      case TaskListStatus.InProgress =>
        userAnswers.get(MembersDetailsCompletedPages(srn)) match {
          case Some(completed) => TaskListStatus.NotStarted
          case None => TaskListStatus.UnableToStart
        }
      case TaskListStatus.Recorded(_, _) => TaskListStatus.NotStarted
      case _ => TaskListStatus.UnableToStart
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

  def getBasicDetailsCompletedOrUpdated(currentUA: UserAnswers, previousUA: UserAnswers): TaskListStatus = {
    val accountingPeriodsSame = currentUA.get(accountingPeriodDetails \ "accountingPeriods") == previousUA.get(
      accountingPeriodDetails \ "accountingPeriods"
    )

    val designatoryCurrent = currentUA
      .get(schemeDesignatory)
      .getOrElse(JsObject.empty)
      .as[JsObject] - "totalAssetValue" - "totalPayments" - "totalCash" - "recordVersion"
    val designatoryPrevious = previousUA
      .get(schemeDesignatory)
      .getOrElse(JsObject.empty)
      .as[JsObject] - "totalAssetValue" - "totalPayments" - "totalCash" - "recordVersion"
    val schemeDesignatoriesSame = designatoryCurrent == designatoryPrevious

    if (accountingPeriodsSame && schemeDesignatoriesSame) {
      Completed
    } else {
      Updated
    }
  }

  def getFinancialDetailsCompletedOrUpdated(currentUA: UserAnswers, previousUA: UserAnswers): TaskListStatus =
    if (
      currentUA.get(schemeDesignatory \ "totalAssetValue") ==
        previousUA.get(schemeDesignatory \ "totalAssetValue")
        &&
        currentUA.get(schemeDesignatory \ "totalPayments") ==
        previousUA.get(schemeDesignatory \ "totalPayments")
        &&
        currentUA.get(schemeDesignatory \ "totalCash") ==
        previousUA.get(schemeDesignatory \ "totalCash")
    ) {
      Completed
    } else {
      Updated
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
