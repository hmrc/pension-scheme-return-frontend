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

package controllers.nonsipp.memberpensionpayments

import viewmodels.implicits._
import play.api.mvc._
import org.slf4j.LoggerFactory
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import utils.IntUtils.toInt
import cats.implicits.toShow
import _root_.config.Constants
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import _root_.config.RefinedTypes.{Max300, OneTo300}
import com.google.inject.Inject
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments._
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

class MemberPensionPaymentsListController @Inject() (
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
  ): Result = {
    val userAnswers = request.userAnswers
    request.userAnswers.completedMembersDetails(srn) match {
      case Left(err) =>
        logger.warn(s"Error when fetching completed member details - $err")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(Nil) =>
        logger.warn(s"No completed member details for srn $srn")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(completedMemberDetails) =>
        val noPageEnabled = !userAnswers.get(PensionPaymentsReceivedPage(srn)).getOrElse(false)
        val memberDetails = completedMemberDetails.map { case (index, memberDetails) =>
          (index, memberDetails, userAnswers.get(TotalAmountPensionPaymentsPage(srn, index)))
        }
        val viewModel = MemberPensionPaymentsListController
          .viewModel(
            srn,
            page,
            mode,
            memberDetails,
            userAnswers,
            viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
              getCompletedOrUpdatedTaskListStatus(
                request.userAnswers,
                request.previousUserAnswers.get,
                pages.nonsipp.memberpensionpayments.Paths.memberDetails \ "pensionAmountReceived"
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
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(
      navigator.nextPage(MemberPensionPaymentsListPage(srn), mode, request.userAnswers)
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
      onPageLoadCommon(srn, page, ViewOnlyMode, showBackLink = false)
    }
}

object MemberPensionPaymentsListController {

  private def rows(
    srn: Srn,
    mode: Mode,
    memberList: List[(Max300, NameDOB, Option[Money])],
    optYear: Option[String],
    optCurrentVersion: Option[Int],
    optPreviousVersion: Option[Int]
  ): List[List[TableElemBase]] =
    memberList
      .map {
        case (index, memberName, pensionPayment) if pensionPayment.isEmpty || pensionPayment.exists(_.isZero) =>
          List(
            TableElem(
              memberName.fullName
            ),
            TableElem(
              "memberPensionPayments.memberList.noPensionPayments"
            ),
            if (mode != ViewOnlyMode) {
              TableElem.add(
                controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
                  .onSubmit(srn, index, mode),
                Message("memberPensionPayments.memberList.add.hidden.text", memberName.fullName)
              )
            } else {
              TableElem.empty
            }
          )
        case (index, memberName, _) =>
          List(
            TableElem(
              memberName.fullName
            ),
            TableElem(
              "memberPensionPayments.memberList.pensionPaymentsReported"
            ),
            TableElemDoubleLink(
              (
                (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                  case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                    TableElem.view(
                      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
                        .onPageLoadViewOnly(
                          srn,
                          index,
                          year = year,
                          current = currentVersion,
                          previous = previousVersion
                        ),
                      Message("memberPensionPayments.memberList.remove.hidden.text", memberName.fullName)
                    )
                  case _ =>
                    TableElem.change(
                      controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
                        .onPageLoad(srn, index, CheckMode),
                      Message("memberPensionPayments.memberList.change.hidden.text", memberName.fullName)
                    )
                },
                if (mode == ViewOnlyMode) {
                  TableElem.empty
                } else {
                  TableElem.remove(
                    controllers.nonsipp.memberpensionpayments.routes.RemovePensionPaymentsController
                      .onPageLoad(srn, index),
                    Message("memberPensionPayments.memberList.remove.hidden.text", memberName.fullName)
                  )
                }
              )
            )
          )
      }
      .sortBy(_.headOption.map(_.asInstanceOf[TableElem].text.toString))

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    memberList: List[(Max300, NameDOB, Option[Money])],
    userAnswers: UserAnswers,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    schemeName: String,
    noPageEnabled: Boolean,
    showBackLink: Boolean
  ): FormPageViewModel[ActionTableViewModel] = {

    val memberListSize = memberList.size
    val (title, heading) =
      if (memberListSize == 1) {
        ("memberPensionPayments.memberList.title", "memberPensionPayments.memberList.heading")
      } else {
        ("memberPensionPayments.memberList.title.plural", "memberPensionPayments.memberList.heading.plural")
      }

    val sumPensionPayments: Int = memberList.zipWithIndex.count { case (_, index) =>
      refineV[OneTo300](index + 1).toOption
        .exists(nextIndex => userAnswers.get(TotalAmountPensionPaymentsPage(srn, nextIndex)).isDefined)
    }

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.memberPensionPayments >= memberListSize) 1 else page
    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.memberPensionPayments,
      memberListSize,
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
            .onPageLoad(srn, _, NormalMode)
      }
    )

    val optDescription =
      Option.when(mode == NormalMode)(
        ParagraphMessage(
          "memberPensionPayments.memberList.paragraphOne"
        ) ++
          ParagraphMessage(
            "memberPensionPayments.memberList.paragraphTwo"
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
            TableElem.empty
          )
        ),
        rows = rows(srn, mode, memberList, optYear, optCurrentVersion, optPreviousVersion),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "memberPensionPayments.memberList.pagination.label",
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
        controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "memberPensionPayments.memberList.viewOnly.link",
                    controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
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
            title = "memberPensionPayments.memberList.viewOnly.title",
            heading = sumPensionPayments match {
              case 0 => Message("memberPensionPayments.memberList.viewOnly.heading")
              case 1 => Message("memberPensionPayments.memberList.viewOnly.singular")
              case _ => Message("memberPensionPayments.memberList.viewOnly.plural", sumPensionPayments)
            },
            buttonText = "site.return.to.tasklist",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
                  .onSubmit(srn, page, mode)
            },
            noLabel = Option.when(noPageEnabled)(
              Message("memberPensionPayments.memberList.view.none", schemeName)
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
