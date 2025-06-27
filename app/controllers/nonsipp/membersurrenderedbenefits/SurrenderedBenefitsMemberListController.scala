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

import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsMemberListController._
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import utils.IntUtils.toInt
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.membersurrenderedbenefits._
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import pages.nonsipp.membersurrenderedbenefits.MemberSurrenderedBenefitsProgress.SurrenderedBenefitsUserAnswersOps
import config.RefinedTypes.Max300
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
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

class SurrenderedBenefitsMemberListController @Inject() (
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
    onPageLoadCommon(srn, page, mode, showBackLink = true)
  }

  private def onPageLoadCommon(srn: Srn, page: Int, mode: Mode, showBackLink: Boolean)(implicit
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
        val noPageEnabled = !request.userAnswers.get(SurrenderedBenefitsPage(srn)).getOrElse(false)
        val memberDetailsWithAmount: List[(Max300, NameDOB, Option[SurrenderedBenefitsAmount])] =
          completedMemberDetails.map { case (index, memberDetails) =>
            (index, memberDetails, request.userAnswers.get(SurrenderedBenefitsAmountPage(srn, index)))
          }

        val memberDetails = buildSurrenderedBenefits(srn, memberDetailsWithAmount)

        Ok(
          view(
            viewModel(
              srn,
              page,
              mode,
              memberList = memberDetails,
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
          )
        )
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
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

  private def buildSurrenderedBenefits(
    srn: Srn,
    indexes: List[(Max300, NameDOB, Option[SurrenderedBenefitsAmount])]
  )(implicit request: DataRequest[?]): List[MemberSurrenderedBenefits] =
    indexes.map { case (index, nameDOB, surrenderedAccountOpt) =>
      val status = request.userAnswers.memberSurrenderedBenefitsProgress(srn, index)
      MemberSurrenderedBenefits(
        memberIndex = index,
        surrenderedFullName = nameDOB.fullName,
        status = status,
        surrenderedBenefitsAccount = surrenderedAccountOpt
      )
    }

}

object SurrenderedBenefitsMemberListController {

  private[membersurrenderedbenefits] type SurrenderedBenefitsAmount = Money

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[MemberSurrenderedBenefits],
    optYear: Option[String],
    optCurrentVersion: Option[Int],
    optPreviousVersion: Option[Int]
  ): List[List[TableElemBase]] =
    memberList
      .map {
        case MemberSurrenderedBenefits(_, memberName, Some(SectionJourneyStatus.InProgress(url)), _) =>
          List(
            TableElem(memberName),
            TableElem("surrenderedBenefits.memberList.status.no.items"),
            if (!mode.isViewOnlyMode) {
              TableElem.add(
                url,
                Message(
                  "surrenderedBenefits.memberList.add.hidden.text",
                  memberName
                )
              )
            } else {
              TableElem.empty
            }
          )
        case MemberSurrenderedBenefits(index, memberName, _, None) =>
          List(
            TableElem(memberName),
            TableElem("surrenderedBenefits.memberList.status.no.items"),
            if (!mode.isViewOnlyMode) {
              TableElem.add(
                controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsAmountController
                  .onSubmit(srn, index, mode),
                Message(
                  "surrenderedBenefits.memberList.add.hidden.text",
                  memberName
                )
              )
            } else {
              TableElem.empty
            }
          )

        case MemberSurrenderedBenefits(index, memberName, _, Some(_)) =>
          val viewOrChangeLink =
            (mode, optYear, optCurrentVersion, optPreviousVersion) match {
              case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                TableElem.view(
                  controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
                    .onPageLoadViewOnly(
                      srn,
                      index,
                      year = year,
                      current = currentVersion,
                      previous = previousVersion
                    ),
                  hiddenText = Message(
                    "surrenderedBenefits.memberList.add.hidden.text",
                    memberName
                  )
                )

              case _ =>
                TableElem.change(
                  controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsCYAController
                    .onPageLoad(srn, index, CheckMode),
                  Message(
                    "surrenderedBenefits.memberList.change.hidden.text",
                    memberName
                  )
                )
            }

          val removeLink =
            if (mode.isViewOnlyMode) {
              TableElem.empty
            } else {
              TableElem.remove(
                controllers.nonsipp.membersurrenderedbenefits.routes.RemoveSurrenderedBenefitsController
                  .onSubmit(srn, index),
                Message(
                  "surrenderedBenefits.memberList.remove.hidden.text",
                  memberName
                )
              )
            }

          List(
            TableElem(memberName),
            TableElem("surrenderedBenefits.memberList.status.some.item"),
            TableElemDoubleLink((viewOrChangeLink, removeLink))
          )
      }
      .sortBy(_.headOption.map(_.asInstanceOf[TableElem].text.toString))

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[MemberSurrenderedBenefits],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    schemeName: String,
    noPageEnabled: Boolean,
    showBackLink: Boolean = true
  ): FormPageViewModel[ActionTableViewModel] = {

    val sumSurrenderedBenefits = memberList.count {
      case MemberSurrenderedBenefits(_, _, _, Some(amount)) => !amount.isZero
      case MemberSurrenderedBenefits(_, _, _, None) => false
    }

    val memberListSize = memberList.size
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

    val description =
      Option.when(mode.isNormalMode)(
        ParagraphMessage(
          "surrenderedBenefits.memberList.inset1"
        ) ++
          ParagraphMessage(
            "surrenderedBenefits.memberList.inset2"
          )
      )

    FormPageViewModel(
      mode = mode,
      title = Message("surrenderedBenefits.memberList.title", memberListSize),
      heading = Message("surrenderedBenefits.memberList.heading", memberListSize),
      description = description,
      page = ActionTableViewModel(
        inset = "",
        head = Some(
          List(
            TableElem("memberList.memberName"),
            TableElem("memberList.status"),
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, optYear, optCurrentVersion, optPreviousVersion),
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
        .onSubmit(srn, mode),
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
                  .onSubmit(srn, mode)
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

  case class MemberSurrenderedBenefits(
    memberIndex: Max300,
    surrenderedFullName: String,
    status: Option[SectionJourneyStatus],
    surrenderedBenefitsAccount: Option[SurrenderedBenefitsAmount]
  )
}
