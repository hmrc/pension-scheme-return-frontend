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

package controllers

import com.google.inject.Inject
import config.Refined.Max3
import controllers.actions._
import forms.DateRangeFormProvider
import forms.mappings.DateFormErrors
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode}
import navigation.Navigator
import pages.{AccountingPeriodPage, AccountingPeriods}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.FormUtils._
import utils.ListUtils.ListOps
import utils.RefinedUtils.RefinedIntOps
import viewmodels.models.DateRangeViewModel
import views.html.DateRangeView

import scala.concurrent.{ExecutionContext, Future}

class AccountingPeriodController @Inject()(
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: DateRangeView,
  formProvider: DateRangeFormProvider,
  saveService: SaveService,
  taxYearService: TaxYearService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(usedAccountingPeriods: List[DateRange] = List()) =
    AccountingPeriodController.form(formProvider, taxYearService.current, usedAccountingPeriods)
  private val viewModel = AccountingPeriodController.viewModel _

  def onPageLoad(srn: Srn, index: Max3, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(view(form().fromUserAnswers(AccountingPeriodPage(srn, index)), viewModel(srn, index, mode)))
    }

  def onSubmit(srn: Srn, index: Max3, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      val usedAccountingPeriods = duplicateAccountingPeriods(srn, index)

      form(usedAccountingPeriods)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(AccountingPeriodPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(AccountingPeriodPage(srn, index), mode, updatedAnswers))
        )
    }

  def duplicateAccountingPeriods(srn: Srn, index: Max3)(implicit request: DataRequest[_]): List[DateRange] =
    request.userAnswers.list(AccountingPeriods(srn)).removeAt(index.arrayIndex)
}

object AccountingPeriodController {

  def form(
    formProvider: DateRangeFormProvider,
    taxYear: TaxYear,
    usedAccountingPeriods: List[DateRange]
  ): Form[DateRange] = formProvider(
    DateFormErrors(
      "accountingPeriod.startDate.error.required.all",
      "accountingPeriod.startDate.error.required.day",
      "accountingPeriod.startDate.error.required.month",
      "accountingPeriod.startDate.error.required.year",
      "accountingPeriod.startDate.error.required.two",
      "accountingPeriod.startDate.error.invalid.date",
      "accountingPeriod.startDate.error.invalid.characters"
    ),
    DateFormErrors(
      "accountingPeriod.endDate.error.required.all",
      "accountingPeriod.endDate.error.required.day",
      "accountingPeriod.endDate.error.required.month",
      "accountingPeriod.endDate.error.required.year",
      "accountingPeriod.endDate.error.required.two",
      "accountingPeriod.endDate.error.invalid.date",
      "accountingPeriod.endDate.error.invalid.characters"
    ),
    "accountingPeriod.endDate.error.range.invalid",
    Some(DateRange(taxYear.starts, taxYear.finishes)),
    Some("accountingPeriod.startDate.error.outsideTaxYear"),
    Some("accountingPeriod.endDate.error.outsideTaxYear"),
    Some("accountingPeriod.startDate.error.duplicate"),
    usedAccountingPeriods
  )

  def viewModel(srn: Srn, index: Max3, mode: Mode): DateRangeViewModel = DateRangeViewModel(
    "accountingPeriod.title",
    "accountingPeriod.heading",
    Some("accountingPeriod.description"),
    routes.AccountingPeriodController.onSubmit(srn, index, mode)
  )
}
