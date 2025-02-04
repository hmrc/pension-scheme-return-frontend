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

package controllers.nonsipp.membertransferout

import controllers.nonsipp.membertransferout.TransferOutMemberListController._
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import cats.implicits.toShow
import viewmodels.models.TaskListStatus.Updated
import models.requests.DataRequest
import config.RefinedTypes.Max300
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineMV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import pages.nonsipp.membertransferout._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._

import scala.concurrent.Future

import java.time.LocalDateTime
import javax.inject.Named

class TransferOutMemberListController @Inject()(
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
    val showBackLink = true
    onPageLoadCommon(srn, page, mode, showBackLink)
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
        val noPageEnabled = !request.userAnswers.get(SchemeTransferOutPage(srn)).getOrElse(false)
        val membersWithTransfers = completedMemberDetails.map {
          case (index, memberDetails) =>
            val completedTransfersOut =
              request.userAnswers.map(TransfersOutSectionCompleted.all(srn, index)).values.size
            (index, memberDetails, completedTransfersOut)
        }
        Ok(
          view(
            viewModel(
              srn,
              page,
              mode,
              membersWithTransfers,
              viewOnlyUpdated = if (mode.isViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getCompletedOrUpdatedTaskListStatus(
                  request.userAnswers,
                  request.previousUserAnswers.get,
                  pages.nonsipp.membertransferout.Paths.memberTransfersOut
                ) == Updated
              } else {
                false
              },
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
              schemeName = request.schemeDetails.schemeName,
              noPageEnabled,
              showBackLink = showBackLink
            )
          )
        )
    }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(
      navigator.nextPage(TransferOutMemberListPage(srn), mode, request.userAnswers)
    )
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }

  def onPreviousViewOnly(
    srn: Srn,
    page: Int,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) { implicit request =>
      val showBackLink = false
      onPageLoadCommon(srn, page, ViewOnlyMode, showBackLink)
    }

}

object TransferOutMemberListController {

  private[membertransferout] type CompletedTransfersOut = Int

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[(Max300, NameDOB, CompletedTransfersOut)],
    optYear: Option[String],
    optCurrentVersion: Option[Int],
    optPreviousVersion: Option[Int]
  ): List[List[TableElemBase]] =
    memberList
      .map {
        case (index, memberName, completedTransfersOut) =>
          if (completedTransfersOut == 0) {
            List(
              TableElem(
                memberName.fullName
              ),
              TableElem(
                Message("transferOut.memberList.status.no.contributions")
              ),
              if (!mode.isViewOnlyMode) {
                TableElem(
                  LinkMessage(
                    Message("site.add"),
                    controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                      .onSubmit(srn, index, refineMV(1), mode)
                      .url,
                    Message(
                      "transferOut.memberList.add.hidden.text",
                      memberName.fullName
                    )
                  )
                )
              } else {
                TableElem.empty
              }
            )
          } else {
            List(
              TableElem(
                memberName.fullName
              ),
              TableElem(
                if (completedTransfersOut == 1) {
                  Message("transferOut.memberList.singleStatus.some.contribution", completedTransfersOut)
                } else {
                  Message("transferOut.memberList.status.some.contributions", completedTransfersOut)
                }
              ),
              TableElemDoubleLink(
                (
                  (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                    case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                      TableElem.view(
                        controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
                          .onPageLoadViewOnly(
                            srn,
                            index,
                            year = year,
                            current = currentVersion,
                            previous = previousVersion
                          ),
                        memberName.fullName
                      )
                    case _ =>
                      TableElem.change(
                        controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
                          .onPageLoad(srn, index, CheckMode),
                        Message(
                          "transferOut.memberList.change.hidden.text",
                          memberName.fullName
                        )
                      )
                  },
                  if (mode.isViewOnlyMode) {
                    TableElem.empty
                  } else {
                    TableElem.remove(
                      controllers.nonsipp.membertransferout.routes.WhichTransferOutRemoveController
                        .onSubmit(srn, index),
                      Message(
                        "transferOut.memberList.remove.hidden.text",
                        memberName.fullName
                      )
                    )
                  }
                )
              )
            )
          }
      }
      .sortBy(_.headOption.map(_.asInstanceOf[TableElem].text.toString))

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    membersWithTransfers: List[(Max300, NameDOB, CompletedTransfersOut)],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    schemeName: String,
    noPageEnabled: Boolean,
    showBackLink: Boolean = true
  ): FormPageViewModel[ActionTableViewModel] = {

    val memberListSize = membersWithTransfers.size
    val (title, heading) =
      if (memberListSize == 1) {
        ("transferOut.memberList.title", "transferOut.memberList.heading")
      } else {
        ("transferOut.memberList.title.plural", "transferOut.memberList.heading.plural")
      }

    val sumTransfersOut = membersWithTransfers.map(_._3).sum

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.transferOutListSize >= memberListSize) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.transferOutListSize,
      memberListSize,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    val optDescription =
      Option.when(mode == NormalMode)(
        ParagraphMessage(
          "transferOut.memberList.paragraph1"
        ) ++
          ParagraphMessage(
            "transferOut.memberList.paragraph2"
          )
      )

    FormPageViewModel(
      mode = mode,
      title = Message(title, memberListSize),
      heading = Message(heading, memberListSize),
      description = optDescription,
      page = ActionTableViewModel(
        inset = "",
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, membersWithTransfers, optYear, optCurrentVersion, optPreviousVersion),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "transferOut.memberList.pagination.label",
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
      onSubmit = controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
        .onSubmit(srn, page, mode),
      optViewOnlyDetails = if (mode.isViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "transferOut.memberList.viewOnly.link",
                    controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
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
            title = "transferOut.memberList.viewOnly.title",
            heading = sumTransfersOut match {
              case 0 => Message("transferOut.memberList.viewOnly.heading")
              case 1 => Message("transferOut.memberList.viewOnly.singular")
              case _ => Message("transferOut.memberList.viewOnly.plural", sumTransfersOut)
            },
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
                  .onSubmit(srn, page, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("transferOut.memberList.view.none", schemeName)
            )
          )
        )
      } else {
        None
      },
      showBackLink = showBackLink
    )
  }
}
