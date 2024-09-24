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

package controllers.nonsipp.moneyborrowed

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.{Max5000, OneTo5000}
import controllers.PSRController
import cats.implicits._
import config.Constants.maxBorrows
import forms.YesNoPageFormProvider
import viewmodels.models.TaskListStatus.Updated
import eu.timepit.refined.api.Refined
import utils.nonsipp.TaskListStatusUtils.{getBorrowingTaskListStatusAndLink, getCompletedOrUpdatedTaskListStatus}
import config.Constants
import views.html.ListView
import models.SchemeId.Srn
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.moneyborrowed.{BorrowInstancesListPage, BorrowedAmountAndRatePage, LenderNamePages}
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.Future

import javax.inject.Named

class BorrowInstancesListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController
    with I18nSupport {

  val form: Form[Boolean] = BorrowInstancesListController.form(formProvider)

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
            pages.nonsipp.moneyborrowed.Paths.borrowing
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
    borrowDetails(srn).map { instances =>
      if (viewOnlyViewModel.isEmpty && instances.isEmpty) {
        Redirect(controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, mode))
      } else {

        val (borrowingStatus, incompleteBorrowingUrl) = getBorrowingTaskListStatusAndLink(request.userAnswers, srn)

        if (borrowingStatus == TaskListStatus.NotStarted) {
          Redirect(controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, mode))
        } else if (borrowingStatus == TaskListStatus.InProgress) {
          Redirect(incompleteBorrowingUrl)
        } else {
          borrowDetails(srn)
            .map(
              instances =>
                Ok(
                  view(
                    form,
                    BorrowInstancesListController.viewModel(
                      srn,
                      mode,
                      page,
                      instances,
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
    borrowDetails(srn).map { instances =>
      if (instances.length == maxBorrows) {
        Redirect(navigator.nextPage(BorrowInstancesListPage(srn, addBorrow = false), mode, request.userAnswers))
      } else {
        val viewModel =
          BorrowInstancesListController.viewModel(srn, mode, page, instances, "", showBackLink = true)

        form
          .bindFromRequest()
          .fold(
            errors => BadRequest(view(errors, viewModel)),
            answer =>
              Redirect(
                navigator.nextPage(BorrowInstancesListPage(srn, answer), mode, request.userAnswers)
              )
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
  ): Action[AnyContent] = identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) {
    implicit request =>
      val showBackLink = false

      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              pages.nonsipp.moneyborrowed.Paths.borrowing
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

  private def borrowDetails(
    srn: Srn
  )(implicit request: DataRequest[_]): Either[Result, List[(Refined[Int, OneTo5000], String, Money)]] = {
    val fromLenderPages = request.userAnswers
      .map(LenderNamePages(srn))
      .map {
        case (key, value) =>
          key.toIntOption.flatMap(k => refineV[OneTo5000](k + 1).toOption.map(_ -> value))
      }
      .toList
      .sortBy(listRow => listRow.map(list => list._1.value))

    for {
      lendersNames <- fromLenderPages.traverse(_.getOrRecoverJourney)

      borrowingInstanceDetails <- lendersNames.traverse {
        case (index, lenderName) =>
          request.userAnswers
            .get(BorrowedAmountAndRatePage(srn, index))
            .map(_._1)
            .getOrRecoverJourney
            .map((index, lenderName, _))
      }
    } yield borrowingInstanceDetails

  }
}

object BorrowInstancesListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "borrowList.radios.error.required"
    )

  private def rows(
    srn: Srn,
    mode: Mode,
    borrowingInstances: List[(Max5000, String, Money)],
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    schemeName: String
  ): List[ListRow] =
    (borrowingInstances, mode) match {
      case (Nil, mode) if mode.isViewOnlyMode =>
        List(
          ListRow.viewNoLink(
            Message("borrowList.view.none", schemeName),
            "borrowList.view.none.value"
          )
        )
      case (Nil, mode) if !mode.isViewOnlyMode =>
        List()
      case (list, _) =>
        list.flatMap {
          case (index, lenderName, amount) =>
            (mode, viewOnlyViewModel) match {
              case (ViewOnlyMode, Some(ViewOnlyViewModel(_, year, current, previous, _))) =>
                List(
                  ListRow.view(
                    lenderName,
                    controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
                      .onPageLoadViewOnly(srn, index, year, current, previous)
                      .url,
                    Message("borrowList.row.view.hidden", amount.displayAs, lenderName)
                  )
                )
              case _ =>
                List(
                  ListRow(
                    Message("borrowList.row.change.hidden", amount.displayAs, lenderName),
                    changeUrl = controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
                      .onPageLoad(srn, index, CheckMode)
                      .url,
                    changeHiddenText = Message("borrowList.row.change.hidden", amount.displayAs, lenderName),
                    controllers.nonsipp.moneyborrowed.routes.RemoveBorrowInstancesController
                      .onPageLoad(srn, index, mode)
                      .url,
                    Message("borrowList.row.remove.hidden", amount.displayAs, lenderName)
                  )
                )
            }
        }
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    borrowingInstances: List[(Max5000, String, Money)],
    schemeName: String,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  ): FormPageViewModel[ListViewModel] = {

    val lengthOfBorrowingInstances = borrowingInstances.length

    val (title, heading) = ((mode, lengthOfBorrowingInstances) match {
      case (ViewOnlyMode, lengthOfBorrowingInstances) if lengthOfBorrowingInstances == 0 =>
        ("borrowList.view.title.none", "borrowList.view.heading.none")
      case (ViewOnlyMode, lengthOfBorrowingInstances) if lengthOfBorrowingInstances > 1 =>
        ("borrowList.view.title.plural", "borrowList.view.heading.plural")
      case (ViewOnlyMode, _) =>
        ("borrowList.view.title", "borrowList.view.heading")
      case (_, lengthOfBorrowingInstances) if lengthOfBorrowingInstances > 1 =>
        ("borrowList.title.plural", "borrowList.heading.plural")
      case _ =>
        ("borrowList.title", "borrowList.heading")
    }) match {
      case (title, heading) =>
        (Message(title, lengthOfBorrowingInstances), Message(heading, lengthOfBorrowingInstances))
    }

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.borrowPageSize,
      borrowingInstances.size,
      call = viewOnlyViewModel match {
        case Some(ViewOnlyViewModel(_, year, currentVersion, previousVersion, _)) =>
          routes.BorrowInstancesListController
            .onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
        case _ =>
          routes.BorrowInstancesListController.onPageLoad(srn, _, NormalMode)
      }
    )

    FormPageViewModel(
      mode = mode,
      title = title,
      heading = heading,
      description = Some(ParagraphMessage("borrowList.description")),
      page = ListViewModel(
        inset = "borrowList.inset",
        rows(srn, mode, borrowingInstances, viewOnlyViewModel, schemeName),
        Message("borrowList.radios"),
        showRadios = borrowingInstances.size < Constants.maxBorrows,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "borrowList.pagination.label",
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
      onSubmit = routes.BorrowInstancesListController.onSubmit(srn, page, mode),
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "borrowList.view.link",
                controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
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
          onSubmit = controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }
}
