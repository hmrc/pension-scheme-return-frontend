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

import pages.nonsipp.schemedesignatory._
import pages.nonsipp.bonds.BondsCompleted
import viewmodels.implicits._
import pages.nonsipp.shares.{DidSchemeHoldAnySharesPage, SharesCompleted}
import pages.nonsipp.otherassetsheld.OtherAssetsCompleted
import utils.nonsipp.TaskListStatusUtils._
import pages.nonsipp.landorproperty.LandOrPropertyCompleted
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.CheckReturnDatesPage
import models._
import viewmodels.models.TaskListStatus._
import cats.data.NonEmptyList
import models.SchemeId.Srn
import viewmodels.DisplayMessage._
import viewmodels.models._

object TaskListUtils {

  private def getSectionListWithoutDeclaration(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId
  ): List[TaskListSectionViewModel] =
    List(
      schemeDetailsSection(srn, schemeName, userAnswers, pensionSchemeId),
      membersSection(srn, schemeName, userAnswers),
      memberPaymentsSection(srn, userAnswers),
      loansSection(srn, schemeName, userAnswers),
      sharesSection(srn, userAnswers),
      landOrPropertySection(srn, userAnswers),
      bondsSection(srn, userAnswers),
      otherAssetsSection(srn, userAnswers)
    )

  private def getDeclarationSection(
    srn: Srn,
    isPsp: Boolean,
    isLinkActive: Boolean,
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isLinkActive) {
        val psaOrPspDeclarationUrl =
          if (isPsp) {
            controllers.nonsipp.declaration.routes.PspDeclarationController.onPageLoad(srn).url
          } else {
            controllers.nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn).url
          }
        LinkMessage(
          s"$prefix.complete",
          psaOrPspDeclarationUrl
        )
      } else {
        Message(s"$prefix.incomplete")
      },
      LinkMessage(
        Message(s"$prefix.saveandreturn", schemeName),
        controllers.routes.OverviewController.onPageLoad(srn).url
      )
    )
  }

  def getSectionList(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId
  ): List[TaskListSectionViewModel] = {

    val sectionListWithoutDeclaration = getSectionListWithoutDeclaration(srn, schemeName, userAnswers, pensionSchemeId)

    val (numSectionsReadyForSubmission, numSectionsTotal) = evaluateReadyForSubmissionTotalTuple(
      sectionListWithoutDeclaration
    )

    val isLinkActive = numSectionsTotal == numSectionsReadyForSubmission

    val declarationSectionViewModel =
      getDeclarationSection(
        srn,
        pensionSchemeId.isPSP,
        isLinkActive,
        schemeName
      )

    sectionListWithoutDeclaration :+ declarationSectionViewModel

  }

  private def schemeDetailsSection(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.schemedetails"

    TaskListSectionViewModel(
      s"$prefix.title",
      getBasicSchemeDetailsTaskListItem(srn, schemeName, prefix, userAnswers, pensionSchemeId),
      getFinancialDetailsTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getBasicSchemeDetailsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId
  ): TaskListItemViewModel = {

    val checkReturnDates = userAnswers.get(CheckReturnDatesPage(srn))
    val accountingPeriods = userAnswers.get(AccountingPeriods(srn))
    val activeBankAccount = userAnswers.get(ActiveBankAccountPage(srn))
    val whyNoBankAccount = userAnswers.get(WhyNoBankAccountPage(srn))
    val howManyMembers = userAnswers.get(HowManyMembersPage(srn, pensionSchemeId))

    val taskListStatus: TaskListStatus =
      getBasicDetailsTaskListStatus(
        checkReturnDates,
        accountingPeriods,
        activeBankAccount,
        whyNoBankAccount,
        howManyMembers
      )

    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "details.title", taskListStatus), schemeName),
        taskListStatus match {
          case Recorded =>
            controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
          case InProgress =>
            if (checkReturnDates.isEmpty) {
              controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
            } else if (!checkReturnDates.get && accountingPeriods.getOrElse(List.empty).isEmpty) {
              controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
            } else if (activeBankAccount.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode).url
            } else if (!activeBankAccount.get && whyNoBankAccount.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode).url
            } else if (howManyMembers.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, NormalMode).url
            } else {
              controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
            }
          case _ =>
            controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      ),
      taskListStatus
    )
  }

  private def getFinancialDetailsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {

    val howMuchCash = userAnswers.get(HowMuchCashPage(srn, NormalMode))
    val valueOfAssets = userAnswers.get(ValueOfAssetsPage(srn, NormalMode))
    val feesCommissionsWagesSalaries = userAnswers.get(FeesCommissionsWagesSalariesPage(srn, NormalMode))

    val taskListStatus: TaskListStatus =
      getFinancialDetailsTaskListStatus(howMuchCash, valueOfAssets, feesCommissionsWagesSalaries)

    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "finances.title", taskListStatus), schemeName),
        taskListStatus match {
          case NotStarted =>
            controllers.nonsipp.schemedesignatory.routes.HowMuchCashController.onPageLoad(srn, NormalMode).url
          case Recorded =>
            controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
              .onPageLoad(srn, NormalMode)
              .url
          case InProgress =>
            if (howMuchCash.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.HowMuchCashController.onPageLoad(srn, NormalMode).url
            } else if (valueOfAssets.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController.onPageLoad(srn, NormalMode).url
            } else {
              controllers.nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController
                .onPageLoad(srn, NormalMode)
                .url
            }
          case _ =>
            controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      ),
      taskListStatus
    )
  }

  private def membersSection(srn: Srn, schemeName: String, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.members"
    val (membersStatus, membersLink) = getMembersTaskListStatusAndLink(userAnswers, srn)
    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "details.title", membersStatus), schemeName),
          membersLink
        ),
        membersStatus
      )
    )
  }

  private def memberPaymentsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.memberpayments"

    // change to check if members section is complete to start
    val (employerContributionStatus, employerContributionLink) =
      getEmployerContributionStatusAndLink(userAnswers, srn)

    val (unallocatedContributionsStatus, unallocatedContributionsLink) =
      getUnallocatedContributionsStatusAndLink(userAnswers, srn)

    val (memberContributionStatus, memberContributionLink) =
      getMemberContributionStatusAndLink(userAnswers, srn)

    val (transferInStatus, transferInLink) =
      getTransferInStatusAndLink(userAnswers, srn)

    val (transferOutStatus, transferOutLink) =
      getTransferOutStatusAndLink(userAnswers, srn)

    val (pclsMemberStatus, pclsMemberLink) =
      getPclsStatusAndLink(userAnswers, srn)

    val (pensionPaymentsStatus, pensionPaymentsLink) =
      getPensionPaymentsStatusAndLink(userAnswers, srn)

    val (surrenderedBenefitsStatus, surrenderedBenefitsLink) =
      getSurrenderedBenefitsStatusAndLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "employercontributions.title", employerContributionStatus),
          employerContributionLink
        ),
        employerContributionStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unallocatedcontributions.title", unallocatedContributionsStatus),
          unallocatedContributionsLink
        ),
        unallocatedContributionsStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "memberContributions.title", memberContributionStatus),
          memberContributionLink
        ),
        memberContributionStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersreceived.title", transferInStatus),
          transferInLink
        ),
        transferInStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersout.title", transferOutStatus),
          transferOutLink
        ),
        transferOutStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "pcls.title", pclsMemberStatus),
          pclsMemberLink
        ),
        pclsMemberStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "payments.title", pensionPaymentsStatus),
          pensionPaymentsLink
        ),
        pensionPaymentsStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "surrenderedbenefits.title", surrenderedBenefitsStatus),
          surrenderedBenefitsLink
        ),
        surrenderedBenefitsStatus
      )
    )
  }

  private def loansSection(srn: Srn, schemeName: String, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = s"nonsipp.tasklist.loans"
    val (loansStatus, loansLink) = getLoansTaskListStatusAndLink(userAnswers, srn)
    val (borrowingStatus, borrowingLink) = getBorrowingTaskListStatusAndLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "loansmade.title", loansStatus), schemeName),
          loansLink
        ),
        loansStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "moneyborrowed.title", borrowingStatus), schemeName),
          borrowingLink
        ),
        borrowingStatus
      )
    )
  }

  private def sharesSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.shares"
    val (sharesStatus, sharesLink) = getSharesTaskListStatusAndLink(userAnswers, srn)
    val (sharesDisposalsStatus, sharesDisposalsLinkUrl) =
      TaskListStatusUtils.getSharesDisposalsTaskListStatusWithLink(userAnswers, srn)

    val sharesItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "sponsoringemployer.title", sharesStatus),
        sharesLink
      ),
      sharesStatus
    )

    val sharesDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey("nonsipp.tasklist.sharesdisposal", "title", sharesDisposalsStatus),
        sharesDisposalsLinkUrl
      ),
      sharesDisposalsStatus
    )

    val viewModelList = NonEmptyList
      .fromList(userAnswers.get(SharesCompleted.all(srn)).filter(_.nonEmpty) match {
        case Some(_) => List(sharesItem, sharesDisposalItem)
        case None => List(sharesItem)
      })
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

  private def landOrPropertySection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.landorproperty"

    val landOrPropertyStatus = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(userAnswers, srn)
    val (landOrPropertyDisposalsStatus, landOrPropertyDisposalsLinkUrl) =
      TaskListStatusUtils.getLandOrPropertyDisposalsTaskListStatusWithLink(userAnswers, srn)

    val landOrPropertyItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "title", landOrPropertyStatus._1),
        landOrPropertyStatus._2
      ),
      landOrPropertyStatus._1
    )

    val landOrPropertyDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey("nonsipp.tasklist.landorpropertydisposal", "title", UnableToStart),
        landOrPropertyDisposalsLinkUrl
      ),
      landOrPropertyDisposalsStatus
    )

    val viewModelList = NonEmptyList
      .fromList(userAnswers.get(LandOrPropertyCompleted.all(srn)).filter(_.nonEmpty) match {
        case Some(_) => List(landOrPropertyItem, landOrPropertyDisposalItem)
        case None => List(landOrPropertyItem)
      })
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

  private def bondsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.bonds"
    val (bondStatus, bondLink) = getBondsTaskListStatusAndLink(userAnswers, srn)
    val (disposalStatus, disposalLink) = getBondsDisposalsTaskListStatusWithLink(userAnswers, srn)

    val bondsItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "unregulatedorconnected.title", bondStatus),
        bondLink
      ),
      bondStatus
    )

    val bondsDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "bondsdisposal.title", disposalStatus),
        disposalLink
      ),
      disposalStatus
    )

    val viewModelList = NonEmptyList
      .fromList(userAnswers.get(BondsCompleted.all(srn)).filter(_.nonEmpty) match {
        case Some(_) => List(bondsItem, bondsDisposalItem)
        case None => List(bondsItem)
      })
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

  private def otherAssetsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.otherassets"
    val quotedSharesStatusAndLink = getQuotedSharesTaskListStatusAndLink(userAnswers, srn)
    val (otherAssetsStatus, otherAssetsLink) = getOtherAssetsTaskListStatusAndLink(userAnswers, srn)
    val (otherAssetsDisposalsStatus, otherAssetsDisposalsLinkUrl) =
      TaskListStatusUtils.getOtherAssetsDisposalTaskListStatusAndLink(userAnswers, srn)

    val quotedSharesItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "quotedshares.title", quotedSharesStatusAndLink._1),
        quotedSharesStatusAndLink._2
      ),
      quotedSharesStatusAndLink._1
    )

    val otherAssetsItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "title", otherAssetsStatus),
        otherAssetsLink
      ),
      otherAssetsStatus
    )

    val otherAssetsDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey("nonsipp.tasklist.otherassetsdisposal", "title", otherAssetsDisposalsStatus),
        otherAssetsDisposalsLinkUrl
      ),
      otherAssetsDisposalsStatus
    )

    val viewModelList = NonEmptyList
      .fromList(
        (
          userAnswers.get(OtherAssetsCompleted.all(srn)).filter(_.nonEmpty),
          userAnswers.get(DidSchemeHoldAnySharesPage(srn))
        ) match {
          case (Some(_), Some(_)) => List(quotedSharesItem, otherAssetsItem, otherAssetsDisposalItem)
          case (Some(_), _) => List(otherAssetsItem, otherAssetsDisposalItem)
          case (_, Some(_)) => List(quotedSharesItem, otherAssetsItem)
          case (_, _) => List(otherAssetsItem)
        }
      )
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

  private def messageKey(prefix: String, suffix: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted => s"$prefix.add.$suffix"
      case _ => s"$prefix.change.$suffix"
    }

  def evaluateReadyForSubmissionTotalTuple(sections: List[TaskListSectionViewModel]): (Int, Int) = {
    val items = sections.flatMap(_.items.fold(_ => Nil, _.toList))
    val numSectionsTotal = items.length
    val numSectionsUnableToStart = items.count(_.status == UnableToStart)
    val numSectionsNotStarted = items.count(_.status == NotStarted)
    val numSectionsInProgress = items.count(_.status == InProgress)
    val numSectionsReadyForSubmission =
      numSectionsTotal - (numSectionsUnableToStart + numSectionsNotStarted + numSectionsInProgress)

    (numSectionsReadyForSubmission, numSectionsTotal)
  }
}
