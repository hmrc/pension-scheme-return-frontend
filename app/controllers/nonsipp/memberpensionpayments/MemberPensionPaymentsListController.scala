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

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.OneTo300
import controllers.PSRController
import cats.implicits.{toBifunctorOps, toShow, toTraverseOps}
import config.Constants.maxNotRelevant
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import config.Constants
import views.html.TwoColumnsTripleAction
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments.{
  MemberPensionPaymentsListPage,
  PensionPaymentsReceivedPage,
  TotalAmountPensionPaymentsPage
}
import controllers.actions._
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

class MemberPensionPaymentsListController @Inject()(
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

  private val form = MemberPensionPaymentsListController.form(formProvider)

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
      val noPageEnabled = !userAnswers.get(PensionPaymentsReceivedPage(srn)).getOrElse(false)
      val viewModel = MemberPensionPaymentsListController
        .viewModel(
          srn,
          page,
          mode,
          optionList,
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
      val filledForm =
        request.userAnswers.get(MemberPensionPaymentsListPage(srn)).fold(form)(form.fill)
      Ok(view(filledForm, viewModel))
    } else {
      Redirect(
        controllers.routes.JourneyRecoveryController.onPageLoad()
      )
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val userAnswers = request.userAnswers
      val optionList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

      if (optionList.flatten.size > Constants.maxSchemeMembers) {
        Future.successful(
          Redirect(
            navigator.nextPage(MemberPensionPaymentsListPage(srn), mode, request.userAnswers)
          )
        )
      } else {

        form
          .bindFromRequest()
          .fold(
            errors => {
              val noPageEnabled = !userAnswers.get(PensionPaymentsReceivedPage(srn)).getOrElse(false)
              Future.successful(
                BadRequest(
                  view(
                    errors,
                    MemberPensionPaymentsListController
                      .viewModel(
                        srn,
                        page,
                        mode,
                        optionList,
                        userAnswers,
                        viewOnlyUpdated = false,
                        None,
                        None,
                        None,
                        None,
                        request.schemeDetails.schemeName,
                        noPageEnabled,
                        showBackLink = true
                      )
                  )
                )
              )
            },
            value =>
              for {
                updatedUserAnswers <- buildUserAnswerBySelection(srn, value, optionList.flatten.size)
                _ <- saveService.save(updatedUserAnswers)
                submissionResult <- if (value) {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedUserAnswers,
                    fallbackCall = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
                      .onPageLoad(srn, page, mode)
                  )
                } else {
                  Future.successful(Some(()))
                }
              } yield submissionResult.getOrRecoverJourney(
                _ =>
                  Redirect(
                    navigator
                      .nextPage(MemberPensionPaymentsListPage(srn), mode, request.userAnswers)
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

  private def buildUserAnswerBySelection(srn: Srn, selection: Boolean, memberListSize: Int)(
    implicit request: DataRequest[_]
  ): Future[UserAnswers] = {
    val userAnswerWithMemberContList = request.userAnswers.set(MemberPensionPaymentsListPage(srn), selection)

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
                val optTotalAmountPensionPayments = request.userAnswers.get(TotalAmountPensionPaymentsPage(srn, index))
                for {
                  ua <- uaTry
                  ua1 <- ua
                    .set(TotalAmountPensionPaymentsPage(srn, index), optTotalAmountPensionPayments.getOrElse(Money(0)))
                } yield ua1
            }
        )
      )
    } else {
      Future.fromTry(userAnswerWithMemberContList)
    }
  }

}

object MemberPensionPaymentsListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "memberPensionPayments.memberList.radios.error.required"
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
              val pensionPayments = userAnswers.get(TotalAmountPensionPaymentsPage(srn, nextIndex))
              if (pensionPayments.nonEmpty && !pensionPayments.exists(_.isZero)) {
                List(
                  TableElem(
                    memberName.fullName
                  ),
                  TableElem(
                    "memberPensionPayments.memberList.pensionPaymentsReported"
                  ),
                  (mode, optYear, optCurrentVersion, optPreviousVersion) match {
                    case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
                      TableElem.view(
                        controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
                          .onPageLoadViewOnly(
                            srn,
                            nextIndex,
                            year = year,
                            current = currentVersion,
                            previous = previousVersion
                          ),
                        Message("memberPensionPayments.memberList.remove.hidden.text", memberName.fullName)
                      )
                    case _ =>
                      TableElem.change(
                        controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
                          .onPageLoad(srn, nextIndex, CheckMode),
                        Message("memberPensionPayments.memberList.change.hidden.text", memberName.fullName)
                      )
                  },
                  if (mode == ViewOnlyMode) {
                    TableElem.empty
                  } else {
                    TableElem.remove(
                      controllers.nonsipp.memberpensionpayments.routes.RemovePensionPaymentsController
                        .onPageLoad(srn, nextIndex),
                      Message("memberPensionPayments.memberList.remove.hidden.text", memberName.fullName)
                    )
                  }
                )
              } else {
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
                        .onSubmit(srn, nextIndex, mode),
                      Message("memberPensionPayments.memberList.add.hidden.text", memberName.fullName)
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
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    schemeName: String,
    noPageEnabled: Boolean,
    showBackLink: Boolean
  ): FormPageViewModel[ActionTableViewModel] = {

    val memberListSize = memberList.flatten.size
    val (title, heading) =
      if (memberListSize == 1) {
        ("memberPensionPayments.memberList.title", "memberPensionPayments.memberList.heading")
      } else {
        ("memberPensionPayments.memberList.title.plural", "memberPensionPayments.memberList.heading.plural")
      }

    val sumPensionPayments: Int = memberList.flatten.zipWithIndex.count {
      case (_, index) =>
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

    FormPageViewModel(
      mode = mode,
      title = Message(title, memberListSize),
      heading = Message(heading, memberListSize),
      description = Some(
        ParagraphMessage(
          "memberPensionPayments.memberList.paragraphOne"
        ) ++
          ParagraphMessage(
            "memberPensionPayments.memberList.paragraphTwo"
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
        radioText = Message("memberPensionPayments.memberList.radios"),
        showRadios = memberList.length < maxNotRelevant,
        showInsetWithRadios = true,
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
