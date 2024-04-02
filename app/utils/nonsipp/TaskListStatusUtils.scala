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

package utils.nonsipp

import pages.nonsipp.employercontributions.{EmployerContributionsPage, EmployerContributionsSectionStatus}
import pages.nonsipp.schemedesignatory.{FeesCommissionsWagesSalariesPage, HowManyMembersPage, HowMuchCashPage}
import models.ConditionalYesNo._
import pages.nonsipp.shares._
import pages.nonsipp.otherassetsheld.OtherAssetsHeldPage
import config.Refined.{Max5000, OneTo5000}
import models.SchemeId.Srn
import pages.nonsipp.landorproperty._
import pages.nonsipp.landorpropertydisposal.{LandOrPropertyDisposalPage, LandPropertyDisposalCompletedPages}
import eu.timepit.refined.{refineMV, refineV}
import pages.nonsipp.sharesdisposal._
import models._
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.models.{SectionStatus, TaskListStatus}
import pages.nonsipp.bonds._
import pages.nonsipp.memberdetails.{MemberDetailsNinoPages, MembersDetailsPages, NoNinoPages}
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import viewmodels.models.TaskListStatus.{TaskListStatus, _}
import pages.nonsipp.common.IdentityTypes
import pages.nonsipp.moneyborrowed.{LenderNamePages, MoneyBorrowedPage, WhySchemeBorrowedMoneyPages}
import pages.nonsipp.bondsdisposal.{BondsDisposalCompletedPages, BondsDisposalPage}

object TaskListStatusUtils {

  def getBasicSchemeDetailsTaskListStatus(
    srn: Srn,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId,
    activeBankAccount: Option[Boolean],
    whyNoBankAccountPage: Option[String]
  ): TaskListStatus.TaskListStatus with Serializable =
    (userAnswers.get(HowManyMembersPage(srn, pensionSchemeId)), activeBankAccount, whyNoBankAccountPage) match {
      case (None, _, _) => InProgress
      case (Some(_), Some(true), _) => Completed
      case (Some(_), Some(false), Some(_)) => Completed
      case (Some(_), Some(false), None) => InProgress
    }

  def getFinancialDetailsTaskListStatus(userAnswers: UserAnswers, srn: Srn): TaskListStatus = {
    val totalSalaries = userAnswers.get(FeesCommissionsWagesSalariesPage(srn, NormalMode))
    val howMuchCash = userAnswers.get(HowMuchCashPage(srn, NormalMode))
    (howMuchCash, totalSalaries) match {
      case (Some(_), Some(_)) => Completed
      case (None, _) => NotStarted
      case (Some(_), None) => InProgress
    }
  }

  def getMembersTaskListStatus(userAnswers: UserAnswers, srn: Srn): TaskListStatus = {
    val membersDetailsPages = userAnswers.get(MembersDetailsPages(srn))
    val ninoPages = userAnswers.get(MemberDetailsNinoPages(srn))
    val noNinoPages = userAnswers.get(NoNinoPages(srn))
    (membersDetailsPages, ninoPages, noNinoPages) match {
      case (None, _, _) => NotStarted
      case (Some(_), None, None) => InProgress
      case (Some(memberDetails), ninos, noNinos) =>
        if (memberDetails.isEmpty) {
          NotStarted
        } else {
          val countMemberDetails = memberDetails.size
          val countNinos = ninos.getOrElse(List.empty).size
          val countNoninos = noNinos.getOrElse(List.empty).size
          if (countMemberDetails > countNinos + countNoninos) {
            InProgress
          } else {
            Completed
          }
        }
    }
  }

  def getLoansTaskListStatus(userAnswers: UserAnswers, srn: Srn): TaskListStatus with Serializable = {
    val loansMadePage = userAnswers.get(LoansMadeOrOutstandingPage(srn))
    val whoReceivedPages = userAnswers.get(IdentityTypes(srn, IdentitySubject.LoanRecipient))
    val arrearsPages = userAnswers.get(OutstandingArrearsOnLoanPages(srn))
    val sponsoringPages = userAnswers.get(RecipientSponsoringEmployerConnectedPartyPages(srn))
    val connectedPartyPages = userAnswers.get(IsIndividualRecipientConnectedPartyPages(srn))
    (loansMadePage, whoReceivedPages, arrearsPages, sponsoringPages, connectedPartyPages) match {
      case (None, _, _, _, _) => NotStarted
      case (Some(loansMade), whoReceived, arrears, sponsoring, connected) =>
        if (!loansMade) {
          Completed
        } else {
          val countLoanTransactions = whoReceived.getOrElse(List.empty).size
          val countLastPage = arrears.getOrElse(List.empty).size
          if (countLoanTransactions + countLastPage == 0) {
            InProgress
          } else if (countLoanTransactions > countLastPage) {
            InProgress
          } else {
            val countSponsoringAndConnectedPages = sponsoring.getOrElse(List.empty).size + connected
              .getOrElse(List.empty)
              .size
            if (countSponsoringAndConnectedPages < countLastPage) {
              InProgress
            } else {
              Completed
            }
          }
        }
    }
  }

  def getIncompleteLoansLink(userAnswers: UserAnswers, srn: Srn): String = {
    val whoReceivedTheLoanPages = userAnswers.get(IdentityTypes(srn, IdentitySubject.LoanRecipient))
    val outstandingArrearsOnLoanPages = userAnswers.get(OutstandingArrearsOnLoanPages(srn))
    val sponsoringPages = userAnswers.get(RecipientSponsoringEmployerConnectedPartyPages(srn))
    val connectedPartyPages = userAnswers.get(IsIndividualRecipientConnectedPartyPages(srn))

    val incompleteIndex =
      (whoReceivedTheLoanPages, outstandingArrearsOnLoanPages, sponsoringPages, connectedPartyPages) match {
        case (None, _, _, _) => 1
        case (Some(_), None, _, _) => 1
        case (Some(whoReceived), arrears, sponsoring, connected) =>
          if (whoReceived.isEmpty) {
            1
          } else {
            val whoReceivedIndexes = (0 until whoReceived.size).toList
            val arrearsIndexes = arrears.getOrElse(List.empty).map(_._1.toInt).toList
            val sponsoringAndConnectedIndexes = sponsoring.getOrElse(List.empty).map(_._1.toInt).toList ++ connected
              .getOrElse(List.empty)
              .map(_._1.toInt)
              .toList
            val filtered = whoReceivedIndexes.filter(arrearsIndexes.indexOf(_) < 0)
            val filteredSC = whoReceivedIndexes.filter(sponsoringAndConnectedIndexes.indexOf(_) < 0)
            if (filtered.isEmpty && filteredSC.isEmpty) {
              1
            } else {
              if (filteredSC.isEmpty) {
                filtered.head + 1 // index based on arrears page missing
              } else {
                filteredSC.head + 1 // index based on sponsoring employer or individual connected party
              }
            }
          }
      }

    refineV[OneTo5000](incompleteIndex).fold(
      _ =>
        controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
          .onPageLoad(srn, 1, NormalMode)
          .url,
      index =>
        controllers.nonsipp.common.routes.IdentityTypeController
          .onPageLoad(srn, index, NormalMode, IdentitySubject.LoanRecipient)
          .url
    )
  }
  def getNotStartedOrCannotStartYetStatus(userAnswers: UserAnswers, srn: Srn): TaskListStatus =
    getMembersTaskListStatus(userAnswers, srn) match {
      case TaskListStatus.InProgress => TaskListStatus.NotStarted
      case TaskListStatus.Completed => TaskListStatus.NotStarted
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
  def getLandOrPropertyTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val heldPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode).url
    val listPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, 1, NormalMode).url
    def inUkPageUrl(index: Max5000) =
      controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad(srn, index, NormalMode).url

    val landOrPropertyHeldPage = userAnswers.get(LandOrPropertyHeldPage(srn))
    val firstPages = userAnswers.get(LandPropertyInUKPages(srn))
    val lastPages = userAnswers.get(LandOrPropertyTotalIncomePages(srn))
    val lastLesseeSubJourneyPages = userAnswers.get(IsLesseeConnectedPartyPages(srn))
    val firstLesseeSubJourneyPages = userAnswers.get(IsLandPropertyLeasedPages(srn))
    val whyDoesSchemeHoldLandPropertyPages = userAnswers.get(WhyDoesSchemeHoldLandPropertyPages(srn))
    val landPropertyIndependentValuationPages = userAnswers.get(LandPropertyIndependentValuationPages(srn))
    (
      landOrPropertyHeldPage,
      firstPages,
      lastPages,
      firstLesseeSubJourneyPages,
      lastLesseeSubJourneyPages,
      whyDoesSchemeHoldLandPropertyPages,
      landPropertyIndependentValuationPages
    ) match {
      case (None, _, _, _, _, _, _) => (NotStarted, heldPageUrl)
      case (
          Some(landOrPropertyHeld),
          firstPages,
          lastPages,
          firstLesseeSubjourney,
          lastLesseeSubjourney,
          whyHeldPages,
          indepValPages
          ) =>
        if (!landOrPropertyHeld) {
          (Completed, heldPageUrl)
        } else {
          val countFirstPages = firstPages.getOrElse(List.empty).size
          val countLastPages = lastPages.getOrElse(List.empty).size
          val countLastLesseeSubjourney = lastLesseeSubjourney.getOrElse(List.empty).size
          val countFirstLesseeSubjourney = firstLesseeSubjourney.getOrElse(List.empty).count(b => !b._2)
          val countAcquisitionContributionSubJourney =
            whyHeldPages.getOrElse(List.empty).count(b => b._2 != SchemeHoldLandProperty.Transfer)
          val countIndepValPages = indepValPages.getOrElse(List.empty).size

          val incompleteIndex: Int = getLandOrPropertyIncompleteIndex(
            firstPages,
            lastPages,
            whyHeldPages,
            indepValPages,
            firstLesseeSubjourney,
            lastLesseeSubjourney
          )
          val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
            _ => listPageUrl,
            index => inUkPageUrl(index)
          )

          if (countFirstPages + countLastPages == 0) {
            (InProgress, inUkPageUrl(refineMV(1)))
          } else if (countFirstPages > countLastPages) {
            (InProgress, inProgressCalculatedUrl)
          } else {
            if ((countLastLesseeSubjourney + countFirstLesseeSubjourney) != countLastPages ||
              countAcquisitionContributionSubJourney != countIndepValPages) {
              (InProgress, inProgressCalculatedUrl)
            } else {
              (Completed, listPageUrl)
            }
          }
        }
    }
  }

  private def getLandOrPropertyIncompleteIndex(
    firstPages: Option[Map[String, Boolean]],
    lastPages: Option[Map[String, Money]],
    whyHeldPages: Option[Map[String, SchemeHoldLandProperty]],
    indepValPages: Option[Map[String, Boolean]],
    firstLesseeSubjourney: Option[Map[String, Boolean]],
    lastLesseeSubjourney: Option[Map[String, Boolean]]
  ): Int =
    (firstPages, lastPages, whyHeldPages, indepValPages, firstLesseeSubjourney, lastLesseeSubjourney) match {
      case (None, _, _, _, _, _) => 1
      case (Some(_), None, _, _, _, _) => 1
      case (Some(first), last, whyHeld, indepVal, firstLessee, lastLessee) =>
        if (first.isEmpty) {
          1
        } else {
          val firstIndexes = (0 until first.size).toList
          val lastIndexes = last.getOrElse(List.empty).map(_._1.toInt).toList
          val whyHeldIndexes =
            whyHeld.getOrElse(List.empty).filter(b => b._2 != SchemeHoldLandProperty.Transfer).map(_._1.toInt).toList
          val indepValIndexes = indepVal.getOrElse(List.empty).map(_._1.toInt).toList
          val firstLesseeIndexes = firstLessee.getOrElse(List.empty).filter(b => !b._2).map(_._1.toInt).toList
          val lastLesseeIndexes = lastLessee.getOrElse(List.empty).map(_._1.toInt).toList
          val lesseeIndexes = firstLesseeIndexes ++ lastLesseeIndexes

          val filtered = firstIndexes.filter(lastIndexes.indexOf(_) < 0)
          val filteredWhyHeld = whyHeldIndexes.filter(indepValIndexes.indexOf(_) < 0)
          val filteredLessee = lastIndexes.filter(lesseeIndexes.indexOf(_) < 0)
          if (filtered.isEmpty && filteredWhyHeld.isEmpty && filteredLessee.isEmpty) {
            1
          } else {
            if (filtered.nonEmpty) {
              filtered.head + 1 // index based on last page missing
            } else if (filteredWhyHeld.nonEmpty) {
              filteredWhyHeld.head + 1 // index based on whyHeld and indepVal indexes
            } else {
              filteredLessee.head + 1 // index based on lessee and indepVal indexes
            }
          }
        }
    }

  def getLandOrPropertyDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val atLeastOneCompleted =
      userAnswers.get(LandPropertyDisposalCompletedPages(srn)).exists(_.values.exists(_.values.nonEmpty))
    val started = userAnswers.get(LandOrPropertyDisposalPage(srn)).contains(true)
    val completedNoDisposals = userAnswers.get(LandOrPropertyDisposalPage(srn)).contains(false)

    val initialDisposalUrl = controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val disposalListPage = controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    if (atLeastOneCompleted) (TaskListStatus.Completed, disposalListPage)
    else if (completedNoDisposals) (TaskListStatus.Completed, initialDisposalUrl)
    else if (started) (TaskListStatus.InProgress, initialDisposalUrl)
    else (TaskListStatus.NotStarted, initialDisposalUrl)
  }

  def getBorrowingTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val moneyBorrowedPageUrl =
      controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, NormalMode).url
    val listPageUrl =
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, 1, NormalMode).url

    def lenderNamePageUrl(index: Max5000) =
      controllers.nonsipp.moneyborrowed.routes.LenderNameController.onPageLoad(srn, index, NormalMode).url

    val moneyBorrowedPage = userAnswers.get(MoneyBorrowedPage(srn))
    val firstPages = userAnswers.get(LenderNamePages(srn))
    val lastPages = userAnswers.get(WhySchemeBorrowedMoneyPages(srn))
    (
      moneyBorrowedPage,
      firstPages,
      lastPages
    ) match {
      case (None, _, _) => (NotStarted, moneyBorrowedPageUrl)
      case (
          Some(moneyBorrowed),
          firstPages,
          lastPages
          ) =>
        if (!moneyBorrowed) {
          (Completed, listPageUrl)
        } else {
          val countFirstPages = firstPages.getOrElse(List.empty).size
          val countLastPages = lastPages.getOrElse(List.empty).size

          val incompleteIndex: Int = getIncompleteIndex(firstPages, lastPages)
          val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
            _ => listPageUrl,
            index => lenderNamePageUrl(index)
          )

          if (countFirstPages + countLastPages == 0) {
            (InProgress, lenderNamePageUrl(refineMV(1)))
          } else if (countFirstPages > countLastPages) {
            (InProgress, inProgressCalculatedUrl)
          } else {
            (Completed, listPageUrl)
          }
        }
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

  private def getDisposalIncompleteIndex[A, B](
    firstPages: Option[Map[String, Map[String, A]]],
    lastPages: Option[Map[String, Map[String, B]]]
  ): (Int, Int) =
    (firstPages, lastPages) match {
      case (None, _) => (1, 1)
      case (Some(_), None) => (1, 1)
      case (Some(first), Some(last)) =>
        if (first.isEmpty) {
          (1, 1)
        } else {
          val firstPageIndexes = first.map(x => x._2.map(_._1.toInt)).zipWithIndex.toList
          val lastPageIndexes = last.map(x => x._2.map(_._1.toInt)).zipWithIndex.toList
          val filtered = firstPageIndexes.filter(lastPageIndexes.indexOf(_) < 0)

          if (filtered.isEmpty) {
            (0, 0)
          } else {
            // compare second index
            val firstPageDisposalIndexes = filtered.head._1.toList
            val firstPageIndex = filtered.head._2
            val lastPageDisposalIndexes = lastPageIndexes(firstPageIndex)._1.toList
            val diff = firstPageDisposalIndexes.filter(lastPageDisposalIndexes.indexOf(_) < 0)
            (firstPageIndex + 1, diff.head + 1)
          }
        }
    }

  def getSharesTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val defaultLink =
      controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController
        .onPageLoad(srn, NormalMode)
        .url
    val sharesListPageUrl =
      controllers.nonsipp.shares.routes.SharesListController
        .onPageLoad(srn, 1, NormalMode)
        .url
    val hadSharesPage = userAnswers.get(DidSchemeHoldAnySharesPage(srn))
    val shareStatusPage = userAnswers.get(SharesJourneyStatus(srn))
    val firstPages = userAnswers.get(TypeOfSharesHeldPages(srn))
    val lastPages = userAnswers.map(SharesCompleted.all(srn))
    val incompleteIndex: Int = getIncompleteIndex(firstPages, Some(lastPages))

    val inProgressCalculatedUrl = refineV[OneTo5000](incompleteIndex).fold(
      _ => sharesListPageUrl,
      index => controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, index, NormalMode).url
    )

    (hadSharesPage, shareStatusPage) match {
      case (None, _) => (NotStarted, defaultLink)
      case (Some(false), _) => (Completed, defaultLink)
      case (Some(true), None) => (InProgress, inProgressCalculatedUrl)
      case (Some(true), Some(SectionStatus.Completed)) => (Completed, sharesListPageUrl)
      case (Some(true), Some(SectionStatus.InProgress)) => (InProgress, inProgressCalculatedUrl)
    }
  }

  def getSharesDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {

    val didSchemeDisposePage = controllers.nonsipp.sharesdisposal.routes.SharesDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val sharesToDisposePage = controllers.nonsipp.sharesdisposal.routes.SharesDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    val reportedDisposalsPage = controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
      .onPageLoad(srn, page = 1)
      .url

    val anyJourneysCompleted = userAnswers
      .map(SharesDisposalProgress.all(srn))
      .values
      .exists(_.values.exists(_.completed))

    val (status, link) =
      (userAnswers.get(SharesDisposalPage(srn)), userAnswers.get(SharesDisposalCompleted(srn))) match {
        case (None, _) => (NotStarted, didSchemeDisposePage)
        case (Some(true), Some(_)) => (TaskListStatus.Completed, reportedDisposalsPage)
        case (Some(true), None) if anyJourneysCompleted => (TaskListStatus.Completed, reportedDisposalsPage)
        case (Some(true), None) => (TaskListStatus.InProgress, sharesToDisposePage)
        case (Some(false), _) => (TaskListStatus.Completed, didSchemeDisposePage)
      }

    (status, link)
  }

  def getQuotedSharesTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val totalSharesPage = userAnswers.get(TotalValueQuotedSharesPage(srn))
    val defaultLink =
      controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesController
        .onPageLoad(srn)
        .url
    totalSharesPage match {
      case None => (NotStarted, defaultLink)
      case Some(_) => (Completed, defaultLink)
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

  def getBondsDisposalsTaskListStatusWithLink(
    userAnswers: UserAnswers,
    srn: Srn,
    mode: Mode
  ): (TaskListStatus, String) = {
    val atLeastOneCompleted =
      userAnswers.get(BondsDisposalCompletedPages(srn)).exists(_.values.exists(_.values.nonEmpty))
    val started = userAnswers.get(BondsDisposalPage(srn)).contains(true)
    val completedNoDisposals = userAnswers.get(BondsDisposalPage(srn)).contains(false)

    val initialDisposalUrl = controllers.nonsipp.bondsdisposal.routes.BondsDisposalController
      .onPageLoad(srn, NormalMode)
      .url

    val disposalListPage = controllers.nonsipp.bondsdisposal.routes.BondsDisposalListController
      .onPageLoad(srn, page = 1, mode)
      .url

    if (atLeastOneCompleted) {
      (TaskListStatus.Completed, disposalListPage)
    } else if (completedNoDisposals) {
      (TaskListStatus.Completed, initialDisposalUrl)
    } else if (started) {
      (TaskListStatus.InProgress, initialDisposalUrl)
    } else {
      (TaskListStatus.NotStarted, initialDisposalUrl)
    }
  }

  def getOtherAssetsTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val hadAssetsPage = userAnswers.get(OtherAssetsHeldPage(srn))
    val defaultLink =
      controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
        .onPageLoad(srn, NormalMode)
        .url
    hadAssetsPage match {
      case None => (NotStarted, defaultLink)
      case Some(hadAssets) =>
        if (!hadAssets) {
          (Completed, defaultLink)
        } else {
          (InProgress, defaultLink)
        }
    }
  }

}
