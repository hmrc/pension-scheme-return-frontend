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

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.{Max300, OneTo300}
import controllers.PSRController
import config.Constants.maxNotRelevant
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import cats.implicits.toShow
import pages.nonsipp.receivetransfer._
import controllers.actions._
import eu.timepit.refined.{refineMV, refineV}
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

class TransferReceivedMemberListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TwoColumnsTripleAction,
  saveService: SaveService,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[Boolean] = TransferReceivedMemberListController.form(formProvider)

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
    val optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)

    if (optionList.flatten.nonEmpty) {
      val noPageEnabled = !request.userAnswers.get(DidSchemeReceiveTransferPage(srn)).getOrElse(false)
      val viewModel = TransferReceivedMemberListController
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
      val filledForm =
        request.userAnswers.get(TransferReceivedMemberListPage(srn)).fold(form)(form.fill)
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
            navigator.nextPage(TransferReceivedMemberListPage(srn), mode, request.userAnswers)
          )
        )
      } else {
        val noPageEnabled = !request.userAnswers.get(DidSchemeReceiveTransferPage(srn)).getOrElse(false)
        val viewModel =
          TransferReceivedMemberListController
            .viewModel(
              srn,
              page,
              mode,
              optionList,
              request.userAnswers,
              viewOnlyUpdated = false,
              None,
              None,
              None,
              None,
              request.schemeDetails.schemeName,
              noPageEnabled
            )

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
                        TransfersInJourneyStatus(srn),
                        if (finishedAddingTransfers) SectionStatus.Completed
                        else SectionStatus.InProgress
                      )
                      .set(TransferReceivedMemberListPage(srn), finishedAddingTransfers)
                  )
                _ <- saveService.save(updatedUserAnswers)
                _ <- if (finishedAddingTransfers)
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall = controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
                      .onPageLoad(srn, page, mode)
                  )
                else Future.successful(Some(()))
              } yield Redirect(
                navigator
                  .nextPage(TransferReceivedMemberListPage(srn), mode, updatedUserAnswers)
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
    identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) { implicit request =>
      val showBackLink = false
      onPageLoadCommon(srn, page, ViewOnlyMode, showBackLink)
    }

}

object TransferReceivedMemberListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "transferIn.MemberList.radios.error.required"
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
              val contributions = userAnswers.map(TransfersInSectionCompletedForMember(srn, nextIndex))
              if (contributions.isEmpty) {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    Message("transferIn.MemberList.status.no.contributions")
                  ),
                  if (mode != ViewOnlyMode) {
                    TableElem(
                      LinkMessage(
                        Message("site.add"),
                        controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
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
                      Message("transferIn.MemberList.singleStatus.some.contribution", contributions.size)
                    else
                      Message("transferIn.MemberList.status.some.contributions", contributions.size)
                  ),
                  (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                    case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                      TableElem.view(
                        controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
                          .onPageLoadViewOnly(srn, nextIndex, year, currentVersion, previousVersion),
                        Message("transferIn.memberList.remove.hidden.text", memberName.fullName)
                      )
                    case _ =>
                      TableElem(
                        LinkMessage(
                          Message("site.change"),
                          controllers.nonsipp.receivetransfer.routes.TransfersInCYAController
                            .onSubmit(srn, nextIndex, CheckMode)
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
                        controllers.nonsipp.receivetransfer.routes.WhichTransferInRemoveController
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
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    schemeName: String,
    noPageEnabled: Boolean,
    showBackLink: Boolean = true
  ): FormPageViewModel[ActionTableViewModel] = {

    val (title, heading) =
      if (memberList.flatten.size == 1) {
        ("transferIn.MemberList.title", "transferIn.MemberList.heading")
      } else {
        ("transferIn.MemberList.title.plural", "transferIn.MemberList.heading.plural")
      }

    val membersWithTransfers: List[(Max300, NameDOB, Int)] = memberList.zipWithIndex
      .flatMap {
        case (Some(memberName), index) =>
          refineV[OneTo300](index + 1) match {
            case Right(refinedIndex) =>
              val transferStatus =
                userAnswers.get(TransfersInSectionCompletedForMember(srn, refinedIndex)).getOrElse(Map.empty)
              val completedTransfers = transferStatus.values.count(_ == SectionCompleted)
              List((refinedIndex, memberName, completedTransfers))
            case Left(_) => List.empty
          }
        case _ => List.empty
      }
    val totalTransfersIn: Int = membersWithTransfers.map(_._3).sum

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.transferInListSize >= memberList.flatten.size) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.transferInListSize,
      memberList.flatten.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    FormPageViewModel(
      mode = mode,
      title = Message(title, memberList.flatten.size),
      heading = Message(heading, memberList.flatten.size),
      description = Some(
        ParagraphMessage(
          "transferIn.MemberList.paragraph1"
        ) ++
          ParagraphMessage(
            "transferIn.MemberList.paragraph2"
          )
      ),
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
        radioText = Message("transferIn.MemberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
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
}
