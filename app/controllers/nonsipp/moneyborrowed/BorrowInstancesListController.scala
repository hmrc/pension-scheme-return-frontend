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

import java.time.LocalDateTime
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
    val (borrowingStatus, incompleteBorrowingUrl) = getBorrowingTaskListStatusAndLink(request.userAnswers, srn)

    if (borrowingStatus == TaskListStatus.Completed) {
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
                  viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                    getCompletedOrUpdatedTaskListStatus(
                      request.userAnswers,
                      request.previousUserAnswers.get,
                      pages.nonsipp.moneyborrowed.Paths.borrowing
                    ) == Updated
                  } else {
                    false
                  },
                  optYear = request.year,
                  optCurrentVersion = request.currentVersion,
                  optPreviousVersion = request.previousVersion,
                  compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
                )
              )
            )
        )
        .merge
    } else if (borrowingStatus == TaskListStatus.InProgress) {
      Redirect(incompleteBorrowingUrl)
    } else {
      Redirect(controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, mode))
    }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    borrowDetails(srn).map { instances =>
      if (instances.length == maxBorrows) {
        Redirect(navigator.nextPage(BorrowInstancesListPage(srn, addBorrow = false), mode, request.userAnswers))
      } else {
        val viewModel =
          BorrowInstancesListController.viewModel(srn, mode, page, instances, false, None, None, None)

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

  def onPreviousViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
            .onPageLoadViewOnly(srn, page, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
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
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None
  ): List[ListRow] =
    borrowingInstances.flatMap {
      case (index, lenderName, amount) =>
        if (mode.isViewOnlyMode) {
          (mode, optYear, optCurrentVersion, optPreviousVersion) match {
            case (ViewOnlyMode, Some(year), Some(current), Some(previous)) =>
              List(
                ListRow.view(
                  lenderName,
                  controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
                    .onPageLoadViewOnly(srn, index, year, current, previous)
                    .url,
                  Message("borrowList.row.change.hidden", amount.displayAs, lenderName)
                )
              )
            case _ => Nil
          }
        } else {
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

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    borrowingInstances: List[(Max5000, String, Money)],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[ListViewModel] = {

    val lengthOfBorrowingInstances = borrowingInstances.length

    val (title, heading) = ((mode, lengthOfBorrowingInstances) match {
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
      call = (mode, optYear, optCurrentVersion, optPreviousVersion) match {
        case (ViewOnlyMode, Some(year), Some(currentVersion), Some(previousVersion)) =>
          routes.BorrowInstancesListController.onPageLoadViewOnly(srn, _, year, currentVersion, previousVersion)
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
        rows(srn, mode, borrowingInstances, optYear, optCurrentVersion, optPreviousVersion),
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
      optViewOnlyDetails = Option.when(mode.isViewOnlyMode) {
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if (optYear.nonEmpty && currentVersion > 1 && previousVersion > 0) =>
              Some(
                LinkMessage(
                  "borrowList.view.link",
                  controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
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
              controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController
                .onSubmit(srn, page, mode)
          }
        )
      }
    )
  }
}
