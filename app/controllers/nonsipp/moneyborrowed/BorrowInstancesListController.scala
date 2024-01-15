/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits._
import com.google.inject.Inject
import config.Constants
import config.Constants.maxBorrows
import config.Refined.{Max5000, OneTo5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{CheckOrChange, Mode, Money, NormalMode, Pagination}
import navigation.Navigator
import pages.nonsipp.moneyborrowed.{BorrowInstancesListPage, BorrowedAmountAndRatePage, LenderNamePages}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.nonsipp.TaskListStatusUtils.getBorrowingTaskListStatusAndLink
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models._
import views.html.ListView

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
      val (borrowingStatus, incompleteBorrowingUrl) = getBorrowingTaskListStatusAndLink(request.userAnswers, srn)

      if (borrowingStatus == TaskListStatus.Completed) {
        borrowDetails(srn)
          .map(instances => Ok(view(form, BorrowInstancesListController.viewModel(srn, mode, page, instances))))
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
        val viewModel = BorrowInstancesListController.viewModel(srn, mode, page, instances)

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
    borrowingInstances: List[(Max5000, String, Money)]
  ): List[ListRow] =
    borrowingInstances.flatMap {
      case (index, lenderName, amount) =>
        List(
          ListRow(
            Message("borrowList.row.change.hidden", amount.displayAs, lenderName),
            changeUrl = controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
              .onPageLoad(srn, index, CheckOrChange.Change)
              .url,
            changeHiddenText = Message("borrowList.row.change.hidden", amount.displayAs),
            controllers.nonsipp.moneyborrowed.routes.RemoveBorrowInstancesController
              .onPageLoad(srn, index, mode)
              .url,
            Message("borrowList.row.remove.hiddenText")
          )
        )
    }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    borrowingInstances: List[(Max5000, String, Money)]
  ): FormPageViewModel[ListViewModel] = {

    val title = if (borrowingInstances.size == 1) "borrowList.title" else "borrowList.title.plural"
    val heading = if (borrowingInstances.size == 1) "borrowList.heading" else "borrowList.heading.plural"

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.borrowPageSize,
      borrowingInstances.size,
      routes.BorrowInstancesListController.onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, borrowingInstances.size),
      heading = Message(heading, borrowingInstances.size),
      description = Some(ParagraphMessage("borrowList.description")),
      page = ListViewModel(
        inset = "borrowList.inset",
        rows(srn, mode, borrowingInstances),
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
      onSubmit = routes.BorrowInstancesListController.onSubmit(srn, page, mode)
    )
  }
}
