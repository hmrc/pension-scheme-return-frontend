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

package controllers.nonsipp.loansmadeoroutstanding

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import utils.ListUtils.ListOps
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import cats.implicits.{toShow, toTraverseOps, _}
import _root_.config.Constants
import _root_.config.Constants.maxLoans
import forms.YesNoPageFormProvider
import pages.nonsipp.loansmadeoroutstanding._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.ListView
import models.SchemeId.Srn
import controllers.nonsipp.loansmadeoroutstanding.LoansListController._
import controllers.actions._
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage, _}
import utils.nonsipp.check.LoansCheckStatusUtils
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import utils.MapUtils.UserAnswersMapOps
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class LoansListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext)
    extends PSRController {

  private implicit val logger: Logger = Logger(getClass)

  val form: Form[Boolean] = LoansListController.form(formProvider)

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
    val viewOnlyViewModel = ViewOnlyViewModel(
      viewOnlyUpdated = request.previousUserAnswers match {
        case Some(previousUserAnswers) =>
          getCompletedOrUpdatedTaskListStatus(
            request.userAnswers,
            previousUserAnswers,
            pages.nonsipp.loansmadeoroutstanding.Paths.loans
          ) == Updated
        case _ => false
      },
      year = year,
      currentVersion = current,
      previousVersion = previous,
      compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
    )

    onPageLoadCommon(srn, page, mode, Some(viewOnlyViewModel), showBackLink)
  }

  def onPageLoadCommon(
    srn: Srn,
    page: Int,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  )(
    implicit request: DataRequest[AnyContent]
  ): Result = {
    val indexes: List[Max5000] =
      request.userAnswers.map(LoanCompleted.all(srn)).keys.toList.refine[Max5000.Refined]

    if (indexes.nonEmpty || mode.isViewOnlyMode) {
      loansToTraverse(srn).map {
        case (loansToCheck, loans) =>
          Ok(
            view(
              form,
              viewModel(
                srn,
                page,
                mode,
                loans,
                loansToCheck,
                request.schemeDetails.schemeName,
                viewOnlyViewModel,
                showBackLink = showBackLink,
                isPrePopulation
              )
            )
          )
      }.merge
    } else {
      Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      loansToTraverse(srn).traverse {
        case (loansToCheck, loans) =>
          if (loansToCheck.size + loans.size >= Constants.maxLoans) {
            Future.successful(
              Redirect(
                navigator.nextPage(LoansListPage(srn, addLoan = false), mode, request.userAnswers)
              )
            )
          } else {
            form
              .bindFromRequest()
              .fold(
                errors => {
                  Future.successful(
                    BadRequest(
                      view(
                        errors,
                        viewModel(
                          srn,
                          page,
                          mode,
                          loans,
                          loansToCheck,
                          request.schemeDetails.schemeName,
                          None,
                          showBackLink = true,
                          isPrePopulation
                        )
                      )
                    )
                  )
                },
                answer =>
                  Future.successful(Redirect(navigator.nextPage(LoansListPage(srn, answer), mode, request.userAnswers)))
              )
          }
      }.merge
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
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              pages.nonsipp.loansmadeoroutstanding.Paths.loans
            ) == Updated
          case _ => false
        },
        year = year,
        currentVersion = (current - 1).max(0),
        previousVersion = (previous - 1).max(0),
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )

      onPageLoadCommon(srn, page, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
    }

  private def loansToTraverse(srn: Srn)(
    implicit request: DataRequest[_],
    logger: Logger
  ): Either[Result, (List[LoansData], List[LoansData])] = {
    // if return has been pre-populated, partition shares by those that need to be checked
    def buildLoans(index: Max5000): Either[Result, LoansData] =
      for {
        loanAmountDetails <- requiredPage(AmountOfTheLoanPage(srn, index))
        loanRecipientType <- requiredPage(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient))
        recipientName <- loanRecipientType match {
          case IdentityType.Individual =>
            requiredPage(IndividualRecipientNamePage(srn, index))
          case IdentityType.UKCompany =>
            requiredPage(CompanyRecipientNamePage(srn, index))
          case IdentityType.UKPartnership =>
            requiredPage(PartnershipRecipientNamePage(srn, index))
          case IdentityType.Other =>
            requiredPage(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient)).map(_.name)
        }
      } yield LoansData(index, loanAmountDetails.loanAmount, recipientName)

    if (isPrePopulation) {
      for {
        indexes <- request.userAnswers
          .map(LoanIdentityTypePages(srn))
          .refine[Max5000.Refined]
          .map(_.keys.toList)
          .getOrRecoverJourney
        loans <- indexes.traverse(buildLoans)
      } yield loans.partition(
        loans => LoansCheckStatusUtils.checkLoansRecord(request.userAnswers, srn, loans.index)
      )
    } else {
      val noLoansToCheck = List.empty[LoansData]
      for {
        indexes <- request.userAnswers
          .map(LoanIdentityTypePages(srn))
          .refine[Max5000.Refined]
          .map(_.keys.toList)
          .getOrRecoverJourney
        loans <- indexes.traverse(buildLoans)
      } yield (noLoansToCheck, loans)
    }
  }
}

object LoansListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "loansList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    loansList: List[LoansData],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String,
    check: Boolean = false
  ): List[ListRow] =
    (loansList, mode) match {
      case (Nil, mode) if mode.isViewOnlyMode =>
        List(
          ListRow.viewNoLink(
            text = Message("loansList.viewOnly.none", schemeName),
            value = "loansList.viewOnly.none.value"
          )
        )
      case (Nil, mode) if !mode.isViewOnlyMode =>
        List()
      case (list, _) =>
        list.map {
          case LoansData(index, loanAmount, recipientName) =>
            (mode, viewOnlyViewModel) match {
              case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
                ListRow.view(
                  text = Message("loansList.row", loanAmount.displayAs, recipientName),
                  url = routes.LoansCYAController.onPageLoadViewOnly(srn, index, year, current, previous).url,
                  hiddenText = Message("loansList.row.view.hidden", loanAmount.displayAs, recipientName)
                )
              case _ if check =>
                ListRow.check(
                  text = Message("loansList.row", loanAmount.displayAs, recipientName),
                  url = routes.LoansCheckAndUpdateController.onPageLoad(srn, index).url,
                  hiddenText =
                    Message("site.check.param", Message("loansList.row", loanAmount.displayAs, recipientName))
                )
              case _ =>
                ListRow(
                  text = Message("loansList.row", loanAmount.displayAs, recipientName),
                  changeUrl = routes.LoansCYAController.onPageLoad(srn, index, CheckMode).url,
                  changeHiddenText = Message("loansList.row.change.hidden", loanAmount.displayAs, recipientName),
                  removeUrl = routes.RemoveLoanController.onPageLoad(srn, index, mode).url,
                  removeHiddenText = Message("loansList.row.remove.hidden", loanAmount.displayAs, recipientName)
                )
            }
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    loans: List[LoansData],
    loansToCheck: List[LoansData],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean,
    isPrePop: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val loansSize = if (isPrePop) loans.length + loansToCheck.length else loans.length

    val (title, heading) = (mode match {
      // View Only
      case ViewOnlyMode if loansSize == 0 =>
        ("loansList.viewOnly.title.none", "loansList.viewOnly.heading.none")
      case ViewOnlyMode if loansSize > 1 =>
        ("loansList.viewOnly.title.plural", "loansList.viewOnly.heading.plural")
      case ViewOnlyMode =>
        ("loansList.viewOnly.title", "loansList.viewOnly.heading")
      // PrePop
      case _ if isPrePop && loans.nonEmpty =>
        ("loansList.title.prepop.check", "loansList.heading.prepop.check")
      case _ if isPrePop && loansSize > 1 =>
        ("loansList.title.prepop.plural", "loansList.heading.prepop.plural")
      case _ if isPrePop =>
        ("loansList.title.prepop", "loansList.heading.prepop")
      // Normal
      case _ if loansSize > 1 =>
        ("loansList.title.plural", "loansList.heading.plural")
      case _ =>
        ("loansList.title", "loansList.heading")
    }) match {
      case (title, heading) =>
        (Message(title, loansSize), Message(heading, loansSize))
    }

    // in view-only mode or with direct url edit page value can be higher than needed
    val currentPage = if ((page - 1) * Constants.pageSize >= loansSize) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.loanPageSize,
      totalSize = loansSize,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.LoansListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.LoansListController.onPageLoad(srn, _, NormalMode)
      }
    )

    val paragraph = Option.when(loansSize < maxLoans) {
      if (loansToCheck.nonEmpty) {
        ParagraphMessage("loansList.description.prepop", "loansList.description")
      } else {
        ParagraphMessage("loansList.description")
      }
    }

    val conditionalInsetText: DisplayMessage = {
      if (loansSize >= Constants.maxLoans) {
        ParagraphMessage("loansList.inset") ++
          ParagraphMessage("")
      } else {
        Message("")
      }
    }

    val sections = {
      if (isPrePop) {
        Option
          .when(loansToCheck.nonEmpty)(
            ListSection(
              heading = Some("loansList.section.check"),
              rows(srn, mode, loansToCheck, viewOnlyViewModel, schemeName, check = true)
            )
          )
          .toList ++
          Option
            .when(loans.nonEmpty)(
              ListSection(
                heading = Some("loansList.section.added"),
                rows(srn, mode, loans, viewOnlyViewModel, schemeName)
              )
            )
            .toList
      } else {
        List(
          ListSection(rows(srn, mode, loans, viewOnlyViewModel, schemeName))
        )
      }
    }

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = paragraph,
      page = ListViewModel(
        inset = conditionalInsetText,
        sections = sections,
        radioText = Message("loansList.radios"),
        showRadios = loansSize < Constants.maxLoans,
        showInsetWithRadios = !(loansSize < Constants.maxLoans),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "loansList.pagination.label",
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
      onSubmit = controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "loansList.viewOnly.link",
                routes.LoansListController
                  .onPreviousViewOnly(
                    srn,
                    page,
                    viewOnly.year,
                    viewOnly.currentVersion,
                    viewOnly.previousVersion
                  )
                  .url
              )
            )
          } else {
            None
          },
          submittedText = viewOnly.compilationOrSubmissionDate
            .fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
          title = title,
          heading = heading,
          buttonText = "site.return.to.tasklist",
          onSubmit = controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }

  case class LoansData(
    index: Max5000,
    loanAmount: Money,
    recipientName: String
  )
}
