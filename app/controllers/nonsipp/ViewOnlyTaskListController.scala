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

import services.{PsrRetrievalService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import cats.implicits.toShow
import controllers.actions._
import viewmodels.models.TaskListStatus._
import views.html.TaskListView
import models.SchemeId.Srn
import pages.nonsipp.WhichTaxYearPage
import play.api.libs.json._
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

class ViewOnlyTaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  saveService: SaveService,
  psrRetrievalService: PsrRetrievalService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(WhichTaxYearPage(srn)) match {
        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(dateRange: DateRange) =>
          val other: Int = if (previous == 0) current else previous
          for {
            currentReturn <- psrRetrievalService.getStandardPsrDetails(
              None,
              Some(year),
              Some("%03d".format(current)),
              controllers.routes.OverviewController.onPageLoad(srn)
            )
            _ <- saveService.save(currentReturn)
            previousReturn <- psrRetrievalService.getStandardPsrDetails(
              None,
              Some(year),
              Some("%03d".format(other)),
              controllers.routes.OverviewController.onPageLoad(srn)
            )
            viewModel = ViewOnlyTaskListController.viewModel(
              srn,
              request.schemeDetails.schemeName,
              dateRange,
              currentReturn,
              previousReturn,
              current
            )
          } yield Ok(view(viewModel))
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
    version: Int
  ): PageViewModel[TaskListViewModel] = {

    val historyLink =
      LinkMessage(
        Message("nonsipp.tasklist.history"),
        controllers.routes.ReturnsSubmittedController.onPageLoad(srn, 1).url
      )

    val sectionListWithoutDeclaration = List(
      schemeDetailsSection(schemeName, currentUA, previousUA),
      membersSection(schemeName, currentUA, previousUA),
      memberPaymentsSection(currentUA, previousUA),
      loansSection(schemeName, currentUA, previousUA),
      sharesSection(currentUA, previousUA),
      landOrPropertySection(currentUA, previousUA),
      bondsSection(currentUA, previousUA),
      otherAssetsSection(currentUA, previousUA)
    )

    val declarationSectionViewModel = declarationSection(srn, schemeName, dateRange, version)

    val viewModel = TaskListViewModel(
      true,
      Some(historyLink),
      sectionListWithoutDeclaration.head,
      sectionListWithoutDeclaration.tail :+ declarationSectionViewModel: _*
    )

    val (numberOfCompleted, numberOfTotal) = evaluateCompletedTotalTuple(viewModel.sections.toList)

    PageViewModel(
      Message("nonsipp.tasklist.title", dateRange.from.show, dateRange.to.show),
      Message("nonsipp.tasklist.heading", dateRange.from.show, dateRange.to.show),
      viewModel
    ).withDescription(
      Heading2.small("nonsipp.tasklist.subheading.completed") ++
        ParagraphMessage(Message("nonsipp.tasklist.description", numberOfCompleted, numberOfTotal))
    )
  }

  private def evaluateCompletedTotalTuple(sections: List[TaskListSectionViewModel]): (Int, Int) = {
    val items = sections.flatMap(_.items.fold(_ => Nil, _.toList))
    val numberOfCompleted = items.count(_.status == Completed)
    val numberOfUpdated = items.count(_.status == Updated)
    val numberOfTotal = items.length
    (numberOfCompleted + numberOfUpdated, numberOfTotal)
  }

  private def messageKey(prefix: String, suffix: String): String = s"$prefix.view.$suffix"

  private def getCompletedOrUpdatedTaskListStatus(
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

// TODO: implement lower-level journey navigation in future ticket - until then, Unauthorised page used for all links

//--Scheme details----------------------------------------------------------------------------------------------------//

  private def schemeDetailsSection(
    schemeName: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.schemedetails"
    TaskListSectionViewModel(
      s"$prefix.title",
      getBasicSchemeDetailsTaskListItem(schemeName, prefix, currentUA, previousUA),
      getFinancialDetailsTaskListItem(schemeName, prefix, currentUA, previousUA)
    )
  }

  private def getBasicSchemeDetailsTaskListItem(
    schemeName: String,
    prefix: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers
  ): TaskListItemViewModel = {

    val accountingPeriodsSame = currentUA.get(JsPath \ "accountingPeriods") == previousUA.get(
      JsPath \ "accountingPeriods"
    )

    val designatoryCurrent = currentUA
      .get(JsPath \ "schemeDesignatory")
      .get
      .as[JsObject] - "totalAssetValue" - "totalPayments" - "totalCash"
    val designatoryPrevious = previousUA
      .get(JsPath \ "schemeDesignatory")
      .get
      .as[JsObject] - "totalAssetValue" - "totalPayments" - "totalCash"
    val schemeDesignatoriesSame = designatoryCurrent == designatoryPrevious

    val basicDetailsTaskListStatus = if (accountingPeriodsSame && schemeDesignatoriesSame) {
      Completed
    } else {
      Updated
    }

    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "details.title"), schemeName),
        controllers.routes.UnauthorisedController.onPageLoad().url
      ),
      basicDetailsTaskListStatus
    )
  }

  private def getFinancialDetailsTaskListItem(
    schemeName: String,
    prefix: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers
  ): TaskListItemViewModel = {

    val financialDetailsTaskListStatus =
      if (currentUA.get(JsPath \ "schemeDesignatory" \ "totalAssetValue") ==
          previousUA.get(JsPath \ "schemeDesignatory" \ "totalAssetValue")
        &&
        currentUA.get(JsPath \ "schemeDesignatory" \ "totalPayments") ==
          previousUA.get(JsPath \ "schemeDesignatory" \ "totalPayments")
        &&
        currentUA.get(JsPath \ "schemeDesignatory" \ "totalCash") ==
          previousUA.get(JsPath \ "schemeDesignatory" \ "totalCash")) {
        Completed
      } else {
        Updated
      }

    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "finances.title"), schemeName),
        controllers.routes.UnauthorisedController.onPageLoad().url
      ),
      financialDetailsTaskListStatus
    )
  }

//--Members-----------------------------------------------------------------------------------------------------------//

  private def membersSection(
    schemeName: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.members"
    val membersTaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      JsPath \ "membersPayments" \ "memberDetails" \ "personalDetails"
    )

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "details.title"), schemeName),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        membersTaskListStatus
      )
    )
  }

//--Member payments---------------------------------------------------------------------------------------------------//

  private def memberPaymentsSection(currentUA: UserAnswers, previousUA: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.memberpayments"

    val employerContributionsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.employercontributions.Paths.memberEmpContribution
    )

    val unallocatedEmployerContributionsTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      JsPath \ "membersPayments" \ "unallocatedContribAmount"
    )

    val memberContributionTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.membercontributions.Paths.memberDetails \ "totalMemberContribution"
    )

    val transferInTaskListStatus: TaskListStatus =
      if (currentUA.get(pages.nonsipp.receivetransfer.Paths.memberTransfersIn) ==
          previousUA.get(pages.nonsipp.receivetransfer.Paths.memberTransfersIn)
        &&
        currentUA.get(JsPath \ "dateOfTransfer") ==
          previousUA.get(JsPath \ "dateOfTransfer")
        &&
        currentUA.get(JsPath \ "transferIncludedAsset") ==
          previousUA.get(JsPath \ "transferIncludedAsset")) {
        Completed
      } else {
        Updated
      }

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
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        employerContributionsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unallocatedcontributions.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        unallocatedEmployerContributionsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "memberContributions.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        memberContributionTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersreceived.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        transferInTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersout.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        transferOutTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "pcls.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        pclsMemberTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "payments.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        pensionPaymentsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "surrenderedbenefits.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        surrenderedBenefitsTaskListStatus
      )
    )
  }

//--Loans made and money borrowed-------------------------------------------------------------------------------------//

  private def loansSection(
    schemeName: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers
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
          Message(messageKey(prefix, "loansmade.title"), schemeName),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        loansTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "moneyborrowed.title"), schemeName),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        borrowingTaskListStatus
      )
    )
  }

//--Shares------------------------------------------------------------------------------------------------------------//

  private def sharesSection(currentUA: UserAnswers, previousUA: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.shares"

    val sharesTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.shares.Paths.shareTransactions,
      Some("disposedSharesTransaction")
    )

    val shareDisposalTaskListStatus: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      pages.nonsipp.sharesdisposal.Paths.disposedSharesTransaction
    )

    val quotedSharesStatusAndLink: TaskListStatus = getCompletedOrUpdatedTaskListStatus(
      currentUA,
      previousUA,
      JsPath \ "totalValueQuotedShares"
    )

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "sponsoringemployer.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        sharesTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.sharesdisposal", "title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        shareDisposalTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "quotedshares.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        quotedSharesStatusAndLink
      )
    )
  }

//--Land or property--------------------------------------------------------------------------------------------------//

  private def landOrPropertySection(currentUA: UserAnswers, previousUA: UserAnswers): TaskListSectionViewModel = {
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

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        landOrPropertyTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.landorpropertydisposal", "title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        landOrPropertyDisposalTaskListStatus
      )
    )
  }

//--Bonds-------------------------------------------------------------------------------------------------------------//

  private def bondsSection(currentUA: UserAnswers, previousUA: UserAnswers): TaskListSectionViewModel = {
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

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unregulatedorconnected.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        bondsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "bondsdisposal.title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        bondsDisposalTaskListStatus
      )
    )
  }

//--Other assets------------------------------------------------------------------------------------------------------//

  private def otherAssetsSection(currentUA: UserAnswers, previousUA: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.otherassets"

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

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        otherAssetsTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.otherassetsdisposal", "title"),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        otherAssetsDisposalTaskListStatus
      )
    )
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