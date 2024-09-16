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
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.{getCompletedOrUpdatedTaskListStatus, getLoansTaskListStatusAndLink}
import cats.implicits._
import _root_.config.Constants
import _root_.config.Constants.maxLoans
import forms.YesNoPageFormProvider
import pages.nonsipp.loansmadeoroutstanding._
import play.api.i18n.MessagesApi
import eu.timepit.refined.api.Refined
import _root_.config.Refined.{Max5000, OneTo5000}
import views.html.ListView
import models.SchemeId.Srn
import controllers.nonsipp.loansmadeoroutstanding.LoansListController._
import controllers.actions._
import eu.timepit.refined.refineV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import viewmodels.models.TaskListStatus.Updated
import pages.nonsipp.common.{IdentityTypes, OtherRecipientDetailsPage}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.Future

import javax.inject.Named

class LoansListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

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
    previous: Int,
    showBackLink: Boolean
  ): Action[AnyContent] = identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
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
  ): Result =
    loanRecipients(srn).map { recipients =>
      if (viewOnlyViewModel.isEmpty && recipients.isEmpty) {
        Redirect(routes.LoansMadeOrOutstandingController.onPageLoad(srn, NormalMode))
      } else {
        val (loansStatus, incompleteLoansUrl) = getLoansTaskListStatusAndLink(request.userAnswers, srn)
        if (loansStatus == TaskListStatus.NotStarted) {
          Redirect(routes.LoansMadeOrOutstandingController.onPageLoad(srn, NormalMode))
        } else if (loansStatus == TaskListStatus.InProgress) {
          Redirect(incompleteLoansUrl)
        } else {
          loanRecipients(srn)
            .map(
              recipients =>
                Ok(
                  view(
                    form,
                    viewModel(
                      srn,
                      page,
                      mode,
                      recipients,
                      request.schemeDetails.schemeName,
                      viewOnlyViewModel,
                      showBackLink = showBackLink
                    )
                  )
                )
            )
            .merge
        }
      }
    }.merge

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    loanRecipients(srn).map { recipients =>
      if (recipients.length == maxLoans) {
        Redirect(navigator.nextPage(LoansListPage(srn, addLoan = false), mode, request.userAnswers))
      } else {

        val viewModel =
          LoansListController.viewModel(srn, page, mode, recipients, "", showBackLink = true)

        form
          .bindFromRequest()
          .fold(
            errors => BadRequest(view(errors, viewModel)),
            answer => Redirect(navigator.nextPage(LoansListPage(srn, answer), mode, request.userAnswers))
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

  def onPreviousViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          routes.LoansListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0), showBackLink = false)
        )
      )
    }

  private def loanRecipients(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, List[(Refined[Int, OneTo5000], String, Money)]] = {
    val whoReceivedLoans = request.userAnswers
      .map(IdentityTypes(srn, IdentitySubject.LoanRecipient))
      .map {
        case (key, value) =>
          key.toIntOption.flatMap(k => refineV[OneTo5000](k + 1).toOption.map(_ -> value))
      }
      .toList
      .sortBy(listRow => listRow.map(list => list._1.value))

    for {
      receivedLoans <- whoReceivedLoans.traverse(_.getOrRecoverJourney)
      recipientNames <- receivedLoans.traverse {
        case (index, IdentityType.Individual) =>
          request.userAnswers.get(IndividualRecipientNamePage(srn, index)).getOrRecoverJourney.map(index -> _)
        case (index, IdentityType.UKCompany) =>
          request.userAnswers.get(CompanyRecipientNamePage(srn, index)).getOrRecoverJourney.map(index -> _)
        case (index, IdentityType.UKPartnership) =>
          request.userAnswers.get(PartnershipRecipientNamePage(srn, index)).getOrRecoverJourney.map(index -> _)
        case (index, IdentityType.Other) =>
          request.userAnswers
            .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient))
            .map(_.name)
            .getOrRecoverJourney
            .map(index -> _)
      }
      recipientDetails <- recipientNames.traverse {
        case (index, recipientName) =>
          request.userAnswers
            .get(AmountOfTheLoanPage(srn, index))
            .map(_._1)
            .getOrRecoverJourney
            .map((index, recipientName, _))
      }
    } yield recipientDetails
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
    recipients: List[(Max5000, String, Money)],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    (recipients, mode) match {
      case (Nil, mode) if mode.isViewOnlyMode =>
        List(
          ListRow.viewNoLink(
            Message("loansList.viewOnly.none", schemeName),
            "loansList.viewOnly.none.value"
          )
        )
      case (Nil, mode) if !mode.isViewOnlyMode =>
        List()
      case (list, _) =>
        list.flatMap {
          case (index, recipientName, totalLoan) =>
            if (mode.isViewOnlyMode) {
              (mode, viewOnlyViewModel) match {
                case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
                  List(
                    ListRow.view(
                      Message("loansList.row", totalLoan.displayAs, recipientName),
                      routes.LoansCYAController.onPageLoadViewOnly(srn, index, year, current, previous).url,
                      Message("loansList.row.change.hidden", totalLoan.displayAs, recipientName)
                    )
                  )
                case _ => Nil
              }
            } else {
              List(
                ListRow(
                  Message("loansList.row", totalLoan.displayAs, recipientName),
                  changeUrl = routes.LoansCYAController.onPageLoad(srn, index, CheckMode).url,
                  changeHiddenText = Message("loansList.row.change.hidden", totalLoan.displayAs, recipientName),
                  removeUrl = routes.RemoveLoanController.onPageLoad(srn, index, mode).url,
                  removeHiddenText = Message("loansList.row.remove.hidden", totalLoan.displayAs, recipientName)
                )
              )
            }
        }
    }

  def viewModel(
    srn: Srn,
    page: Int,
    mode: Mode,
    recipients: List[(Max5000, String, Money)],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val (title, heading) = ((mode, recipients.length) match {
      case (ViewOnlyMode, numberOfLoans) if numberOfLoans == 0 =>
        ("loansList.viewOnly.title.none", "loansList.viewOnly.heading.none")
      case (ViewOnlyMode, numberOfLoans) if numberOfLoans > 1 =>
        ("loansList.viewOnly.title.plural", "loansList.viewOnly.heading.plural")
      case (ViewOnlyMode, _) =>
        ("loansList.viewOnly.title", "loansList.viewOnly.heading")
      case (_, numberOfLoans) if numberOfLoans > 1 =>
        ("loansList.title.plural", "loansList.heading.plural")
      case _ =>
        ("loansList.title", "loansList.heading")
    }) match {
      case (title, heading) => (Message(title, recipients.size), Message(heading, recipients.length))
    }

    val description = if (recipients.length < maxLoans) Some(ParagraphMessage("loansList.description")) else None

    val currentPage = if ((page - 1) * Constants.loanPageSize >= recipients.size) 1 else page

    val pagination = Pagination(
      currentPage = currentPage,
      pageSize = Constants.loanPageSize,
      recipients.size,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.LoansListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion, showBackLink = true)
        case _ =>
          routes.LoansListController.onPageLoad(srn, _, NormalMode)
      }
    )

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = description,
      page = ListViewModel(
        inset = "loansList.inset",
        rows(srn, mode, recipients, viewOnlyViewModel, schemeName),
        Message("loansList.radios"),
        showRadios = recipients.length < Constants.maxLoans,
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
      onSubmit = routes.LoansListController
        .onSubmit(srn, page, mode),
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
          onSubmit = routes.LoansListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }
}
