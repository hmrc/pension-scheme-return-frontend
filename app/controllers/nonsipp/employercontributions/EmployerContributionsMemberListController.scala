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

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import cats.implicits.{catsSyntaxApplicativeId, toShow}
import _root_.config.Constants
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import pages.nonsipp.employercontributions._
import services.{PsrSubmissionService, SaveService}
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineMV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import forms.YesNoPageFormProvider
import controllers.nonsipp.employercontributions.EmployerContributionsMemberListController._
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models.SectionJourneyStatus.InProgress
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form
import pages.nonsipp.employercontributions.EmployerContributionsProgress.EmployerContributionsUserAnswersOps
import _root_.config.Refined.{Max300, Max50}

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.Named

class EmployerContributionsMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  view: TwoColumnsTripleAction,
  psrSubmissionService: PsrSubmissionService,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form: Form[Boolean] = EmployerContributionsMemberListController.form(formProvider)

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
    onPageLoadCommon(srn, page, mode)
  }

  private def onPageLoadCommon(srn: Srn, page: Int, mode: Mode)(implicit request: DataRequest[AnyContent]): Result = {
    val optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)
    if (optionList.flatten.nonEmpty) {
      optionList
        .zipWithRefinedIndex[Max300.Refined]
        .map { indexes =>
          val employerContributions = buildEmployerContributions(srn, indexes)
          val filledForm = request.userAnswers.fillForm(EmployerContributionsMemberListPage(srn), form)
          val noPageEnabled = !request.userAnswers.get(EmployerContributionsPage(srn)).getOrElse(false)
          Ok(
            view(
              filledForm,
              viewModel(
                srn,
                page,
                mode,
                employerContributions,
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
                noPageEnabled
              )
            )
          )
        }
        .merge
    } else {
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)

      if (optionList.flatten.size > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        form
          .bindFromRequest()
          .fold(
            errors => {
              val noPageEnabled = !request.userAnswers.get(EmployerContributionsPage(srn)).getOrElse(false)
              optionList
                .zipWithRefinedIndex[Max300.Refined]
                .map { indexes =>
                  val employerContributions = buildEmployerContributions(srn, indexes)
                  BadRequest(
                    view(
                      errors,
                      EmployerContributionsMemberListController
                        .viewModel(
                          srn,
                          page,
                          mode,
                          employerContributions,
                          viewOnlyUpdated = false,
                          None,
                          None,
                          None,
                          None,
                          noPageEnabled
                        )
                    )
                  )
                }
                .merge
                .pure[Future]
            },
            value =>
              for {
                updatedUserAnswers <- Future.fromTry(
                  request.userAnswers
                    .set(
                      EmployerContributionsSectionStatus(srn),
                      if (value) {
                        SectionStatus.Completed
                      } else {
                        SectionStatus.InProgress
                      }
                    )
                    .set(EmployerContributionsMemberListPage(srn), value)
                )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  updatedUserAnswers,
                  fallbackCall =
                    controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
                      .onPageLoad(srn, page, mode)
                )
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(EmployerContributionsMemberListPage(srn), mode, request.userAnswers)
                  )
              )
          )
      }
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }

  def onPreviousViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        ).flashing("showBackLink" -> "false")
      )
    }

  private def buildEmployerContributions(srn: Srn, indexes: List[(Max300, Option[NameDOB])])(
    implicit request: DataRequest[_]
  ): List[MemberWithEmployerContributions] = indexes.flatMap {
    case (index, Some(nameDOB)) =>
      Some(
        MemberWithEmployerContributions(
          memberIndex = index,
          employerFullName = nameDOB.fullName,
          contributions = request.userAnswers
            .employerContributionsProgress(srn, index)
            .map {
              case (secondaryIndex, status) =>
                EmployerContributions(secondaryIndex, status)
            }
        )
      )
    case _ => None
  }
}

object EmployerContributionsMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "employerContributions.MemberList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberWithEmployerContributions: List[MemberWithEmployerContributions],
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[List[TableElem]] =
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
                case None =>
                  controllers.nonsipp.employercontributions.routes.EmployerNameController
                    .onSubmit(srn, memberWithEmployerContributions.memberIndex, refineMV(1), mode)
                    .url
              },
              Message(
                "employerContributions.MemberList.add.hidden.text",
                memberWithEmployerContributions.employerFullName
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
    noPageEnabled: Boolean
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

    FormPageViewModel(
      mode = mode,
      title = Message(title, employerContributions.size),
      heading = Message(heading, employerContributions.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage(
          "employerContributions.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "employerContributions.MemberList.paragraph2"
          ),
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
          employerContributions.sortBy(_.employerFullName),
          optYear,
          optCurrentVersion,
          optPreviousVersion
        ),
        radioText = Message("employerContributions.MemberList.radios"),
        showInsetWithRadios = true,
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
        .onSubmit(srn, page, mode),
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
                  .onSubmit(srn, page, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("employerContributions.MemberList.view.none")
            )
          )
        )
      } else {
        None
      }
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
