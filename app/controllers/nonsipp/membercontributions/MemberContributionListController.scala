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

package controllers.nonsipp.membercontributions

import play.api.mvc._
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import controllers.nonsipp.membercontributions.MemberContributionListController._
import cats.implicits.toShow
import _root_.config.Constants
import controllers.actions.IdentifyAndRequireData
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import viewmodels.implicits._
import pages.nonsipp.membercontributions.{
  MemberContributionsListPage,
  MemberContributionsPage,
  TotalMemberContributionPage
}
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._

import scala.concurrent.Future

import java.time.LocalDateTime
import javax.inject.Named

class MemberContributionListController @Inject()(
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
        val noPageEnabled = !request.userAnswers.get(MemberContributionsPage(srn)).getOrElse(false)
        Ok(
          view(
            viewModel(
              srn,
              page,
              mode,
              completedMemberDetails,
              request.userAnswers,
              viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getCompletedOrUpdatedTaskListStatus(
                  request.userAnswers,
                  request.previousUserAnswers.get,
                  pages.nonsipp.membercontributions.Paths.memberDetails \ "totalMemberContribution"
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
          )
        )
    }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(
      navigator.nextPage(MemberContributionsListPage(srn), mode, request.userAnswers)
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

object MemberContributionListController {

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[(Max300, NameDOB, Option[Money])],
    optYear: Option[String],
    optCurrentVersion: Option[Int],
    optPreviousVersion: Option[Int]
  ): List[List[TableElemBase]] = {
    val x = memberList
      .map {
        case (index, memberName, memberContribution) =>
          if (memberContribution.exists(!_.isZero)) {
            List(
              TableElem(memberName.fullName),
              TableElem("Member contributions reported"),
              TableElemDoubleLink(
                (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                  case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                    TableElem.view(
                      controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
                        .onPageLoadViewOnly(srn, index, year, currentVersion, previousVersion),
                      Message("ReportContribution.MemberList.remove.hidden.text", memberName.fullName)
                    )
                  case _ =>
                    TableElem.change(
                      controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
                        .onPageLoad(srn, index, CheckMode),
                      Message("ReportContribution.MemberList.change.hidden.text", memberName.fullName)
                    )
                },
                if (mode == ViewOnlyMode) {
                  TableElem.empty
                } else {
                  TableElem.remove(
                    controllers.nonsipp.membercontributions.routes.RemoveMemberContributionController
                      .onPageLoad(srn, index),
                    Message("ReportContribution.MemberList.remove.hidden.text", memberName.fullName)
                  )
                }
              )
            )
          } else {
            List(
              TableElem(memberName.fullName),
              TableElem("No member contributions"),
              if (mode != ViewOnlyMode) {
                TableElem.add(
                  controllers.nonsipp.membercontributions.routes.TotalMemberContributionController
                    .onSubmit(srn, index, mode),
                  Message("ReportContribution.MemberList.add.hidden.text", memberName.fullName)
                )
              } else {
                TableElem.empty
              }
            )
          }
      }
    x.sortBy(_.headOption.map(_.asInstanceOf[TableElem].text.toString))
  }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[(Max300, NameDOB)],
    userAnswers: UserAnswers,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    noPageEnabled: Boolean,
    showBackLink: Boolean = true
  ): FormPageViewModel[ActionTableViewModel] = {

    val (title, heading) =
      if (memberList.size == 1) {
        ("ReportContribution.MemberList.title", "ReportContribution.MemberList.heading")
      } else {
        ("ReportContribution.MemberList.title.plural", "ReportContribution.MemberList.heading.plural")
      }

    val membersWithContributions: List[(Max300, NameDOB, Option[Money])] = memberList
      .map {
        case (index, memberName) =>
          (index, memberName, userAnswers.get(TotalMemberContributionPage(srn, index)))
      }

    val sumMemberContributions = membersWithContributions.count {
      case (_, _, contribution) => contribution.exists(!_.isZero)
    }

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage =
      if ((page - 1) * Constants.memberContributionsMemberListSize >= memberList.size) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.memberContributionsMemberListSize,
      memberList.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    val optDescription = Option.when(mode == NormalMode)(
      ParagraphMessage(
        "ReportContribution.MemberList.paragraph1"
      ) ++
        ParagraphMessage(
          "ReportContribution.MemberList.paragraph2"
        )
    )

    FormPageViewModel(
      mode = mode,
      title = Message(title, memberList.size),
      heading = Message(heading, memberList.size),
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
        rows = rows(srn, mode, membersWithContributions, optYear, optCurrentVersion, optPreviousVersion),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "ReportContribution.MemberList.pagination.label",
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
      onSubmit =
        controllers.nonsipp.membercontributions.routes.MemberContributionListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "ReportContribution.MemberList.viewOnly.link",
                    controllers.nonsipp.membercontributions.routes.MemberContributionListController
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
            title = "ReportContribution.MemberList.viewOnly.title",
            heading = sumMemberContributions match {
              case 0 => Message("ReportContribution.MemberList.viewOnly.noContributions")
              case 1 => Message("ReportContribution.MemberList.viewOnly.singular")
              case _ => Message("ReportContribution.MemberList.viewOnly.plural", sumMemberContributions)
            },
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.membercontributions.routes.MemberContributionListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membercontributions.routes.MemberContributionListController
                  .onSubmit(srn, page, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("ReportContribution.MemberList.view.none")
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
