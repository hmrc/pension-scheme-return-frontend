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

import com.google.inject.Inject
import config.Constants
import config.Constants.maxBorrows
import config.Refined.Max5000
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.{CheckOrChange, Mode, Money, NormalMode, Pagination, Percentage}
import models.SchemeId.Srn
import pages.nonsipp.moneyborrowed.{BorrowInstancesListPage, BorrowedAmountAndRatePages, LenderNamePages}
import navigation.Navigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel, PaginatedViewModel}
import views.html.ListView
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._

import javax.inject.Named

class BorrowInstancesListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends FrontendBaseController
    with I18nSupport {

  val form = BorrowInstancesListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val borrows = request.userAnswers.map(BorrowedAmountAndRatePages(srn))
      val lenderName = request.userAnswers.map(LenderNamePages(srn))

      if (borrows.nonEmpty) {
        val viewModel = BorrowInstancesListController.viewModel(srn, mode, page, borrows, lenderName)
        Ok(view(form, viewModel))
      } else {
        Redirect(controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, mode))
      }
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val borrows = request.userAnswers.map(BorrowedAmountAndRatePages(srn))
    val lenderName = request.userAnswers.map(LenderNamePages(srn))

    if (borrows.size == maxBorrows) {
      Redirect(navigator.nextPage(BorrowInstancesListPage(srn, addBorrow = false), mode, request.userAnswers))
    } else {
      val viewModel = BorrowInstancesListController.viewModel(srn, mode, page, borrows, lenderName)

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
    borrow: Map[String, (Money, Percentage)],
    lenderName: Map[String, String]
  ): List[ListRow] = {
    val myLenderName = lenderName.map {
      case (index, name) =>
        refineV[Max5000.Refined](index.toInt + 1).fold(_ => Nil, index => {
          name
        })
    }.toList
    borrow.flatMap {
      case (index, amount) =>
        refineV[Max5000.Refined](index.toInt + 1).fold(
          _ => Nil,
          index =>
            List(
              ListRow(
                Message("borrowList.row.change.hidden", amount._1.displayAs, myLenderName(index.value - 1).toString),
                changeUrl = controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
                  .onPageLoad(srn, index, CheckOrChange.Check)
                  .url,
                changeHiddenText = Message("borrowList.row.change.hidden", amount._1.displayAs),
                controllers.routes.UnauthorisedController.onPageLoad().url, //TODO change with remove controller
                Message("borrowList.row.remove.hiddenText")
              )
            )
        )
    }.toList

  }

  def viewModel(
    srn: Srn,
    mode: Mode,
    page: Int,
    borrows: Map[String, (Money, Percentage)],
    lenderName: Map[String, String]
  ): FormPageViewModel[ListViewModel] = {

    val title = if (borrows.size == 1) "borrowList.title" else "borrowList.title.plural"
    val heading = if (borrows.size == 1) "borrowList.heading" else "borrowList.heading.plural"

    borrows.map {
      case (index, _) =>
        refineV[Max5000.Refined](index.toInt + 1).fold(_ => Nil, index => (index.value - 1).toString)
    }
    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.borrowPageSize,
      borrows.size,
      routes.BorrowInstancesListController.onPageLoad(srn, _, NormalMode)
    )

    FormPageViewModel(
      title = Message(title, borrows.size),
      heading = Message(heading, borrows.size),
      description = Some(ParagraphMessage("borrowList.description")),
      page = ListViewModel(
        inset = "borrowList.inset",
        rows(srn, mode, borrows, lenderName),
        Message("borrowList.radios"),
        showRadios = borrows.size < Constants.maxBorrows,
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
      onSubmit = routes.BorrowInstancesListController.onSubmit(srn, page, mode)
    )
  }
}
