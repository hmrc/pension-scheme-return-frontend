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

package controllers.nonsipp.receivetransfer

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import pages.nonsipp.receivetransfer.ReceiveTransferProgress.TransfersInSectionCompletedUserAnswersOps
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import config.RefinedTypes.{Max300, Max5}
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import cats.implicits.toShow
import pages.nonsipp.receivetransfer._
import controllers.nonsipp.receivetransfer.TransferReceivedMemberListController._
import controllers.actions._
import eu.timepit.refined.refineMV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models.SectionJourneyStatus.InProgress
import viewmodels.models._

import scala.concurrent.Future

import java.time.LocalDateTime
import javax.inject.Named

class TransferReceivedMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction
) extends PSRController {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, page, mode, showBackLink = true)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
    onPageLoadCommon(srn, page, mode, showBackLink = true)
  }

  private def onPageLoadCommon(srn: Srn, page: Int, mode: Mode, showBackLink: Boolean)(
    implicit request: DataRequest[AnyContent]
  ): Result =
    request.userAnswers.completedMembersDetails(srn) match {
      case Left(err) =>
        logger.warn(s"Error when fetching completed member details - $err")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(Nil) =>
        logger.warn(s"No completed member details for srn $srn")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(completedMemberDetails) =>
        val noPageEnabled = !request.userAnswers.get(DidSchemeReceiveTransferPage(srn)).getOrElse(false)

        Ok(
          view(
            viewModel(
              srn,
              page,
              mode,
              membersWithTransfers = buildReceiveTransfer(srn, completedMemberDetails),
              viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getCompletedOrUpdatedTaskListStatus(
                  request.userAnswers,
                  request.previousUserAnswers.get,
                  pages.nonsipp.receivetransfer.Paths.memberTransfersIn
                ) == Updated
              } else {
                false
              },
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
              schemeName = request.schemeDetails.schemeName,
              noPageEnabled = noPageEnabled,
              showBackLink = showBackLink
            )
          )
        )
    }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(
      navigator.nextPage(TransferReceivedMemberListPage(srn), mode, request.userAnswers)
    )
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }

  def onPreviousViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) { implicit request =>
      val showBackLink = false
      onPageLoadCommon(srn, page, ViewOnlyMode, showBackLink)
    }

  private def buildReceiveTransfer(srn: Srn, indexes: List[(Max300, NameDOB)])(
    implicit request: DataRequest[_]
  ): List[MemberWithReceiveTransfer] = indexes.map {
    case (index, nameDOB) =>
      MemberWithReceiveTransfer(
        memberIndex = index,
        transferFullName = nameDOB.fullName,
        receive = request.userAnswers
          .receiveTransferProgress(srn, index)
          .map {
            case (secondaryIndex, status) =>
              ReceiveTransfer(secondaryIndex, status)
          }
      )
  }

}

object TransferReceivedMemberListController {

  private def rows(
    srn: Srn,
    mode: Mode,
    membersWithTransfers: List[MemberWithReceiveTransfer],
    optYear: Option[String],
    optCurrentVersion: Option[Int],
    optPreviousVersion: Option[Int]
  ): List[List[TableElem]] =
    membersWithTransfers
      .map { membersWithTransfers =>
        val noContributions = membersWithTransfers.receive.isEmpty
        val onlyInProgressContributions = membersWithTransfers.receive.forall(_.status.inProgress)

        if (noContributions || onlyInProgressContributions) {
          List(
            TableElem(
              membersWithTransfers.transferFullName
            ),
            TableElem(
              Message("transferIn.MemberList.status.no.contributions")
            ),
            if (mode != ViewOnlyMode) {
              TableElem.add(
                membersWithTransfers.receive.find(_.status.inProgress) match {
                  case Some(ReceiveTransfer(_, InProgress(url))) => url
                  case _ =>
                    controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
                      .onSubmit(srn, membersWithTransfers.memberIndex, refineMV(1), mode)
                      .url
                },
                Message(
                  "receiveTransfer.MemberList.add.hidden.text",
                  membersWithTransfers.transferFullName
                )
              )
            } else {
              TableElem.empty
            },
            // Remove link
            TableElem.empty
          )
        } else {
          List(
            TableElem(
              membersWithTransfers.transferFullName
            ),
            TableElem(
              if (membersWithTransfers.receive.size == 1) {
                Message(
                  "transferIn.MemberList.singleStatus.some.contribution",
                  membersWithTransfers.receive.size
                )
              } else {
                Message(
                  "transferIn.MemberList.status.some.contributions",
                  membersWithTransfers.receive.count(_.status.completed)
                )
              }
            ),
            (mode, optYear, optCurrentVersion, optPreviousVersion) match {
              case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                TableElem.view(
                  controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
                    .onPageLoadViewOnly(
                      srn,
                      membersWithTransfers.memberIndex,
                      year,
                      currentVersion,
                      previousVersion
                    ),
                  hiddenText = membersWithTransfers.transferFullName
                )
              case _ =>
                TableElem.change(
                  controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
                    .onSubmit(srn, membersWithTransfers.memberIndex, CheckMode),
                  Message(
                    "receiveTransfer.MemberList.add.hidden.text",
                    membersWithTransfers.transferFullName
                  )
                )
            },
            if (mode == ViewOnlyMode) {
              TableElem.empty
            } else {
              TableElem.remove(
                controllers.nonsipp.receivetransfer.routes.WhichTransferInRemoveController
                  .onSubmit(srn, membersWithTransfers.memberIndex),
                Message(
                  "receiveTransfer.MemberList.add.hidden.text",
                  membersWithTransfers.transferFullName
                )
              )
            }
          )
        }
      }
      .sortBy(_.headOption.map(_.text.toString))

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    membersWithTransfers: List[MemberWithReceiveTransfer],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    schemeName: String,
    noPageEnabled: Boolean,
    showBackLink: Boolean = true
  ): FormPageViewModel[ActionTableViewModel] = {

    val (title, heading) =
      if (membersWithTransfers.size == 1) {
        ("transferIn.MemberList.title", "transferIn.MemberList.heading")
      } else {
        ("transferIn.MemberList.title.plural", "transferIn.MemberList.heading.plural")
      }

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.transferInListSize >= membersWithTransfers.size) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.transferInListSize,
      membersWithTransfers.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    val optDescription =
      Option.when(mode == NormalMode)(
        ParagraphMessage(
          "transferIn.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "transferIn.MemberList.paragraph2"
          )
      )

    FormPageViewModel(
      mode = mode,
      title = Message(title, membersWithTransfers.size),
      heading = Message(heading, membersWithTransfers.size),
      description = optDescription,
      page = ActionTableViewModel(
        inset = "",
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(
          srn,
          mode,
          membersWithTransfers.sortBy(_.transferFullName),
          optYear,
          optCurrentVersion,
          optPreviousVersion
        ),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "transferIn.MemberList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      onSubmit = controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
        .onSubmit(srn, page, mode),
      optViewOnlyDetails = if (mode.isViewOnlyMode) {
        val totalTransfersIn: Int = membersWithTransfers.map(_.receive.size).sum
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "transferIn.MemberList.viewOnly.link",
                    controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
                      .onPreviousViewOnly(
                        srn,
                        page,
                        year,
                        currentVersion,
                        previousVersion
                      )
                      .url
                  )
                )
              case _ => None
            },
            submittedText =
              compilationOrSubmissionDate.fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
            title = "transferIn.MemberList.viewOnly.title",
            heading = totalTransfersIn match {
              case 0 => Message("transferIn.MemberList.viewOnly.noTransfers")
              case 1 => Message("transferIn.MemberList.viewOnly.singular")
              case _ => Message("transferIn.MemberList.viewOnly.plural", totalTransfersIn)
            },
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
                  .onSubmit(srn, page, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("transferIn.MemberList.view.none", schemeName)
            )
          )
        )
      } else {
        None
      },
      showBackLink = showBackLink
    )
  }

  protected[receivetransfer] case class MemberWithReceiveTransfer(
    memberIndex: Max300,
    transferFullName: String,
    receive: List[ReceiveTransfer]
  )

  protected[receivetransfer] case class ReceiveTransfer(receiveIndex: Max5, status: SectionJourneyStatus)

}
