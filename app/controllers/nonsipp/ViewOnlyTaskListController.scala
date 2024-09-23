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

import pages.nonsipp.bonds.BondsCompleted
import viewmodels.implicits._
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils._
import pages.nonsipp.landorproperty.LandOrPropertyCompleted
import cats.implicits.toShow
import controllers.actions._
import viewmodels.models.TaskListStatus._
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.shares.{DidSchemeHoldAnySharesPage, SharesCompleted}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.OtherAssetsCompleted
import com.google.inject.Inject
import cats.data.NonEmptyList
import views.html.TaskListView
import models.SchemeId.Srn
import pages.nonsipp.{CompilationOrSubmissionDatePage, WhichTaxYearPage}
import play.api.Logger
import utils.nonsipp.TaskListUtils.evaluateReadyForSubmissionTotalTuple
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.Future

class ViewOnlyTaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView
) extends PSRController
    with I18nSupport {

  private val logger = Logger(getClass)

  def onPageLoad(srn: Srn, year: String, currentVersion: Int, previousVersion: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, currentVersion, previousVersion).async { implicit request =>
      request.userAnswers.get(WhichTaxYearPage(srn)) match {
        case None =>
          logger.error(s"WhichTaxYearPage not found for srn $srn, redirecting to journey recovery")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(dateRange: DateRange) =>
          val currentReturn = request.userAnswers
          val optPreviousReturn = if (previousVersion == 0) Some(request.userAnswers) else request.previousUserAnswers
          optPreviousReturn match {
            case None =>
              logger.error(s"previousVersion is $previousVersion but previousUserAnswers is empty")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            case Some(previousReturn) =>
              val viewModel = ViewOnlyTaskListController.viewModel(
                srn,
                request.schemeDetails.schemeName,
                dateRange,
                currentReturn,
                previousReturn,
                year,
                currentVersion,
                previousVersion
              )
              Future.successful(Ok(view(viewModel, request.schemeDetails.schemeName)))
          }
      }
    }
}

object ViewOnlyTaskListController {

  def viewModel(
    srn: Srn,
    schemeName: String,
    dateRange: DateRange,
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): PageViewModel[TaskListViewModel] = {

    val historyLink =
      LinkMessage(
        Message("nonsipp.tasklist.history"),
        controllers.routes.ReturnsSubmittedController.onPageLoad(srn, 1).url
      )

    val submissionDateMessage = currentUA
      .get(CompilationOrSubmissionDatePage(srn))
      .fold(Message(""))(date => Message("site.submittedOn", date.show))

    val sectionListWithoutDeclaration = List(
      schemeDetailsSection(schemeName, currentUA, previousUA, srn, year, currentVersion, previousVersion),
      membersSection(schemeName, currentUA, previousUA, srn, year, currentVersion, previousVersion),
      memberPaymentsSection(currentUA, previousUA, srn, year, currentVersion, previousVersion),
      loansSection(schemeName, currentUA, previousUA, srn, year, currentVersion, previousVersion),
      sharesSection(currentUA, previousUA, srn, year, currentVersion, previousVersion),
      landOrPropertySection(currentUA, previousUA, srn, year, currentVersion, previousVersion),
      bondsSection(currentUA, previousUA, srn, year, currentVersion, previousVersion),
      otherAssetsSection(currentUA, previousUA, srn, year, currentVersion, previousVersion)
    )

    val declarationSectionViewModel = declarationSection(srn, schemeName, dateRange, currentVersion)

    val viewModel = TaskListViewModel(
      false,
      true,
      Some(historyLink),
      submissionDateMessage,
      sectionListWithoutDeclaration.head,
      sectionListWithoutDeclaration.tail :+ declarationSectionViewModel: _*
    )

    val (numSectionsSubmitted, numSectionsTotal) = evaluateReadyForSubmissionTotalTuple(viewModel.sections.toList)

    PageViewModel(
      Message("nonsipp.tasklist.title", dateRange.from.show, dateRange.to.show),
      Message("nonsipp.tasklist.heading", dateRange.from.show, dateRange.to.show),
      viewModel
    ).withDescription(
      Heading2.small("nonsipp.tasklist.subheading.completed")
    )
  }

  private def messageKey(prefix: String, suffix: String): String = s"$prefix.view.$suffix"

//--Scheme details----------------------------------------------------------------------------------------------------//

  private def schemeDetailsSection(
    schemeName: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.schemedetails"
    TaskListSectionViewModel(
      s"$prefix.title",
      getBasicSchemeDetailsTaskListItem(
        schemeName,
        prefix,
        currentUA,
        previousUA,
        srn,
        year,
        currentVersion,
        previousVersion
      ),
      getFinancialDetailsTaskListItem(
        schemeName,
        prefix,
        currentUA,
        previousUA,
        srn,
        year,
        currentVersion,
        previousVersion
      )
    )
  }

  private def getBasicSchemeDetailsTaskListItem(
    schemeName: String,
    prefix: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "details.title")),
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
          .onPageLoadViewOnly(srn, year, currentVersion, previousVersion)
          .url
      ),
      getBasicDetailsCompletedOrUpdated(currentUA, previousUA)
    )

  private def getFinancialDetailsTaskListItem(
    schemeName: String,
    prefix: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "finances.title")),
        controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
          .onPageLoadViewOnly(srn, year, currentVersion, previousVersion)
          .url
      ),
      getFinancialDetailsCompletedOrUpdated(currentUA, previousUA)
    )

//--Members-----------------------------------------------------------------------------------------------------------//

  private def membersSection(
    schemeName: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.members"
    val membersTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberdetails.Paths.personalDetails
    )

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "details.title")),
          controllers.nonsipp.memberdetails.routes.SchemeMembersListController
            .onPageLoadViewOnly(srn, page = 1, year, currentVersion, previousVersion)
            .url
        ),
        membersTaskListStatus
      )
    )
  }

//--Member payments---------------------------------------------------------------------------------------------------//

  private def memberPaymentsSection(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.memberpayments"

    val employerContributionsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.employercontributions.Paths.memberEmpContribution
    )

    val unallocatedEmployerContributionsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberpayments.Paths.membersPayments \ "unallocatedContribAmount"
    )

    val memberContributionTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.membercontributions.Paths.memberDetails \ "totalMemberContribution"
    )

    val transferInTaskListStatus: TaskListStatus =
      getCompletedOrUpdatedTaskListStatus(
        currentUA,
        previousUA,
        pages.nonsipp.receivetransfer.Paths.memberTransfersIn
      )

    val transferOutTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.membertransferout.Paths.memberTransfersOut
    )

    val pclsMemberTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberreceivedpcls.Paths.memberDetails \ "memberLumpSumReceived"
    )

    val pensionPaymentsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.memberpensionpayments.Paths.memberDetails \ "pensionAmountReceived"
    )

    val surrenderedBenefitsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.membersurrenderedbenefits.Paths.memberPensionSurrender
    )

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "employercontributions.title"),
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        employerContributionsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unallocatedcontributions.title"),
          controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
            .onPageLoadViewOnly(srn, year, currentVersion, previousVersion)
            .url
        ),
        unallocatedEmployerContributionsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "memberContributions.title"),
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        memberContributionTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersreceived.title"),
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        transferInTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersout.title"),
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        transferOutTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "pcls.title"),
          controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        pclsMemberTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "payments.title"),
          controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        pensionPaymentsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "surrenderedbenefits.title"),
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        surrenderedBenefitsTaskListStatus
      )
    )
  }

//--Loans made and money borrowed-------------------------------------------------------------------------------------//

  private def loansSection(
    schemeName: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = s"nonsipp.tasklist.loans"
    val loansTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.loansmadeoroutstanding.Paths.loans
    )
    val borrowingTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.moneyborrowed.Paths.borrowing
    )

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "loansmade.title")),
          controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        loansTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "moneyborrowed.title")),
          controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
            .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
            .url
        ),
        borrowingTaskListStatus
      )
    )
  }

//--Shares------------------------------------------------------------------------------------------------------------//

  private def sharesSection(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.shares"

    val sharesTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.shares.Paths.shareTransactions,
      Some("disposedSharesTransaction")
    )

    val sharesDisposalsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.sharesdisposal.Paths.disposedSharesTransaction
    )

    val sharesItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "sponsoringemployer.title"),
        controllers.nonsipp.shares.routes.SharesListController
          .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
          .url
      ),
      sharesTaskListStatus
    )

    val sharesDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey("nonsipp.tasklist.sharesdisposal", "title"),
        controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
          .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
          .url
      ),
      sharesDisposalsTaskListStatus
    )

    val currentSharesCompleted = currentUA.get(SharesCompleted.all(srn)).filter(_.nonEmpty)
    val previousSharesCompleted = previousUA.get(SharesCompleted.all(srn)).filter(_.nonEmpty)

    val viewModelList = NonEmptyList
      .fromList((currentSharesCompleted, previousSharesCompleted) match {
        case (Some(_), _) => List(sharesItem, sharesDisposalItem)
        case (_, Some(_)) => List(sharesItem, sharesDisposalItem)
        case (_, _) => List(sharesItem)
      })
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

//--Land or property--------------------------------------------------------------------------------------------------//

  private def landOrPropertySection(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.landorproperty"

    val landOrPropertyTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.landorproperty.Paths.landOrPropertyTransactions,
      Some("disposedPropertyTransaction")
    )

    val landOrPropertyDisposalTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.landorpropertydisposal.Paths.disposalPropertyTransaction
    )

    val landOrPropertyItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "title"),
        controllers.nonsipp.landorproperty.routes.LandOrPropertyListController
          .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
          .url
      ),
      landOrPropertyTaskListStatus
    )

    val landOrPropertyDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey("nonsipp.tasklist.landorpropertydisposal", "title"),
        controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
          .onPageLoadViewOnly(srn, page = 1, year, currentVersion, previousVersion)
          .url
      ),
      landOrPropertyDisposalTaskListStatus
    )

    val currentLandOrPropertyCompleted = currentUA.get(LandOrPropertyCompleted.all(srn)).filter(_.nonEmpty)
    val previousLandOrPropertyCompleted = previousUA.get(LandOrPropertyCompleted.all(srn)).filter(_.nonEmpty)

    val viewModelList = NonEmptyList
      .fromList((currentLandOrPropertyCompleted, previousLandOrPropertyCompleted) match {
        case (Some(_), _) => List(landOrPropertyItem, landOrPropertyDisposalItem)
        case (_, Some(_)) => List(landOrPropertyItem, landOrPropertyDisposalItem)
        case (_, _) => List(landOrPropertyItem)
      })
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

//--Bonds-------------------------------------------------------------------------------------------------------------//

  private def bondsSection(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.bonds"

    val bondsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.bonds.Paths.bondTransactions,
      Some("bondsDisposed")
    )

    val bondsDisposalTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.bondsdisposal.Paths.bondsDisposed
    )

    val bondsItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "unregulatedorconnected.title"),
        controllers.nonsipp.bonds.routes.BondsListController
          .onPageLoadViewOnly(srn, page = 1, year, currentVersion, previousVersion)
          .url
      ),
      bondsTaskListStatus
    )

    val bondsDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "bondsdisposal.title"),
        controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
          .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
          .url
      ),
      bondsDisposalTaskListStatus
    )

    val currentBondsCompleted = currentUA.get(BondsCompleted.all(srn)).filter(_.nonEmpty)
    val previousBondsCompleted = previousUA.get(BondsCompleted.all(srn)).filter(_.nonEmpty)

    val viewModelList = NonEmptyList
      .fromList((currentBondsCompleted, previousBondsCompleted) match {
        case (Some(_), _) => List(bondsItem, bondsDisposalItem)
        case (_, Some(_)) => List(bondsItem, bondsDisposalItem)
        case (_, _) => List(bondsItem)
      })
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

//--Other assets------------------------------------------------------------------------------------------------------//

  private def otherAssetsSection(
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    srn: Srn,
    year: String,
    currentVersion: Int,
    previousVersion: Int
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.otherassets"

    val quotedSharesTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.totalvaluequotedshares.Paths.quotedShares \ "totalValueQuotedShares"
    )

    val otherAssetsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.otherassetsheld.Paths.otherAssetsTransactions,
      Some("assetsDisposed")
    )

    val otherAssetsDisposalTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.otherassetsdisposal.Paths.assetsDisposed
    )

    val quotedSharesItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "quotedshares.title"),
        controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
          .onPageLoadViewOnly(srn, year, currentVersion, previousVersion)
          .url
      ),
      quotedSharesTaskListStatus
    )

    val otherAssetsItem = TaskListItemViewModel(
      LinkMessage(
        messageKey(prefix, "title"),
        controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
          .onPageLoadViewOnly(srn, page = 1, year, currentVersion, previousVersion)
          .url
      ),
      otherAssetsTaskListStatus
    )

    val otherAssetsDisposalItem = TaskListItemViewModel(
      LinkMessage(
        messageKey("nonsipp.tasklist.otherassetsdisposal", "title"),
        controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
          .onPageLoadViewOnly(srn, 1, year, currentVersion, previousVersion)
          .url
      ),
      otherAssetsDisposalTaskListStatus
    )

    val sharesExist = currentUA.get(DidSchemeHoldAnySharesPage(srn)).isDefined || previousUA
      .get(DidSchemeHoldAnySharesPage(srn))
      .isDefined

    val currentOtherAssetsCompleted = currentUA.get(OtherAssetsCompleted.all(srn)).filter(_.nonEmpty)
    val previousOtherAssetsCompleted = previousUA.get(OtherAssetsCompleted.all(srn)).filter(_.nonEmpty)

    val viewModelList = NonEmptyList
      .fromList((sharesExist, currentOtherAssetsCompleted.orElse(previousOtherAssetsCompleted)) match {
        case (true, Some(_)) => List(quotedSharesItem, otherAssetsItem, otherAssetsDisposalItem)
        case (true, None) => List(quotedSharesItem, otherAssetsItem)
        case (false, Some(_)) => List(otherAssetsItem, otherAssetsDisposalItem)
        case (false, None) => List(otherAssetsItem)
      })
      .get

    TaskListSectionViewModel(s"$prefix.title", Right(viewModelList), None)
  }

//--Declaration-------------------------------------------------------------------------------------------------------//

  private def declarationSection(
    srn: Srn,
    schemeName: String,
    dateRange: DateRange,
    version: Int
  ): TaskListSectionViewModel = {
    val prefix = s"nonsipp.tasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      LinkMessage(
        Message("nonsipp.tasklist.declaration.view"),
        controllers.nonsipp.routes.ViewOnlyReturnSubmittedController
          .onPageLoad(srn, dateRange.from.toString, version)
          .url
      ),
      LinkMessage(
        Message(s"$prefix.saveandreturn", schemeName),
        controllers.routes.OverviewController.onPageLoad(srn).url
      )
    )
  }
}
