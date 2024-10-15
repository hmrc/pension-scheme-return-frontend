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

package controllers.nonsipp.memberreceivedpcls

import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.OneTo300
import controllers.PSRController
import cats.implicits.toShow
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls._
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._

import scala.concurrent.Future

import java.time.LocalDateTime
import javax.inject.Named

class PclsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction
) extends PSRController {

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
  ): Result = {
    val ua = request.userAnswers
    val optionList: List[Option[NameDOB]] = ua.membersOptionList(srn)

    if (optionList.flatten.nonEmpty) {
      val noPageEnabled = !ua.get(PensionCommencementLumpSumPage(srn)).getOrElse(false)
      val viewModel = PclsMemberListController
        .viewModel(
          srn,
          page,
          mode,
          optionList,
          ua,
          viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
            getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              request.previousUserAnswers.get,
              pages.nonsipp.membercontributions.Paths.memberDetails \ "memberLumpSumReceived"
            ) == Updated
          } else {
            false
          },
          optYear = request.year,
          optCurrentVersion = request.currentVersion,
          optPreviousVersion = request.previousVersion,
          compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
          noPageEnabled = noPageEnabled,
          showBackLink = showBackLink
        )

      Ok(view(viewModel))
    } else {
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(
      navigator.nextPage(PclsMemberListPage(srn), mode, request.userAnswers)
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

object PclsMemberListController {

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[Option[NameDOB]],
    userAnswers: UserAnswers,
    optYear: Option[String],
    optCurrentVersion: Option[Int],
    optPreviousVersion: Option[Int]
  ): List[List[TableElem]] =
    memberList.zipWithIndex
      .map {
        case (Some(memberName), index) =>
          refineV[OneTo300](index + 1) match {
            case Left(_) => Nil
            case Right(nextIndex) =>
              val items = userAnswers.get(PensionCommencementLumpSumAmountPage(srn, nextIndex))
              if (items.isEmpty || items.exists(_.isZero)) {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    Message("pcls.memberlist.status.no.items")
                  ),
                  if (mode != ViewOnlyMode) {
                    TableElem.add(
                      controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                        .onSubmit(srn, nextIndex, NormalMode),
                      Message("pcls.memberList.add.hidden.text", memberName.fullName)
                    )
                  } else {
                    TableElem.empty
                  },
                  TableElem.empty
                )
              } else {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    Message("pcls.memberlist.status.some.item", items.size)
                  ),
                  (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                    case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                      TableElem.view(
                        controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                          .onPageLoadViewOnly(srn, nextIndex, year, currentVersion, previousVersion),
                        Message("pcls.memberList.remove.hidden.text", memberName.fullName)
                      )
                    case _ =>
                      TableElem.change(
                        controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                          .onSubmit(srn, nextIndex, CheckMode),
                        Message("pcls.memberList.change.hidden.text", memberName.fullName)
                      )
                  },
                  if (mode == ViewOnlyMode) {
                    TableElem.empty
                  } else {
                    TableElem.remove(
                      controllers.nonsipp.memberreceivedpcls.routes.RemovePclsController
                        .onSubmit(srn, nextIndex),
                      Message("pcls.memberList.remove.hidden.text", memberName.fullName)
                    )
                  }
                )
              }
          }
        case _ => List.empty

      }
      .sortBy(_.headOption.map(_.text.toString))

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[Option[NameDOB]],
    userAnswers: UserAnswers,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    noPageEnabled: Boolean,
    showBackLink: Boolean = true
  ): FormPageViewModel[ActionTableViewModel] = {
    val title = "pcls.memberlist.title"
    val heading = "pcls.memberlist.heading"

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.pclsInListSize >= memberList.flatten.size) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.pclsInListSize,
      memberList.flatten.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
            .onPageLoad(srn, _, NormalMode)
      }
    )
    val sumPcls = memberList.flatten.zipWithIndex.count {
      case (_, index) =>
        refineV[OneTo300](index + 1).toOption
          .exists(nextIndex => userAnswers.get(PensionCommencementLumpSumAmountPage(srn, nextIndex)).isDefined)
    }

    val optDescription =
      Option.when(mode == NormalMode)(
        ParagraphMessage(
          "pcls.memberlist.paragraph1"
        ) ++
          ParagraphMessage(
            "pcls.memberlist.paragraph2"
          )
      )

    FormPageViewModel(
      mode = mode,
      title = Message(title, memberList.flatten.size),
      heading = Message(heading, memberList.flatten.size),
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
        rows = rows(srn, mode, memberList, userAnswers, optYear, optCurrentVersion, optPreviousVersion),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "pcls.memberlist.pagination.label",
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
      onSubmit = controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
        .onSubmit(srn, page, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "pcls.MemberList.viewOnly.link",
                    controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
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
            title = "pcls.MemberList.viewOnly.title",
            heading = sumPcls match {
              case 0 => Message("pcls.MemberList.viewOnly.heading")
              case _ => Message("pcls.MemberList.viewOnly.withValue", sumPcls)
            },
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.memberreceivedpcls.routes.PclsMemberListController
                  .onSubmit(srn, page, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("pcls.MemberList.view.none")
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
