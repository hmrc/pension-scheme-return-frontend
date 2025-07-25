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

package controllers.nonsipp.employercontributions

import pages.nonsipp.employercontributions._
import viewmodels.implicits._
import play.api.mvc._
import org.slf4j.LoggerFactory
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import utils.IntUtils.toInt
import cats.implicits.toShow
import controllers.actions._
import controllers.nonsipp.employercontributions.EmployerContributionsMemberListController._
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import pages.nonsipp.employercontributions.EmployerContributionsProgress.EmployerContributionsUserAnswersOps
import _root_.config.RefinedTypes.{Max300, Max50}
import com.google.inject.Inject
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import _root_.config.Constants
import utils.StringUtils.LastName
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

class EmployerContributionsMemberListController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction
) extends PSRController {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, page, mode)
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

  private def onPageLoadCommon(srn: Srn, page: Int, mode: Mode, showBackLink: Boolean = true)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    request.userAnswers.completedMembersDetails(srn) match {
      case Left(err) =>
        logger.warn(s"Error when fetching completed member details - $err")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(Nil) =>
        logger.warn(s"No completed member details for srn $srn")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(completedMemberDetails) =>
        val noPageEnabled = !request.userAnswers.get(EmployerContributionsPage(srn)).getOrElse(false)
        Ok(
          view(
            viewModel(
              srn,
              page,
              mode,
              employerContributions = buildEmployerContributions(completedMemberDetails),
              viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getCompletedOrUpdatedTaskListStatus(
                  request.userAnswers,
                  request.previousUserAnswers.get,
                  pages.nonsipp.employercontributions.Paths.memberEmpContribution
                ) == Updated
              } else {
                false
              },
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
              noPageEnabled,
              showBackLink = showBackLink
            )
          )
        )
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(
      navigator.nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
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

  private def buildEmployerContributions(indexes: List[(Max300, NameDOB)])(implicit
    request: DataRequest[?]
  ): List[MemberWithEmployerContributions] = indexes.map { case (index, nameDOB) =>
    MemberWithEmployerContributions(
      memberIndex = index,
      employerFullName = nameDOB.fullName,
      contributions = request.userAnswers
        .employerContributionsProgress(index)
        .map { case (secondaryIndex, status) =>
          EmployerContributions(secondaryIndex, status)
        }
    )
  }
}

object EmployerContributionsMemberListController {

  private def rows(
    srn: Srn,
    mode: Mode,
    memberWithEmployerContributions: List[MemberWithEmployerContributions],
    optYear: Option[String],
    optCurrentVersion: Option[Int],
    optPreviousVersion: Option[Int]
  ): List[List[TableElemBase]] =
    memberWithEmployerContributions.map { memberWithEmployerContributions =>
      val noContributions = memberWithEmployerContributions.contributions.isEmpty
      val onlyInProgressContributions = memberWithEmployerContributions.contributions.forall(_.status.inProgress)

      if (noContributions || onlyInProgressContributions) {
        List(
          TableElem(
            memberWithEmployerContributions.employerFullName
          ),
          TableElem(
            Message("employerContributions.MemberList.status.no.contributions")
          ),
          if (mode != ViewOnlyMode) {
            TableElem.add(
              memberWithEmployerContributions.contributions.find(_.status.inProgress) match {
                case Some(EmployerContributions(_, InProgress(url))) => url
                case _ =>
                  controllers.nonsipp.employercontributions.routes.EmployerNameController
                    .onSubmit(srn, memberWithEmployerContributions.memberIndex, 1, mode)
                    .url
              },
              Message(
                "employerContributions.MemberList.add.hidden.text",
                memberWithEmployerContributions.employerFullName
              )
            )
          } else {
            TableElem.empty
          }
        )
      } else {
        List(
          TableElem(
            memberWithEmployerContributions.employerFullName
          ),
          TableElem(
            if (memberWithEmployerContributions.contributions.size == 1) {
              Message(
                "employerContributions.MemberList.status.single.contribution",
                memberWithEmployerContributions.contributions.size
              )
            } else {
              Message(
                "employerContributions.MemberList.status.some.contributions",
                memberWithEmployerContributions.contributions.count(_.status.completed)
              )
            }
          ),
          TableElemDoubleLink(
            (
              // Change link
              (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                  TableElem.view(
                    controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                      .onPageLoadViewOnly(
                        srn,
                        memberWithEmployerContributions.memberIndex,
                        page = 1,
                        year = year,
                        current = currentVersion,
                        previous = previousVersion
                      ),
                    Message(
                      "employerContributions.MemberList.remove.hidden.text",
                      memberWithEmployerContributions.employerFullName
                    )
                  )
                case _ =>
                  TableElem.change(
                    controllers.nonsipp.employercontributions.routes.EmployerContributionsCYAController
                      .onSubmit(srn, memberWithEmployerContributions.memberIndex, page = 1, CheckMode),
                    Message(
                      "employerContributions.MemberList.change.hidden.text",
                      memberWithEmployerContributions.employerFullName
                    )
                  )
              },
              // Remove link
              if (mode == ViewOnlyMode) {
                TableElem.empty
              } else {
                TableElem.remove(
                  controllers.nonsipp.employercontributions.routes.WhichEmployerContributionRemoveController
                    .onSubmit(srn, memberWithEmployerContributions.memberIndex),
                  Message(
                    "employerContributions.MemberList.remove.hidden.text",
                    memberWithEmployerContributions.employerFullName
                  )
                )
              }
            )
          )
        )
      }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    employerContributions: List[MemberWithEmployerContributions],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    noPageEnabled: Boolean,
    showBackLink: Boolean = true
  ): FormPageViewModel[ActionTableViewModel] = {

    val (title, heading) = if (employerContributions.size == 1) {
      ("employerContributions.MemberList.title", "employerContributions.MemberList.heading")
    } else {
      ("employerContributions.MemberList.title.plural", "employerContributions.MemberList.heading.plural")
    }
    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage =
      if ((page - 1) * Constants.employerContributionsMemberListSize >= employerContributions.size) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.employerContributionsMemberListSize,
      employerContributions.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    val optDescription = Option.when(mode == NormalMode)(
      ParagraphMessage(
        "employerContributions.MemberList.paragraph1"
      ) ++
        ParagraphMessage(
          "employerContributions.MemberList.paragraph2"
        )
    )

    FormPageViewModel(
      mode = mode,
      title = Message(title, employerContributions.size),
      heading = Message(heading, employerContributions.size),
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
        rows = rows(
          srn,
          mode,
          employerContributions.sortBy(_.employerFullName.lastName),
          optYear,
          optCurrentVersion,
          optPreviousVersion
        ),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "employerContributions.MemberList.pagination.label",
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
      onSubmit = controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
        .onSubmit(srn, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        val sumContributions: Int = employerContributions.map(_.contributions.size).sum
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "employerContributions.MemberList.viewOnly.link",
                    controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
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
            title = "employerContributions.MemberList.viewOnly.title",
            heading = sumContributions match {
              case 0 => Message("employerContributions.MemberList.viewOnly.noContributions")
              case 1 => Message("employerContributions.MemberList.viewOnly.singular")
              case _ => Message("employerContributions.MemberList.viewOnly.plural", sumContributions)
            },
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
                  .onSubmit(srn, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("employerContributions.MemberList.view.none")
            )
          )
        )
      } else {
        None
      },
      showBackLink = showBackLink
    )
  }

  protected[employercontributions] case class MemberWithEmployerContributions(
    memberIndex: Max300,
    employerFullName: String,
    contributions: List[EmployerContributions]
  )

  protected[employercontributions] case class EmployerContributions(
    contributionIndex: Max50,
    status: SectionJourneyStatus
  )
}
