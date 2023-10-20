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

import config.Refined.{Max5000, OneTo5000}
import eu.timepit.refined.{refineMV, refineV}
import models.ConditionalYesNo._
import models.SchemeId.Srn
import models.{IdentitySubject, Money, NormalMode, PensionSchemeId, SchemeHoldLandProperty, UserAnswers}
import pages.nonsipp.common.IdentityTypes
import pages.nonsipp.loansmadeoroutstanding.{
  IsIndividualRecipientConnectedPartyPages,
  LoansMadeOrOutstandingPage,
  OutstandingArrearsOnLoanPages,
  RecipientSponsoringEmployerConnectedPartyPages
}
import pages.nonsipp.landorproperty.{
  IsLandPropertyLeasedPages,
  IsLesseeConnectedPartyPages,
  LandOrPropertyHeldPage,
  LandOrPropertyTotalIncomePages,
  LandPropertyInUKPages,
  LandPropertyIndependentValuationPages,
  WhyDoesSchemeHoldLandPropertyPages
}
import pages.nonsipp.landorpropertydisposal.{LandOrPropertyDisposalPage, LandPropertyDisposalCompletedPages}
import pages.nonsipp.memberdetails.{MemberDetailsNinoPages, MembersDetailsPages, NoNinoPages}
import pages.nonsipp.schemedesignatory.{FeesCommissionsWagesSalariesPage, HowManyMembersPage, HowMuchCashPage}
import viewmodels.models.TaskListStatus
import viewmodels.models.TaskListStatus.{Completed, InProgress, NotStarted, TaskListStatus}

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

  def getFinancialDetailsTaskListStatus(userAnswers: UserAnswers, srn: Srn) = {
    val totalSalaries = userAnswers.get(FeesCommissionsWagesSalariesPage(srn, NormalMode))
    val howMuchCash = userAnswers.get(HowMuchCashPage(srn, NormalMode))
    (howMuchCash, totalSalaries) match {
      case (Some(_), Some(_)) => Completed
      case (None, _) => NotStarted
      case (Some(_), None) => InProgress
    }
  }

  def getMembersTaskListStatus(userAnswers: UserAnswers, srn: Srn) = {
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

  def getLoansTaskListStatus(userAnswers: UserAnswers, srn: Srn) = {
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

  def getLandOrPropertyTaskListStatusAndLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
    val heldPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode).url
    val listPageUrl =
      controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, NormalMode).url
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
          (Completed, listPageUrl)
        } else {
          val countFirstPages = firstPages.getOrElse(List.empty).size
          val countLastPages = lastPages.getOrElse(List.empty).size
          val countLastLesseeSubjourney = lastLesseeSubjourney.getOrElse(List.empty).size
          val countFirstLesseeSubjourney = firstLesseeSubjourney.getOrElse(List.empty).filter(b => !b._2).size
          val countAcquisitionContributionSubJourney =
            whyHeldPages.getOrElse(List.empty).filter(b => b._2 != SchemeHoldLandProperty.Transfer).size
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
            (InProgress, inUkPageUrl(refineMV(1))) // index ok
          } else if (countFirstPages > countLastPages) {
            (InProgress, inProgressCalculatedUrl) //Calculated here!
          } else {
            if ((countLastLesseeSubjourney + countFirstLesseeSubjourney) != countLastPages ||
              countAcquisitionContributionSubJourney != countIndepValPages) {
              (InProgress, inProgressCalculatedUrl) //Calculated here!
            } else {
              (Completed, listPageUrl) // no index
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
          val firstIndexes = (0 to first.size - 1).toList
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
              filtered(0) + 1 // index based on last page missing
            } else if (filteredWhyHeld.nonEmpty) {
              filteredWhyHeld(0) + 1 // index based on whyHeld and indepVal indexes
            } else {
              filteredLessee(0) + 1 // index based on lessee and indepVal indexes
            }
          }
        }
    }

  def getDisposalsTaskListStatusWithLink(userAnswers: UserAnswers, srn: Srn): (TaskListStatus, String) = {
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
}
