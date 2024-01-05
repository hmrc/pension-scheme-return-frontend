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
import config.Refined.{OneTo300, OneTo5000}
import controllers.actions._
import eu.timepit.refined.refineV
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, IdentitySubject, ManualOrUpload, NormalMode, PensionSchemeId, UserAnswers}
import models.ConditionalYesNo._
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.common.IdentityTypes
import pages.nonsipp.employercontributions.EmployerContributionsSectionStatus
import pages.nonsipp.loansmadeoroutstanding.{
  IsIndividualRecipientConnectedPartyPages,
  OutstandingArrearsOnLoanPages,
  RecipientSponsoringEmployerConnectedPartyPages
}
import pages.nonsipp.membercontributions.MemberContributionsListPage
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsNinoPages, MembersDetailsPages, NoNinoPages}
import pages.nonsipp.memberreceivedpcls.PclsMemberListPage
import pages.nonsipp.receivetransfer.TransfersInJourneyStatus
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, ValueOfAssetsPage, WhyNoBankAccountPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import utils.nonsipp.TaskListStatusUtils
import utils.nonsipp.TaskListStatusUtils._
import viewmodels.DisplayMessage.{Heading2, LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.TaskListStatus.{Completed, InProgress, NotStarted, TaskListStatus, UnableToStart}
import viewmodels.models.{
  PageViewModel,
  SectionStatus,
  TaskListItemViewModel,
  TaskListSectionViewModel,
  TaskListStatus,
  TaskListViewModel
}
import views.html.TaskListView

import java.time.LocalDate

class TaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  schemeDateService: SchemeDateService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    withSchemeDate(srn) { dates =>
      val viewModel = TaskListController.viewModel(
        srn,
        request.schemeDetails.schemeName,
        dates.from,
        dates.to,
        request.userAnswers,
        request.pensionSchemeId
      )
      Ok(view(viewModel))
    }
  }

  private def withSchemeDate(srn: Srn)(body: DateRange => Result)(implicit request: DataRequest[_]): Result =
    schemeDateService.schemeDate(srn) match {
      case Some(dates) => body(dates)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
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
  ) = {
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
  ) = {
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
  ) = {

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

  private def membersSection(srn: Srn, schemeName: String, userAnswers: UserAnswers) = {
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

  private def getIncompleteMembersIndex(userAnswers: UserAnswers, srn: Srn) = {
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
          val memberDetailsIndexes = memberDetails.indices.toList
          val ninoIndexes = ninos.getOrElse(List.empty).map(_._1.toInt).toList
          val noninoIndexes = noNinos.getOrElse(List.empty).map(_._1.toInt).toList
          val finishedIndexes = ninoIndexes ++ noninoIndexes
          val filtered = memberDetailsIndexes.filter(finishedIndexes.indexOf(_) < 0)
          if (filtered.isEmpty) {
            1
          } else {
            filtered(0) + 1
          }
        }
    }
  }

  private def getIncompleteLoansIndex(userAnswers: UserAnswers, srn: Srn) = {
    val whoReceivedTheLoanPages = userAnswers.get(IdentityTypes(srn, IdentitySubject.LoanRecipient))
    val outstandingArrearsOnLoanPages = userAnswers.get(OutstandingArrearsOnLoanPages(srn))
    val sponsoringPages = userAnswers.get(RecipientSponsoringEmployerConnectedPartyPages(srn))
    val connectedPartyPages = userAnswers.get(IsIndividualRecipientConnectedPartyPages(srn))

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
              filtered(0) + 1 // index based on arrears page missing
            } else {
              filteredSC(0) + 1 // index based on sponsoring employer or individual connected party
            }
          }
        }
    }
  }

  private def memberPaymentsSection(srn: Srn, userAnswers: UserAnswers): TaskListSectionViewModel = {
    val prefix = "nonsipp.tasklist.memberpayments"

    // change to check if members section is complete to start
    val employerContributionStatus: TaskListStatus =
      userAnswers
        .get(EmployerContributionsSectionStatus(srn))
        .fold[TaskListStatus](TaskListStatus.NotStarted) {
          case SectionStatus.InProgress => TaskListStatus.InProgress
          case SectionStatus.Completed => TaskListStatus.Completed
        }

    val transferInStatus: TaskListStatus =
      userAnswers
        .get(TransfersInJourneyStatus(srn))
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

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "employercontributions.title", employerContributionStatus),
          employerContributionStatus match {
            // change so Completed goes to member list page
            case _ =>
              controllers.nonsipp.employercontributions.routes.EmployerContributionsController
                .onPageLoad(srn, NormalMode)
                .url
          }
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
          messageKey(prefix, "transfersout.title", UnableToStart),
          controllers.nonsipp.membertransferout.routes.SchemeTransferOutController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
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
          messageKey(prefix, "payments.title", UnableToStart),
          controllers.nonsipp.memberpensionpayments.routes.PensionPaymentsReceivedController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        NotStarted
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "surrenderedbenefits.title", UnableToStart),
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        NotStarted
      )
    )
  }

  private def loansSection(srn: Srn, schemeName: String, userAnswers: UserAnswers) = {
    val prefix = s"nonsipp.tasklist.loans"
    val taskListStatus: TaskListStatus = getLoansTaskListStatus(userAnswers, srn)
    val borrowingStatus = TaskListStatusUtils.getBorrowingTaskListStatusAndLink(userAnswers, srn)

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
            case InProgress =>
              val incompleteIndex: Int = getIncompleteLoansIndex(userAnswers, srn)
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

  private def sharesSection(srn: Srn, userAnswers: UserAnswers) = {
    val prefix = "nonsipp.tasklist.shares"
    val sharesStatusAndLink = getSharesTaskListStatusAndLink(userAnswers, srn)
    val quotedSharesStatusAndLink = getQuotedSharesTaskListStatusAndLink(userAnswers, srn)

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "sponsoringemployer.title", sharesStatusAndLink._1),
          sharesStatusAndLink._2
        ),
        sharesStatusAndLink._1
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

  private def landOrPropertySection(srn: Srn, userAnswers: UserAnswers) = {
    val prefix = "nonsipp.tasklist.landorproperty"

    val landOrPropertyStatus = TaskListStatusUtils.getLandOrPropertyTaskListStatusAndLink(userAnswers, srn)
    val (disposalsStatus, disposalLinkUrl) = TaskListStatusUtils.getDisposalsTaskListStatusWithLink(userAnswers, srn)

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
          disposalLinkUrl
        ),
        disposalsStatus
      )
    )
  }

  private def bondsSection(srn: Srn, userAnswers: UserAnswers) = {
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
      )
    )
  }

  private def otherAssetsSection(srn: Srn, userAnswers: UserAnswers) = {
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
      )
    )
  }

  private val declarationSection = {
    val prefix = "nonsipp.tasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      Message(s"$prefix.incomplete"),
      LinkMessage(s"$prefix.saveandreturn", controllers.routes.UnauthorisedController.onPageLoad().url)
    )
  }

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId
  ): PageViewModel[TaskListViewModel] = {

    val viewModel = TaskListViewModel(
      schemeDetailsSection(srn, schemeName, userAnswers, pensionSchemeId),
      membersSection(srn, schemeName, userAnswers),
      memberPaymentsSection(srn, userAnswers),
      loansSection(srn, schemeName, userAnswers),
      sharesSection(srn, userAnswers),
      landOrPropertySection(srn, userAnswers),
      bondsSection(srn, userAnswers),
      otherAssetsSection(srn, userAnswers),
      declarationSection
    )

    val items = viewModel.sections.toList.flatMap(_.items.fold(_ => Nil, _.toList))
    val completed = items.count(_.status == Completed)
    val total = items.length

    PageViewModel(
      Message("nonsipp.tasklist.title", startDate.show, endDate.show),
      Message("nonsipp.tasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      Heading2("nonsipp.tasklist.subheading") ++
        ParagraphMessage(Message("nonsipp.tasklist.description", completed, total))
    )
  }
}
