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

package controllers.nonsipp.membersurrenderedbenefits

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import cats.implicits.toShow
import pages.nonsipp.membersurrenderedbenefits._
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import config.RefinedTypes.OneTo300
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
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

import scala.concurrent.Future

import java.time.LocalDateTime
import javax.inject.Named

class SurrenderedBenefitsMemberListController @Inject()(
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
    val userAnswers = request.userAnswers
    val optionList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)
    if (optionList.flatten.nonEmpty) {
      val noPageEnabled = !userAnswers.get(SurrenderedBenefitsPage(srn)).getOrElse(false)
      val viewModel = SurrenderedBenefitsMemberListController
        .viewModel(
          srn,
          page,
          mode,
          optionList,
          userAnswers,
          viewOnlyUpdated = if (mode.isViewOnlyMode && request.previousUserAnswers.nonEmpty) {
            getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              request.previousUserAnswers.get,
              pages.nonsipp.membersurrenderedbenefits.Paths.memberPensionSurrender
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

      Ok(view(viewModel))
    } else {
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(
      navigator.nextPage(SurrenderedBenefitsMemberListPage(srn), mode, request.userAnswers)
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

object SurrenderedBenefitsMemberListController {

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
              val items = userAnswers.get(SurrenderedBenefitsAmountPage(srn, nextIndex))
              if (items.isEmpty) {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    Message("surrenderedBenefits.memberList.status.no.items")
                  ),
                  if (!mode.isViewOnlyMode) {
                    TableElem(
                      LinkMessage(
                        Message("site.add"),
                        controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsAmountController
                          .onSubmit(srn, nextIndex, mode)
                          .url
                      )
                    )
                  } else {
                    TableElem("")
                  },
                  TableElem("")
                )
              } else {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    Message("surrenderedBenefits.memberList.status.some.item", items.size)
                  ),
                  (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                    case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                      TableElem.view(
                        controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
                          .onPageLoadViewOnly(
                            srn,
                            nextIndex,
                            year = year,
                            current = currentVersion,
                            previous = previousVersion
                          ),
                        Message("surrenderedBenefits.memberList.add.hidden.text", memberName.fullName)
                      )
                    case _ =>
                      TableElem(
                        LinkMessage(
                          Message("site.change"),
                          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
                            .onPageLoad(srn, nextIndex, CheckMode)
                            .url
                        )
                      )
                  },
                  if (mode.isViewOnlyMode) {
                    TableElem("")
                  } else {
                    TableElem(
                      LinkMessage(
                        Message("site.remove"),
                        controllers.nonsipp.membersurrenderedbenefits.routes.RemoveSurrenderedBenefitsController
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
    val title = "surrenderedBenefits.memberList.title"
    val heading = "surrenderedBenefits.memberList.heading"

    val sumSurrenderedBenefits = memberList.flatten.indices.foldLeft(0) { (count, index) =>
      refineV[OneTo300](index + 1) match {
        case Right(memberIndex) =>
          userAnswers.get(SurrenderedBenefitsAmountPage(srn, memberIndex)) match {
            case Some(amount) if !amount.isZero => count + 1
            case _ => count
          }
        case Left(_) => count
      }
    }
    val memberListSize = memberList.flatten.size
    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.surrenderedBenefitsListSize >= memberListSize) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.surrenderedBenefitsListSize,
      memberListSize,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    val optDescription =
      Option.when(mode == NormalMode)(
        ParagraphMessage(
          "surrenderedBenefits.memberList.inset1"
        ) ++
          ParagraphMessage(
            "surrenderedBenefits.memberList.inset2"
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
            TableElem.empty,
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, userAnswers, optYear, optCurrentVersion, optPreviousVersion),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "surrenderedBenefits.memberList.pagination.label",
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
      onSubmit = controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
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
                    "surrenderedBenefits.memberList.viewOnly.link",
                    controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
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
            title = "surrenderedBenefits.memberList.viewOnly.title",
            heading = sumSurrenderedBenefits match {
              case 0 => Message("surrenderedBenefits.memberList.viewOnly.heading")
              case 1 => Message("surrenderedBenefits.memberList.viewOnly.singular")
              case _ => Message("surrenderedBenefits.memberList.viewOnly.plural", sumSurrenderedBenefits)
            },
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsMemberListController
                  .onSubmit(srn, page, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("surrenderedBenefits.memberList.view.none", schemeName)
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
