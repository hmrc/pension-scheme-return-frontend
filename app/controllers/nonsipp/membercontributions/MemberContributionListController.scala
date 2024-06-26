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

import services.{PsrSubmissionService, SaveService}
import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import controllers.nonsipp.membercontributions.MemberContributionListController._
import cats.implicits.{toBifunctorOps, toShow, toTraverseOps}
import _root_.config.Constants
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import _root_.config.Refined.OneTo300
import viewmodels.implicits._
import pages.nonsipp.membercontributions.{MemberContributionsListPage, TotalMemberContributionPage}
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.Named

class MemberContributionListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = MemberContributionListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      onPageLoadCommon(srn, page, mode)(implicitly)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
    onPageLoadCommon(srn, page, mode)(implicitly)
  }

  def onPageLoadCommon(srn: Srn, page: Int, mode: Mode)(implicit request: DataRequest[AnyContent]): Result = {
    val optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)

    if (optionList.flatten.nonEmpty) {
      val filledForm = request.userAnswers.get(MemberContributionsListPage(srn)).fold(form)(form.fill)
      Ok(
        view(
          filledForm,
          viewModel(
            srn,
            page,
            mode,
            optionList,
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
            compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
          )
        )
      )
    } else {
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val userAnswers = request.userAnswers
      val optionList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

      if (optionList.flatten.size > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(MemberContributionsListPage(srn), mode, request.userAnswers)
          )
        )
      } else {

        form
          .bindFromRequest()
          .fold(
            errors =>
              Future.successful(
                BadRequest(
                  view(
                    errors,
                    MemberContributionListController
                      .viewModel(srn, page, mode, optionList, userAnswers, false, None, None, None)
                  )
                )
              ),
            value =>
              for {
                updatedUserAnswers <- buildUserAnswerBySelection(srn, value, optionList.flatten.size)
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (value) {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall = controllers.nonsipp.membercontributions.routes.MemberContributionListController
                      .onPageLoad(srn, page, mode)
                  )
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(MemberContributionsListPage(srn), mode, request.userAnswers)
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
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }

  private def buildUserAnswerBySelection(srn: Srn, selection: Boolean, memberListSize: Int)(
    implicit request: DataRequest[_]
  ): Future[UserAnswers] = {
    val userAnswerWithMemberContList = request.userAnswers.set(MemberContributionsListPage(srn), selection)

    if (selection) {
      val indexes = (1 to memberListSize)
        .map(i => refineV[OneTo300](i).leftMap(new Exception(_)).toTry)
        .toList
        .sequence

      Future.fromTry(
        indexes.fold(
          _ => userAnswerWithMemberContList,
          index =>
            index.foldLeft(userAnswerWithMemberContList) {
              case (uaTry, index) =>
                val optTotalMemberContribution = request.userAnswers.get(TotalMemberContributionPage(srn, index))
                for {
                  ua <- uaTry
                  ua1 <- ua.set(TotalMemberContributionPage(srn, index), optTotalMemberContribution.getOrElse(Money(0)))
                } yield ua1
            }
        )
      )
    } else {
      Future.fromTry(userAnswerWithMemberContList)
    }
  }
}

object MemberContributionListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "ReportContribution.MemberList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[Option[NameDOB]],
    userAnswers: UserAnswers,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[List[TableElem]] =
    memberList.zipWithIndex
      .map {
        case (Some(memberName), index) =>
          refineV[OneTo300](index + 1) match {
            case Left(_) => Nil
            case Right(index) =>
              val contributions = userAnswers.get(TotalMemberContributionPage(srn, index))
              if (contributions.nonEmpty && !contributions.exists(_.isZero)) {
                List(
                  TableElem(memberName.fullName),
                  TableElem("Member contributions reported"),
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
                  },
                  TableElem.empty
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
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ActionTableViewModel] = {

    val (title, heading) =
      if (memberList.flatten.size == 1) {
        ("ReportContribution.MemberList.title", "ReportContribution.MemberList.heading")
      } else {
        ("ReportContribution.MemberList.title.plural", "ReportContribution.MemberList.heading.plural")
      }

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.landOrPropertiesSize >= memberList.flatten.size) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.landOrPropertiesSize,
      memberList.flatten.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    FormPageViewModel(
      mode = mode,
      title = Message(title, memberList.flatten.size),
      heading = Message(heading, memberList.flatten.size),
      description = None,
      page = ActionTableViewModel(
        inset = ParagraphMessage(
          "ReportContribution.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "ReportContribution.MemberList.paragraph2"
          ),
        showInsetWithRadios = true,
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, userAnswers, optYear, optCurrentVersion, optPreviousVersion),
        radioText = Message("ReportContribution.MemberList.radios"),
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
                  if (optYear.nonEmpty && currentVersion > 1 && previousVersion > 0) =>
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
            heading = "ReportContribution.MemberList.viewOnly.heading",
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.membercontributions.routes.MemberContributionListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membercontributions.routes.MemberContributionListController
                  .onSubmit(srn, page, mode)
            }
          )
        )
      } else {
        None
      }
    )
  }
}
