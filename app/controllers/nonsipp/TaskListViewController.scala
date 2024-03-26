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

package controllers.nonsipp

import cats.implicits.toShow
import com.google.inject.Inject
import controllers.actions._
import models.SchemeId.Srn
import models.{ManualOrUpload, NormalMode, PensionSchemeId, UserAnswers}
import pages.nonsipp.membercontributions.MemberContributionsListPage
import pages.nonsipp.memberpayments.UnallocatedEmployerContributionsPage
import pages.nonsipp.memberpensionpayments.{PensionPaymentsJourneyStatus, PensionPaymentsReceivedPage}
import pages.nonsipp.memberreceivedpcls.PclsMemberListPage
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsJourneyStatus
import pages.nonsipp.membertransferout.TransfersOutJourneyStatus
import pages.nonsipp.receivetransfer.TransfersInJourneyStatus
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrRetrievalService, SaveService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import utils.nonsipp.TaskListStatusUtils
import utils.nonsipp.TaskListStatusUtils._
import viewmodels.DisplayMessage.{Heading2, LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.TaskListStatus._
import viewmodels.models.{TaskListStatus, _}
import views.html.TaskListView

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class TaskListViewController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  saveService: SaveService,
  psrRetrievalService: PsrRetrievalService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).async { implicit request =>
      for {
        currentReturn <- psrRetrievalService.getStandardPsrDetails(None, Some(year), Some(current.toString))
        _ <- saveService.save(currentReturn)
        previousReturn <- psrRetrievalService.getStandardPsrDetails(None, Some(year), Some(previous.toString))
        viewModel = TaskListViewController.viewModel(
          srn,
          request.schemeDetails.schemeName,
          LocalDate.now(), //TODO
          LocalDate.now(), //TODO
          currentReturn,
          previousReturn,
          request.pensionSchemeId
        )
      } yield Ok(view(viewModel))
    }
}

object TaskListViewController {

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    currentUA: UserAnswers,
    previousUA: UserAnswers,
    pensionSchemeId: PensionSchemeId
  ): PageViewModel[TaskListViewModel] = {

    val sectionListWithoutDeclaration = List(
      schemeDetailsSection(srn, schemeName, currentUA, previousUA),
//      membersSection(srn, schemeName, currentUA),
//      memberPaymentsSection(srn, currentUA),
      loansSection(srn, schemeName, currentUA, previousUA),
      sharesSection(srn, currentUA, previousUA)
//      landOrPropertySection(srn, currentUA),
//      bondsSection(srn, currentUA),
//      otherAssetsSection(srn, currentUA)
    )

    val (numberOfCompletedWithoutDeclaration, numberOfTotalWithoutDeclaration) = evaluateCompletedTotalTuple(
      sectionListWithoutDeclaration
    )

    // TODO:
    //  isUnallocatedEmployerAnsweredAsNo and isPensionPaymentsReceivedAnsweredAsNo must be removed once tasklist status logic implemented for them properly.
    //  Now we're just checking whether user selected no in the beginning of the journey (using shortcut).
    val isUnallocatedEmployerAnsweredAsNo = currentUA
      .get(UnallocatedEmployerContributionsPage(srn))
      .fold(0)(x => if (x) 0 else 1)

    val isPensionPaymentsReceivedAnsweredAsNo = currentUA
      .get(PensionPaymentsReceivedPage(srn))
      .fold(0)(x => if (x) 0 else 1)

    val numberOfCompletedPages = numberOfCompletedWithoutDeclaration + isUnallocatedEmployerAnsweredAsNo + isPensionPaymentsReceivedAnsweredAsNo
    val isLinkActive = numberOfTotalWithoutDeclaration == numberOfCompletedPages

    val declarationSectionViewModel =
      declarationSection(
        srn,
        pensionSchemeId.isPSP,
        isLinkActive
      )

    val viewModel = TaskListViewModel(
      sectionListWithoutDeclaration.head,
      sectionListWithoutDeclaration.tail :+ declarationSectionViewModel: _*
    )

    val (numberOfCompleted, numberOfTotal) = evaluateCompletedTotalTuple(viewModel.sections.toList)

    PageViewModel(
      Message("nonsipp.tasklist.title", startDate.show, endDate.show),
      Message("nonsipp.tasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      Heading2("nonsipp.tasklist.subheading") ++
        ParagraphMessage(Message("nonsipp.tasklist.description", numberOfCompleted, numberOfTotal))
    )
  }

  private def schemeDetailsSection(
    srn: Srn,
    schemeName: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.schemedetails"
    TaskListSectionViewModel(
      s"$prefix.title",
      getBasicSchemeDetailsTaskListItem(srn, schemeName, prefix, currentUA, previousUA),
      getFinancialDetailsTaskListItem(srn, schemeName, prefix, currentUA, previousUA)
    )
  }

  private def getBasicSchemeDetailsTaskListItem(
    srn: Srn,
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

    val taskListStatus = if (accountingPeriodsSame && schemeDesignatoriesSame) {
      Completed
    } else {
      Updated
    }

    TaskListItemViewModel(
      LinkMessage(
        //Message(s"$prefix.details.title", schemeName),
        Message(messageKey(prefix, "details.title", taskListStatus), schemeName),
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
      ),
      taskListStatus
    )
  }

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

  private def getFinancialDetailsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    currentUA: UserAnswers,
    previousUA: UserAnswers
  ): TaskListItemViewModel = {

    val taskListStatus =
      if (currentUA.get(JsPath \ "schemeDesignatory" \ "totalAssetValue") == previousUA
          .get(JsPath \ "schemeDesignatory" \ "totalAssetValue") &&
        currentUA.get(JsPath \ "schemeDesignatory" \ "totalPayments") == previousUA.get(
          JsPath \ "schemeDesignatory" \ "totalPayments"
        ) &&
        currentUA.get(JsPath \ "schemeDesignatory" \ "totalCash") == previousUA.get(
          JsPath \ "schemeDesignatory" \ "totalCash"
        )) {
        Completed
      } else {
        Updated
      }

    TaskListItemViewModel(
      LinkMessage(
        Message(messageKey(prefix, "finances.title", taskListStatus), schemeName),
        controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
          .onPageLoad(srn, NormalMode)
          .url
      ),
      taskListStatus
    )
  }

  def messageKey(prefix: String, suffix: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted => s"$prefix.add.$suffix"
      case _ => s"$prefix.view.$suffix"
    }

  private def loansSection(
    srn: Srn,
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

    val borrowingStatus = getBorrowingTaskListStatusAndLink(currentUA, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "loansmade.title", loansTaskListStatus), schemeName),
          controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
            .onPageLoad(srn, 1, NormalMode)
            .url // TODO navigate to read-only loan list page
        ),
        loansTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "moneyborrowed.title", borrowingStatus._1), schemeName),
          borrowingStatus._2 // / TODO navigate to read-only list or did they have any page
        ),
        borrowingTaskListStatus
      )
    )
  }

  private def sharesSection(srn: Srn, currentUA: UserAnswers, previousUA: UserAnswers) = {
    val prefix = "nonsipp.tasklist.shares"
    val (sharesStatus, sharesLink) = getSharesTaskListStatusAndLink(currentUA, srn)
    //val quotedSharesStatusAndLink = getQuotedSharesTaskListStatusAndLink(currentUA, srn)
    val (sharesDisposalsStatus, sharesDisposalsLinkUrl) =
      TaskListStatusUtils.getSharesDisposalsTaskListStatusWithLink(currentUA, srn)

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

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "sponsoringemployer.title", sharesStatus),
          sharesLink
        ),
        sharesTaskListStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.sharesdisposal", "title", shareDisposalTaskListStatus),
          sharesDisposalsLinkUrl
        ),
        shareDisposalTaskListStatus
      )
      // TODO:
      //      TaskListItemViewModel(
      //        LinkMessage(
      //          messageKey(prefix, "quotedshares.title", quotedSharesStatusAndLink._1),
      //          quotedSharesStatusAndLink._2
      //        ),
      //        quotedSharesStatusAndLink._1
      //      )
    )
  }

  //  private def getCompletedOrUpdatedTaskListStatus(
  //    currentUA: UserAnswers,
  //    previousUA: UserAnswers,
  //    path: JsPath
  //  ): TaskListStatus = {
  //    val c = currentUA.get(path)
  //    val p = previousUA.get(path)
  //    if (c == p) {
  //      Completed
  //    } else {
  //      Updated
  //    }
  //  }

  private def declarationSection(srn: Srn, isPsp: Boolean, isLinkActive: Boolean): TaskListSectionViewModel = {
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
      LinkMessage(s"$prefix.saveandreturn", controllers.routes.UnauthorisedController.onPageLoad().url)
    )
  }

  private def evaluateCompletedTotalTuple(sections: List[TaskListSectionViewModel]): (Int, Int) = {
    val items = sections.flatMap(_.items.fold(_ => Nil, _.toList))
    val numberOfCompleted = items.count(_.status == Completed)
    val numberOfTotal = items.length
    (numberOfCompleted, numberOfTotal)
  }

  private def membersSection(srn: Srn, schemeName: String, userAnswers: UserAnswers) = {
    // example
    val exampleMemberPersonalDetails = userAnswers.get(JsPath \ "membersPayments" \ "memberDetails" \ "personalDetails")

    // TODO:
    val prefix = "nonsipp.tasklist.members"
    val taskListStatus = Completed
    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "details.title", taskListStatus), schemeName),
          controllers.nonsipp.memberdetails.routes.SchemeMembersListController
            .onPageLoad(srn, 1, ManualOrUpload.Manual)
            .url
        ),
        taskListStatus
      )
    )
  }

  private def memberPaymentsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    // example
    val exampleEmployerContribution =
      userAnswers.get(JsPath \ "membersPayments" \ "memberDetails" \ "memberEmpContribution")

    // TODO
    val prefix = "nonsipp.tasklist.memberpayments"

    // change to check if members section is complete to start
    val (employerContributionStatus, employerContributionLink) = getEmployerContributionStatusAndLink(userAnswers, srn)

    val transferInStatus: TaskListStatus =
      userAnswers
        .get(TransfersInJourneyStatus(srn))
        .fold[TaskListStatus](TaskListStatus.NotStarted) {
          case SectionStatus.InProgress => TaskListStatus.InProgress
          case SectionStatus.Completed => TaskListStatus.Completed
        }

    val transferOutStatus: TaskListStatus =
      userAnswers
        .get(TransfersOutJourneyStatus(srn))
        .fold[TaskListStatus](TaskListStatus.NotStarted) {
          case SectionStatus.InProgress => TaskListStatus.InProgress
          case SectionStatus.Completed => TaskListStatus.Completed
        }

    val memberContributionStatus: TaskListStatus =
      userAnswers
        .get(MemberContributionsListPage(srn))
        .fold[TaskListStatus](TaskListStatus.NotStarted) {
          case false => TaskListStatus.InProgress
          case true => TaskListStatus.Completed
        }

    val pclsMemberStatus: TaskListStatus =
      userAnswers
        .get(PclsMemberListPage(srn))
        .fold[TaskListStatus](TaskListStatus.NotStarted) {
          case false => TaskListStatus.InProgress
          case true => TaskListStatus.Completed
        }
    val surrenderedBenefitsStatus: TaskListStatus =
      userAnswers
        .get(SurrenderedBenefitsJourneyStatus(srn))
        .fold[TaskListStatus](TaskListStatus.NotStarted) {
          case SectionStatus.InProgress => TaskListStatus.InProgress
          case SectionStatus.Completed => TaskListStatus.Completed
        }

    val pensionPaymentsStatus: TaskListStatus =
      userAnswers
        .get(PensionPaymentsJourneyStatus(srn))
        .fold[TaskListStatus](TaskListStatus.NotStarted) {
          case SectionStatus.InProgress => TaskListStatus.InProgress
          case SectionStatus.Completed => TaskListStatus.Completed
        }

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
          messageKey(prefix, "unallocatedcontributions.title", UnableToStart),
          controllers.nonsipp.memberpayments.routes.UnallocatedEmployerContributionsController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        NotStarted
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "memberContributions.title", memberContributionStatus),
          controllers.nonsipp.membercontributions.routes.MemberContributionsController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        memberContributionStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersreceived.title", transferInStatus),
          controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController.onPageLoad(srn, NormalMode).url
        ),
        transferInStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersout.title", transferOutStatus),
          controllers.nonsipp.membertransferout.routes.SchemeTransferOutController.onPageLoad(srn, NormalMode).url
        ),
        transferOutStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "pcls.title", pclsMemberStatus),
          controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        pclsMemberStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "payments.title", pensionPaymentsStatus),
          controllers.nonsipp.memberpensionpayments.routes.PensionPaymentsReceivedController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        pensionPaymentsStatus
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "surrenderedbenefits.title", surrenderedBenefitsStatus),
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        surrenderedBenefitsStatus
      )
    )
  }

  private def landOrPropertySection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    // TODO
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
    // TODO[
    val prefix = "nonsipp.tasklist.bonds"
    val statusAndLink = getBondsTaskListStatusAndLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unregulatedorconnected.title", statusAndLink._1),
          statusAndLink._2
        ),
        statusAndLink._1
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "bondsdisposal.title", NotStarted),
          controllers.nonsipp.bondsdisposal.routes.BondsDisposalController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
      )
    )
  }

  private def otherAssetsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    // TODO
    val prefix = "nonsipp.tasklist.otherassets"
    val statusAndLink = getOtherAssetsTaskListStatusAndLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "title", statusAndLink._1),
          statusAndLink._2
        ),
        statusAndLink._1
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey("nonsipp.tasklist.otherassetsdisposal", "title", NotStarted),
          controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
      )
    )
  }
}
