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

import models.ConditionalYesNo._
import models.SchemeId.Srn
import models.{IdentitySubject, NormalMode, PensionSchemeId, SchemeHoldLandProperty, UserAnswers}
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

  def getLandOrPropertyTaskListStatus(userAnswers: UserAnswers, srn: Srn) = {
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
      case (None, _, _, _, _, _, _) => NotStarted
      case (
          Some(landOrPropertyHeld),
          firstPage,
          lastPages,
          firstLesseeSubjourney,
          lastLesseeSubjourney,
          whyHeld,
          indepVal
          ) =>
        if (!landOrPropertyHeld) {
          Completed
        } else {
          val countFirstPages = firstPage.getOrElse(List.empty).size
          val countLastPages = lastPages.getOrElse(List.empty).size
          val countLastLesseeSubjourney = lastLesseeSubjourney.getOrElse(List.empty).size
          val countFirstLesseeSubjourney = firstLesseeSubjourney.getOrElse(List.empty).filter(b => !b._2).size
          val countAcquisitionContributionSubJourney =
            whyHeld.getOrElse(List.empty).filter(b => b._2 != SchemeHoldLandProperty.Transfer).size
          val countIndepValPages = indepVal.getOrElse(List.empty).size
          if (countFirstPages + countLastPages == 0) {
            InProgress
          } else if (countFirstPages > countLastPages) {
            InProgress
          } else {
            if ((countLastLesseeSubjourney + countFirstLesseeSubjourney) != countLastPages ||
              countAcquisitionContributionSubJourney != countIndepValPages) {
              InProgress
            } else {
              Completed
            }
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
