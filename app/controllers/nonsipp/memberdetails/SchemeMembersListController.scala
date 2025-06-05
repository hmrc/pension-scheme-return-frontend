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

package controllers.nonsipp.memberdetails

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MembersDetailsChecked, MembersDetailsCompletedPages, SchemeMembersListPage}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import utils.IntUtils.toInt
import cats.implicits.{catsSyntaxApplicativeId, toShow}
import config.Constants.maxSchemeMembers
import forms.YesNoPageFormProvider
import play.api.i18n.MessagesApi
import config.RefinedTypes.Max300
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.actions._
import eu.timepit.refined.refineV
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.models.TaskListStatus.Updated
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.Named

class SchemeMembersListController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val logger: Logger = Logger(classOf[SchemeMembersListController])

  private def form(manualOrUpload: ManualOrUpload, maxNumberReached: Boolean): Form[Boolean] =
    SchemeMembersListController.form(formProvider, manualOrUpload, maxNumberReached)

  def onPageLoadViewOnly(
    srn: Srn,
    page: Int,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous) { implicit request =>
      val showBackLink = true
      onPageLoadCommon(srn, page, ManualOrUpload.Manual, ViewOnlyMode, showBackLink)
    }

  def onPageLoad(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, page, manualOrUpload, mode, showBackLink = true)
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
      onPageLoadCommon(srn, page, ManualOrUpload.Manual, ViewOnlyMode, showBackLink)
    }

  def onPageLoadCommon(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode, showBackLink: Boolean)(implicit
    request: DataRequest[_]
  ): Result = {
    val completedMembers = request.userAnswers.get(MembersDetailsCompletedPages(srn)).getOrElse(Map.empty)
    val membersDetails = request.userAnswers.membersDetails(srn)
    val completedMembersDetails = completedMembers.keySet
      .intersect(membersDetails.keySet)
      .map(k => k -> membersDetails(k))
      .toMap
    if (membersDetails.isEmpty) {
      logger.error(s"no members found")
      Redirect(routes.PensionSchemeMembersController.onPageLoad(srn))
    } else {

      completedMembersDetails.view
        .mapValues(_.fullName)
        .toList
        .zipWithRefinedIndex[Max300.Refined]
        .map { filteredMembers =>
          Ok(
            view(
              form(manualOrUpload, filteredMembers.size >= maxSchemeMembers),
              viewModel(
                srn,
                page,
                manualOrUpload,
                mode,
                filteredMembers,
                viewOnlyUpdated = (mode, request.previousUserAnswers) match {
                  case (ViewOnlyMode, Some(previousUserAnswers)) =>
                    val updated = getCompletedOrUpdatedTaskListStatus(
                      request.userAnswers,
                      previousUserAnswers,
                      pages.nonsipp.memberdetails.Paths.personalDetails
                    ) == Updated
                    updated
                  case (ViewOnlyMode, None) =>
                    false
                  case _ => false
                },
                optYear = request.year,
                optCurrentVersion = request.currentVersion,
                optPreviousVersion = request.previousVersion,
                compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
                showBackLink = showBackLink,
                prePopNotChecked = isPrePopulation && request.userAnswers
                  .get(MembersDetailsChecked(srn))
                  .contains(false)
              )
            )
          )
        }
        .merge
    }
  }

  def onSubmit(srn: Srn, page: Int, manualOrUpload: ManualOrUpload, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val membersDetails = request.userAnswers.membersDetails(srn)
      val lengthOfMembersDetails = membersDetails.size

      form(manualOrUpload, lengthOfMembersDetails >= maxSchemeMembers)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            membersDetails.view
              .mapValues(_.fullName)
              .toList
              .zipWithRefinedIndex[Max300.Refined]
              .map { filteredMembers =>
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(
                      srn,
                      page,
                      manualOrUpload,
                      mode,
                      filteredMembers,
                      viewOnlyUpdated = false,
                      None,
                      None,
                      None
                    )
                  )
                )
              }
              .merge
              .pure[Future],
          value => {
            val notChecked = isPrePopulation && request.userAnswers.get(MembersDetailsChecked(srn)).contains(false)

            if (notChecked) {
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers
                    .set(MembersDetailsChecked(srn), true)
                )
                _ <- saveService.save(updatedAnswers)
                _ <- psrSubmissionService
                  .submitPsrDetailsWithUA(
                    srn,
                    updatedAnswers,
                    fallbackCall = controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
                  )
              } yield ()
            }

            // the value that indicates that the section is completed is mapped to 'no' in the UI for the 'check' scenario
            val completed = if (notChecked) !value else value

            if (lengthOfMembersDetails == maxSchemeMembers && completed) {
              Future.successful(Redirect(routes.HowToUploadController.onPageLoad(srn)))
            } else {
              Future.successful(
                Redirect(
                  navigator.nextPage(SchemeMembersListPage(srn, completed, manualOrUpload), mode, request.userAnswers)
                )
              )
            }
          }
        )
    }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }
}

object SchemeMembersListController {
  def form(
    formProvider: YesNoPageFormProvider,
    manualOrUpload: ManualOrUpload,
    maxNumberReached: Boolean = false
  ): Form[Boolean] = formProvider(
    manualOrUpload.fold(
      manual = if (maxNumberReached) "membersUploaded.error.required" else "schemeMembersList.error.required",
      upload = "membersUploaded.error.required"
    )
  )

  def viewModel(
    srn: Srn,
    page: Int,
    manualOrUpload: ManualOrUpload,
    mode: Mode,
    filteredMembers: List[(Max300, (String, String))],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    showBackLink: Boolean = true,
    prePopNotChecked: Boolean = false
  ): FormPageViewModel[ListViewModel] = {

    val lengthOfFilteredMembers = filteredMembers.length

    val rows: List[ListRow] = filteredMembers
      .sortBy { case (_, (_, name)) => name }
      .flatMap { case (_, (unrefinedIndex, memberFullName)) =>
        val index = refineV[Max300.Refined](unrefinedIndex.toInt + 1).toOption.get
        if (mode.isViewOnlyMode) {
          (mode, optYear, optCurrentVersion, optPreviousVersion) match {
            case (ViewOnlyMode, Some(year), Some(current), Some(previous)) =>
              List(
                ListRow.view(
                  memberFullName,
                  routes.SchemeMemberDetailsAnswersController
                    .onPageLoadViewOnly(srn, index, year, current, previous)
                    .url,
                  Message("site.view.param", memberFullName)
                )
              )
            case _ => Nil
          }
        } else {
          List(
            ListRow(
              memberFullName,
              changeUrl = routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, CheckMode).url,
              changeHiddenText = Message("schemeMembersList.change.hidden", memberFullName),
              removeUrl = routes.RemoveMemberDetailsController.onPageLoad(srn, index, mode).url,
              removeHiddenText = Message("schemeMembersList.remove.hidden", memberFullName)
            )
          )
        }
      }

    val (title, heading) = ((mode, lengthOfFilteredMembers) match {
      case (ViewOnlyMode, lengthOfFilteredMembers) if lengthOfFilteredMembers > 1 =>
        ("schemeMembersList.view.title.plural", "schemeMembersList.view.heading.plural")
      case (ViewOnlyMode, _) =>
        ("schemeMembersList.view.title", "schemeMembersList.view.heading")
      case (_, lengthOfFilteredMembers) if lengthOfFilteredMembers > 1 =>
        ("schemeMembersList.title.plural", "schemeMembersList.heading.plural")
      case _ =>
        ("schemeMembersList.title", "schemeMembersList.heading")
    }) match {
      case (title, heading) => (Message(title, lengthOfFilteredMembers), Message(heading, lengthOfFilteredMembers))
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.schemeMembersPageSize,
      rows.size,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          routes.SchemeMembersListController
            .onPreviousViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.SchemeMembersListController.onPageLoad(srn, _, manualOrUpload)
      }
    )

    val radioText = (prePopNotChecked, manualOrUpload) match {
      case (true, _) => "schemeMembersList.notchecked.radio"
      case (false, ManualOrUpload.Upload) => "membersUploaded.radio"
      case (false, ManualOrUpload.Manual) =>
        if (lengthOfFilteredMembers < Constants.maxSchemeMembers) "schemeMembersList.radio" else "membersUploaded.radio"
    }

    val hintText: Option[Message] = manualOrUpload.fold(
      upload = Some(Message("membersUploaded.radio.yes.hint")),
      manual = if (lengthOfFilteredMembers < Constants.maxSchemeMembers) {
        None
      } else {
        Some(Message("membersUploaded.radio.yes.hint"))
      }
    )

    val yesHintText = if (!prePopNotChecked) hintText else None
    val noHintText = if (prePopNotChecked) hintText else None

    val radioYesMessage = Option.when(prePopNotChecked)(Message("schemeMembersList.notchecked.option1"))
    val radioNoMessage = Option.when(prePopNotChecked)(Message("schemeMembersList.notchecked.option2"))

    val description =
      if (prePopNotChecked) ParagraphMessage("schemeMembersList.paragraph.prepop")
      else ParagraphMessage("schemeMembersList.paragraph")

    FormPageViewModel(
      title = title,
      heading = heading,
      description = Option
        .when(lengthOfFilteredMembers < Constants.maxSchemeMembers)(description),
      page = ListViewModel(
        inset = "schemeMembersList.inset",
        sections = List(ListSection(rows)),
        radioText,
        showInsetWithRadios = !mode.isViewOnlyMode && lengthOfFilteredMembers == Constants.maxSchemeMembers,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "schemeMembersList.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        ),
        yesHintText = yesHintText,
        noHintText = noHintText,
        radioYesMessage = radioYesMessage,
        radioNoMessage = radioNoMessage
      ),
      refresh = None,
      Message("site.saveAndContinue"),
      details = None,
      onSubmit = routes.SchemeMembersListController.onSubmit(srn, page, manualOrUpload),
      mode = mode,
      optViewOnlyDetails = Option.when(mode.isViewOnlyMode) {
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if currentVersion > 1 && previousVersion > 0 =>
              Some(
                LinkMessage(
                  "schemeMembersList.view.link",
                  routes.SchemeMembersListController
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
          title = title,
          heading = heading,
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
      },
      showBackLink = showBackLink
    )
  }
}
