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

package controllers.nonsipp

import services.{PsrVersionsService, SchemeDateService}
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, ValueOfAssetsPage, WhyNoBankAccountPage}
import pages.nonsipp.memberdetails._
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils._
import cats.implicits.toShow
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.CheckReturnDatesPage
import viewmodels.models.TaskListStatus._
import _root_.config.Refined.OneTo300
import views.html.TaskListView
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import utils.nonsipp.TaskListStatusUtils
import models.backend.responses.ReportStatus
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal.BondsDisposalPage
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

class TaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  schemeDateService: SchemeDateService,
  psrVersionsService: PsrVersionsService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    schemeDateService.schemeDate(srn) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(dates) =>
        for {
          response <- psrVersionsService.getVersions(request.schemeDetails.pstr, formatDateForApi(dates.from))
          hasHistory = response
            .filter(
              psrVersionsService =>
                psrVersionsService.reportStatus == ReportStatus.SubmittedAndInProgress
                  || psrVersionsService.reportStatus == ReportStatus.SubmittedAndSuccessfullyProcessed
            )
            .toList
            .nonEmpty
          viewModel = TaskListController.viewModel(
            srn,
            request.schemeDetails.schemeName,
            dates.from,
            dates.to,
            request.userAnswers,
            request.pensionSchemeId,
            hasHistory
          )
        } yield Ok(view(viewModel))
    }
  }
}

object TaskListController {

  def messageKey(prefix: String, suffix: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted => s"$prefix.add.$suffix"
      case _ => s"$prefix.change.$suffix"
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
    val activeBankAccount = userAnswers.get(ActiveBankAccountPage(srn))
    val whyNoBankAccountPage = userAnswers.get(WhyNoBankAccountPage(srn))

    val taskListStatus: TaskListStatus =
      getBasicSchemeDetailsTaskListStatus(srn, userAnswers, pensionSchemeId, activeBankAccount, whyNoBankAccountPage)

    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        taskListStatus match {
          case Completed =>
            controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
          case _ =>
            val checkReturnDates = userAnswers.get(CheckReturnDatesPage(srn))
            lazy val accountingPeriods = userAnswers.get(AccountingPeriods(srn))

            if (checkReturnDates.isEmpty) {
              controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
            } else if (!checkReturnDates.get && accountingPeriods.isEmpty) {
              controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
            } else if (activeBankAccount.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode).url
            } else if (!activeBankAccount.get && whyNoBankAccountPage.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode).url
            } else {
              controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, NormalMode).url
            }
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

    val taskListStatus: TaskListStatus = getFinancialDetailsTaskListStatus(userAnswers, srn)

    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "finances.title", taskListStatus), schemeName),
        taskListStatus match {
          case NotStarted =>
            controllers.nonsipp.schemedesignatory.routes.HowMuchCashController.onPageLoad(srn, NormalMode).url
          case Completed =>
            controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
              .onPageLoad(srn, NormalMode)
              .url
          case _ =>
            val valueOfAssets = userAnswers.get(ValueOfAssetsPage(srn, NormalMode))
            if (valueOfAssets.isEmpty) {
              controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController.onPageLoad(srn, NormalMode).url
            } else {
              controllers.nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController
                .onPageLoad(srn, NormalMode)
                .url
            }
        }
      ),
      taskListStatus
    )
  }

  private def membersSection(srn: Srn, schemeName: String, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.members"
    val taskListStatus = getMembersTaskListStatus(userAnswers, srn)
    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "details.title", taskListStatus), schemeName),
          taskListStatus match {
            case NotStarted =>
              controllers.nonsipp.memberdetails.routes.PensionSchemeMembersController.onPageLoad(srn).url
            case Completed =>
              controllers.nonsipp.memberdetails.routes.SchemeMembersListController
                .onPageLoad(srn, 1, ManualOrUpload.Manual)
                .url
            case _ =>
              val incompleteIndex = getIncompleteMembersIndex(userAnswers, srn)
              refineV[OneTo300](incompleteIndex).fold(
                _ =>
                  controllers.nonsipp.memberdetails.routes.SchemeMembersListController
                    .onPageLoad(srn, 1, ManualOrUpload.Manual)
                    .url,
                index => {
                  val doesMemberHaveNino = userAnswers.get(DoesMemberHaveNinoPage(srn, index))
                  if (doesMemberHaveNino.isEmpty) {
                    controllers.nonsipp.memberdetails.routes.DoesSchemeMemberHaveNINOController
                      .onPageLoad(srn, index, NormalMode)
                      .url
                  } else if (doesMemberHaveNino.getOrElse(false)) {
                    controllers.nonsipp.memberdetails.routes.MemberDetailsNinoController
                      .onPageLoad(srn, index, NormalMode)
                      .url
                  } else {
                    controllers.nonsipp.memberdetails.routes.NoNINOController
                      .onPageLoad(srn, index, NormalMode)
                      .url
                  }
                }
              )
          }
        ),
        taskListStatus
      )
    )
  }

  private def getIncompleteMembersIndex(userAnswers: UserAnswers, srn: Srn): Int = {
    val membersDetailsPages = userAnswers.get(MembersDetailsPages(srn))
    val ninoPages = userAnswers.get(MemberDetailsNinoPages(srn))
    val noNinoPages = userAnswers.get(NoNinoPages(srn))
    (membersDetailsPages, ninoPages, noNinoPages) match {
      case (None, _, _) => 1
      case (Some(_), None, None) => 1
      case (Some(memberDetails), ninos, noNinos) =>
        if (memberDetails.isEmpty) {
          1
        } else {
          val memberDetailsIndexes = memberDetails.map(_._1.toInt).toList
          val ninoIndexes = ninos.getOrElse(List.empty).map(_._1.toInt).toList
          val noninoIndexes = noNinos.getOrElse(List.empty).map(_._1.toInt).toList
          val finishedIndexes = ninoIndexes ++ noninoIndexes
          val filtered = memberDetailsIndexes.filter(finishedIndexes.indexOf(_) < 0)
          if (filtered.isEmpty) {
            1
          } else {
            filtered.head + 1
          }
        }
    }
  }

  private def memberPaymentsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.memberpayments"

    // change to check if members section is complete to start
    val (employerContributionStatus, employerContributionLink) = getEmployerContributionStatusAndLink(userAnswers, srn)

    val (transferInStatus, transferInLink) = getTransferInStatusAndLink(userAnswers, srn)
    val (transferOutStatus, transferOutLink) = getTransferOutStatusAndLink(userAnswers, srn)

    val (memberContributionStatus, memberContributionLink) = getMemberContributionStatusAndLink(userAnswers, srn)
    val (pclsMemberStatus, pclsMemberLink) = getPclsStatusAndLink(userAnswers, srn)

    val (surrenderedBenefitsStatus, surrenderedBenefitsLink) = getSurrenderedBenefitsStatusAndLink(userAnswers, srn)

    val (pensionPaymentsStatus, pensionPaymentsLink) = getPensionPaymentsStatusAndLink(userAnswers, srn)
    val (unallocatedContributionsStatus, unallocatedContributionsLink) =
      getUnallocatedContributionsStatusAndLink(userAnswers, srn)

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
    val taskListStatus: TaskListStatus = getLoansTaskListStatus(userAnswers, srn)
    val borrowingStatus = getBorrowingTaskListStatusAndLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "loansmade.title", taskListStatus), schemeName),
          taskListStatus match {
            case NotStarted =>
              controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
                .onPageLoad(srn, NormalMode)
                .url
            case Completed =>
              controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
                .onPageLoad(srn, 1, NormalMode)
                .url
            case InProgress => getIncompleteLoansLink(userAnswers, srn)
          }
        ),
        taskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "moneyborrowed.title", borrowingStatus._1), schemeName),
          borrowingStatus._2
        ),
        borrowingStatus._1
      )
    )
  }

  private def sharesSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.shares"
    val (sharesStatus, sharesLink) = getSharesTaskListStatusAndLink(userAnswers, srn)
    val quotedSharesStatusAndLink = getQuotedSharesTaskListStatusAndLink(userAnswers, srn)
    val (sharesDisposalsStatus, sharesDisposalsLinkUrl) =
      TaskListStatusUtils.getSharesDisposalsTaskListStatusWithLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "sponsoringemployer.title", sharesStatus),
          sharesLink
        ),
        sharesStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.sharesdisposal", "title", sharesDisposalsStatus),
          sharesDisposalsLinkUrl
        ),
        sharesDisposalsStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "quotedshares.title", quotedSharesStatusAndLink._1),
          quotedSharesStatusAndLink._2
        ),
        quotedSharesStatusAndLink._1
      )
    )
  }

  private def landOrPropertySection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.landorproperty"

    val landOrPropertyStatus = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(userAnswers, srn)
    val (landOrPropertyDisposalsStatus, landOrPropertyDisposalsLinkUrl) =
      TaskListStatusUtils.getLandOrPropertyDisposalsTaskListStatusWithLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "title", landOrPropertyStatus._1),
          landOrPropertyStatus._2
        ),
        landOrPropertyStatus._1
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.landorpropertydisposal", "title", UnableToStart),
          landOrPropertyDisposalsLinkUrl
        ),
        landOrPropertyDisposalsStatus
      )
    )
  }

  private def bondsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.bonds"
    val (bondStatus, bondLink) = getBondsTaskListStatusAndLink(userAnswers, srn)
    val (disposalStatus, disposalLink) = getBondsDisposalsTaskListStatusWithLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unregulatedorconnected.title", bondStatus),
          bondLink
        ),
        bondStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "bondsdisposal.title", disposalStatus),
          disposalLink
        ),
        disposalStatus
      )
    )
  }

  private def otherAssetsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.otherassets"
    val (otherAssetsStatus, otherAssetsLink) = getOtherAssetsTaskListStatusAndLink(userAnswers, srn)
    val (otherAssetsDisposalsStatus, otherAssetsDisposalsLinkUrl) =
      TaskListStatusUtils.getOtherAssetsDisposalTaskListStatusAndLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "title", otherAssetsStatus),
          otherAssetsLink
        ),
        otherAssetsStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.otherassetsdisposal", "title", otherAssetsDisposalsStatus),
          otherAssetsDisposalsLinkUrl
        ),
        otherAssetsDisposalsStatus
      )
    )
  }

  private def declarationSection(
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

  private def evaluateCompletedTotalTuple(sections: List[TaskListSectionViewModel]): (Int, Int) = {
    val items = sections.flatMap(_.items.fold(_ => Nil, _.toList))
    val numberOfCompleted = items.count(_.status == Completed)
    val numberOfTotal = items.length
    (numberOfCompleted, numberOfTotal)
  }

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId,
    hasHistory: Boolean = false
  ): PageViewModel[TaskListViewModel] = {

    val sectionListWithoutDeclaration = List(
      schemeDetailsSection(srn, schemeName, userAnswers, pensionSchemeId),
      membersSection(srn, schemeName, userAnswers),
      memberPaymentsSection(srn, userAnswers),
      loansSection(srn, schemeName, userAnswers),
      sharesSection(srn, userAnswers),
      landOrPropertySection(srn, userAnswers),
      bondsSection(srn, userAnswers),
      otherAssetsSection(srn, userAnswers)
    )

    val (numberOfCompletedWithoutDeclaration, numberOfTotalWithoutDeclaration) = evaluateCompletedTotalTuple(
      sectionListWithoutDeclaration
    )

    // TODO:
    //  These pages must be removed once tasklist status logic implemented for them properly.
    //  Now we're just checking whether user selected no in the beginning of the journey (using shortcut).
    val unhandledStatusPages = List(
      BondsDisposalPage(srn)
    ).map(
        page => userAnswers.get(page).fold(0)(x => if (x) 0 else 1)
      )
      .sum

    val numberOfCompletedPages = numberOfCompletedWithoutDeclaration + unhandledStatusPages

    val isLinkActive = numberOfTotalWithoutDeclaration == numberOfCompletedPages

    val declarationSectionViewModel =
      declarationSection(
        srn,
        pensionSchemeId.isPSP,
        isLinkActive,
        schemeName
      )

    val historyLink = if (hasHistory) {
      Some(
        LinkMessage(
          Message("nonsipp.tasklist.history"),
          controllers.routes.ReturnsSubmittedController.onPageLoad(srn, 1).url
        )
      )
    } else {
      None
    }

    val viewModel = TaskListViewModel(
      hasHistory,
      historyLink,
      sectionListWithoutDeclaration.head,
      sectionListWithoutDeclaration.tail :+ declarationSectionViewModel: _*
    )

    val (numberOfCompleted, numberOfTotal) = evaluateCompletedTotalTuple(viewModel.sections.toList)

    PageViewModel(
      Message("nonsipp.tasklist.title", startDate.show, endDate.show),
      Message("nonsipp.tasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      Heading2.small("nonsipp.tasklist.subheading.incomplete") ++
        ParagraphMessage(Message("nonsipp.tasklist.description", numberOfCompleted, numberOfTotal))
    )
  }
}
