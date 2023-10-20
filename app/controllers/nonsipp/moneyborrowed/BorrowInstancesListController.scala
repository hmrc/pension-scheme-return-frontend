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
import config.Constants.maxBorrows
import config.Refined.Max5000
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.{CheckOrChange, Mode, Money, Percentage}
import models.SchemeId.Srn
import pages.nonsipp.moneyborrowed.{BorrowInstancesListPage, BorrowedAmountAndRatePages, MoneyBorrowedCYAPage}
import navigation.Navigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel}
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

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val addresses = request.userAnswers.map(BorrowedAmountAndRatePages(srn))

    if (addresses.nonEmpty) {
      val viewModel = BorrowInstancesListController.viewModel(srn, mode, addresses)
      Ok(view(form, viewModel))
    } else {
      Redirect(controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, mode))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val addresses = request.userAnswers.map(BorrowedAmountAndRatePages(srn))

    if (addresses.size == maxBorrows) {
      Redirect(navigator.nextPage(BorrowInstancesListPage(srn, addBorrow = false), mode, request.userAnswers))
    } else {
      val viewModel = BorrowInstancesListController.viewModel(srn, mode, addresses)

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
      "landOrPropertyList.radios.error.required"
    )

  private def rows(srn: Srn, mode: Mode, borrow: Map[String, (Money, Percentage)]): List[ListRow] =
    borrow.flatMap {
      case (index, amount) =>
        refineV[Max5000.Refined](index.toInt + 1).fold(
          _ => Nil,
          index =>
            List(
              ListRow(
                amount._1.displayAs,
                changeUrl = controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedCYAController
                  .onPageLoad(srn, index, CheckOrChange.Check)
                  .url,
                changeHiddenText = Message("landOrPropertyList.row.change.hiddenText", amount._1.displayAs),
                controllers.routes.UnauthorisedController.onPageLoad().url, //TODO change with remove controller
                Message("landOrPropertyList.row.remove.hiddenText")
              )
            )
        )
    }.toList

  def viewModel(srn: Srn, mode: Mode, addresses: Map[String, (Money, Percentage)]): FormPageViewModel[ListViewModel] = {

    val title = if (addresses.size == 1) "landOrPropertyList.title" else "landOrPropertyList.title.plural"
    val heading = if (addresses.size == 1) "landOrPropertyList.heading" else "landOrPropertyList.heading.plural"

    FormPageViewModel(
      Message(title, addresses.size),
      Message(heading, addresses.size),
      ParagraphMessage("landOrPropertyList.paragraph"),
      ListViewModel(
        inset = "landOrPropertyList.inset",
        rows(srn, mode, addresses),
        Message("landOrPropertyList.radios"),
        showRadios = addresses.size < 25,
        paginatedViewModel = None
      ),
      controllers.nonsipp.moneyborrowed.routes.BorrowInstancesListController.onSubmit(srn, mode)
    )
  }
}
