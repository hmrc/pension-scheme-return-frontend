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

package controllers.nonsipp.accountingperiod

import cats.implicits.toShow
import com.google.inject.Inject
import config.Constants.maxAccountingPeriods
import config.Refined.OneToThree
import controllers.actions._
import eu.timepit.refined.{refineMV, refineV}
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{DateRange, Mode}
import navigation.Navigator
import pages.nonsipp.accountingperiod.{AccountingPeriodListPage, AccountingPeriods}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ListRow, ListViewModel, PageViewModel}
import views.html.ListView

import javax.inject.Named

class AccountingPeriodListController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ListView,
  formProvider: YesNoPageFormProvider
) extends FrontendBaseController
    with I18nSupport {

  val form = AccountingPeriodListController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val periods = request.userAnswers.list(AccountingPeriods(srn))

    if (periods.nonEmpty) {

      val viewModel = AccountingPeriodListController.viewModel(srn, mode, periods)
      Ok(view(form, viewModel))
    } else {

      Redirect(routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), mode))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val periods = request.userAnswers.list(AccountingPeriods(srn))

    if (periods.length == maxAccountingPeriods) {

      Redirect(navigator.nextPage(AccountingPeriodListPage(srn, addPeriod = false), mode, request.userAnswers))
    } else {

      val viewModel = AccountingPeriodListController.viewModel(srn, mode, periods)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer => Redirect(navigator.nextPage(AccountingPeriodListPage(srn, answer), mode, request.userAnswers))
        )
    }
  }
}

object AccountingPeriodListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "accountingPeriods.radios.error.required"
    )

  private def rows(srn: Srn, mode: Mode, periods: List[DateRange]): List[ListRow] =
    periods.zipWithIndex.flatMap {
      case (range, index) =>
        refineV[OneToThree](index + 1).fold(
          _ => Nil,
          index =>
            List(
              ListRow(
                Message("accountingPeriods.row", range.from.show, range.to.show),
                routes.AccountingPeriodController.onPageLoad(srn, index, mode).url,
                Message("accountingPeriods.row.change.hiddenText", range.from.show, range.to.show),
                routes.RemoveAccountingPeriodController.onPageLoad(srn, index, mode).url,
                Message("accountingPeriods.row.remove.hiddenText", range.from.show, range.to.show)
              )
            )
        )
    }
  def viewModel(srn: Srn, mode: Mode, periods: List[DateRange]): PageViewModel[ListViewModel] = {

    val title = if (periods.length == 1) "accountingPeriods.title" else "accountingPeriods.title.plural"
    val heading = if (periods.length == 1) "accountingPeriods.heading" else "accountingPeriods.heading.plural"

    PageViewModel(
      Message(title, periods.length),
      Message(heading, periods.length),
      ListViewModel(
        rows(srn, mode, periods),
        Message("accountingPeriods.radios"),
        showRadios = periods.length < 3,
        paginatedViewModel = None
      ),
      routes.AccountingPeriodListController.onSubmit(srn, mode)
    ).withInset(Message("accountingPeriods.inset"))
  }
}
