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

import cats.implicits.toTraverseOps
import com.google.inject.Inject
import config.Constants
import config.Constants.maxBorrows
import config.Refined.{Max5000, OneTo5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.CheckOrChange.Change
import models.{Mode, Money, NormalMode, Pagination, Percentage}
import models.SchemeId.Srn
import models.requests.DataRequest
import pages.nonsipp.moneyborrowed.{
  BorrowInstancesListPage,
  BorrowedAmountAndRatePage,
  LenderNamePage,
  MoneyBorrowedCYAPage
}
import navigation.Navigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel, PaginatedViewModel}
import views.html.ListView
import viewmodels.implicits._

import javax.inject.Named

class BorrowInstancesListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends PSRController {

  val form = BorrowInstancesListController.form(formProvider)

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      dataBorrow(srn)
        .map(
          borrow =>
            if (borrow.isEmpty) {
              Redirect(routes.MoneyBorrowedController.onPageLoad(srn, NormalMode))
            } else {
              Ok(view(form, BorrowInstancesListController.viewModel(srn, page, mode, borrow)))
            }
        )
        .merge
  }

  def onSubmit(srn: Srn, page: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    dataBorrow(srn)(request).map { borrow =>
      val viewModel = BorrowInstancesListController.viewModel(srn, page, mode, borrow)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer => Redirect(navigator.nextPage(BorrowInstancesListPage(srn, answer), mode, request.userAnswers))
        )

    }.merge
  }

  private def dataBorrow(srn: Srn)(
    implicit request: DataRequest[AnyContent]
  ): Either[Result, List[(String, Money, Max5000)]] = {

    val index: Max5000 = request.userAnswers.map(MoneyBorrowedCYAPage(srn)).toList.traverse {
      case (key, value) =>
        key.toIntOption.flatMap(k => refineV[OneTo5000](k + 1).toOption.map(_ -> value)).get._1
    }

    val lenderName = request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney
    val borrowFinancialDetails = request.userAnswers
      .get(BorrowedAmountAndRatePage(srn, index))
      .getOrRecoverJourney
      .map(_._1)
    List((lenderName, borrowFinancialDetails, index))
  }

  object BorrowInstancesListController {
    def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
      formProvider(
        "loansList.radios.error.required"
      )

    private def rows(srn: Srn, recipients: List[(String, Money, Max5000)]): List[ListRow] =
      recipients.flatMap {
        case (recipientName, totalLoan, index) =>
          List(
            ListRow(
              Message("loansList.row", totalLoan.displayAs, recipientName),
              changeUrl =
                controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController.onPageLoad(srn, index, Change).url,
              changeHiddenText = Message("loansList.row.change.hidden", totalLoan.displayAs, recipientName),
              removeUrl = controllers.routes.UnauthorisedController.onPageLoad().url, //TODO to be changed with a BorrowRemoveController
              removeHiddenText = Message("loansList.row.remove.hidden", totalLoan.displayAs, recipientName)
            )
          )
      }

    def viewModel(
      srn: Srn,
      page: Int,
      mode: Mode,
      borrow: List[(String, Money, Max5000)]
    ): FormPageViewModel[ListViewModel] = {

      val title = if (borrow.length == 1) "loansList.title" else "loansList.title.plural"
      val heading = if (borrow.length == 1) "loansList.heading" else "loansList.heading.plural"

      val pagination = Pagination(
        currentPage = page,
        pageSize = Constants.loanPageSize,
        borrow.size,
        controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onPageLoad(srn, _, NormalMode)
      )

      FormPageViewModel(
        title = Message(title, borrow.length),
        heading = Message(heading, borrow.length),
        description = Some(ParagraphMessage("loansList.description")),
        page = ListViewModel(
          inset = "loansList.inset",
          rows(srn, borrow),
          Message("loansList.radios"),
          showRadios = borrow.length < 9999999,
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
        onSubmit = controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onSubmit(srn, page, mode)
      )
    }
  }
}
