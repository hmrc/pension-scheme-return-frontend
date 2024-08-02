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

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.OneTo300
import controllers.PSRController
import cats.implicits.toShow
import config.Constants.maxNotRelevant
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.{refineMV, refineV}
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePages, TransferOutMemberListPage, TransfersOutJourneyStatus}
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.Named

class TransferOutMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = TransferOutMemberListController.form(formProvider)

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
      val viewModel = TransferOutMemberListController
        .viewModel(
          srn,
          page,
          mode,
          optionList,
          request.userAnswers,
          viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
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
          compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
        )
      val filledForm = request.userAnswers.fillForm(TransferOutMemberListPage(srn), form)
      Ok(view(filledForm, viewModel))
    } else {
      Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)

      if (optionList.flatten.size > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(TransferOutMemberListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        val viewModel =
          TransferOutMemberListController
            .viewModel(srn, page, mode, optionList, request.userAnswers, false, None, None, None)

        form
          .bindFromRequest()
          .fold(
            errors => Future.successful(BadRequest(view(errors, viewModel))),
            finishedAddingTransfers =>
              for {
                updatedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .set(
                        TransfersOutJourneyStatus(srn),
                        if (finishedAddingTransfers) SectionStatus.Completed
                        else SectionStatus.InProgress
                      )
                      .set(TransferOutMemberListPage(srn), finishedAddingTransfers)
                  )
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (finishedAddingTransfers)
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall = controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
                      .onPageLoad(srn, 1, NormalMode)
                  )
                else Future.successful(Some(()))
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(TransferOutMemberListPage(srn), mode, request.userAnswers)
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
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
    }

}

object TransferOutMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "transferOut.memberList.radios.error.required"
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
            case Right(nextIndex) =>
              val contributions = userAnswers.map(ReceivingSchemeNamePages(srn, nextIndex))
              if (contributions.isEmpty) {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    Message("transferOut.memberList.status.no.contributions")
                  ),
                  if (mode != ViewOnlyMode) {
                    TableElem(
                      LinkMessage(
                        Message("site.add"),
                        controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
                          .onSubmit(srn, nextIndex, refineMV(1), mode)
                          .url
                      )
                    )
                  } else {
                    TableElem.empty
                  },
                  TableElem("")
                )
              } else {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    if (contributions.size == 1)
                      Message("transferOut.memberList.singleStatus.some.contribution", contributions.size)
                    else
                      Message("transferOut.memberList.status.some.contributions", contributions.size)
                  ),
                  (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                    case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                      TableElem.view(
                        controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
                          .onPageLoadViewOnly(srn, nextIndex, year, currentVersion, previousVersion),
                        Message("transferOut.memberList.remove.hidden.text", memberName.fullName)
                      )
                    case _ =>
                      TableElem(
                        LinkMessage(
                          Message("site.change"),
                          controllers.nonsipp.membertransferout.routes.TransfersOutCYAController
                            .onPageLoad(srn, nextIndex, CheckMode)
                            .url
                        )
                      )
                  },
                  if (mode == ViewOnlyMode) {
                    TableElem.empty
                  } else {
                    TableElem(
                      LinkMessage(
                        Message("site.remove"),
                        controllers.nonsipp.membertransferout.routes.WhichTransferOutRemoveController
                          .onSubmit(srn, nextIndex)
                          .url
                      )
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
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ActionTableViewModel] = {
    val title =
      if (memberList.flatten.size == 1) "transferOut.memberList.title"
      else "transferOut.memberList.title.plural"

    val heading =
      if (memberList.flatten.size == 1) "transferOut.memberList.heading"
      else "transferOut.memberList.heading.plural"

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.landOrPropertiesSize >= memberList.flatten.size) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.transferOutListSize,
      memberList.flatten.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
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
          "transferOut.memberList.paragraph1"
        ) ++
          ParagraphMessage(
            "transferOut.memberList.paragraph2"
          ),
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, userAnswers, optYear, optCurrentVersion, optPreviousVersion),
        radioText = Message("transferOut.memberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
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
                  if (optYear.nonEmpty && currentVersion > 1 && previousVersion > 0) =>
                Some(
                  LinkMessage(
                    "transferOut.MemberList.viewOnly.link",
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
            title = "transferOut.MemberList.viewOnly.title",
            heading = "transferOut.MemberList.viewOnly.heading",
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
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
