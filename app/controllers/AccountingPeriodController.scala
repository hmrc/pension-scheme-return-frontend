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
import controllers.actions._
import forms.DateRangeFormProvider
import forms.mappings.DateFormErrors
import models.SchemeId.Srn
import models.{DateRange, Mode}
import navigation.Navigator
import pages.AccountingPeriodPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
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
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = AccountingPeriodController.form(formProvider)
  private val viewModel = AccountingPeriodController.viewModel _

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      Ok(view(form.fromUserAnswers(AccountingPeriodPage(srn)), viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AccountingPeriodPage(srn), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(AccountingPeriodPage(srn), mode, updatedAnswers))
      )
  }
}

object AccountingPeriodController {

  def form(formProvider: DateRangeFormProvider): Form[DateRange] = formProvider(
    DateFormErrors(
      "accountPeriod.startDate.required.all",
      "accountPeriod.startDate.required.day",
      "accountPeriod.startDate.required.month",
      "accountPeriod.startDate.required.year",
      "accountPeriod.startDate.invalid.date",
      "accountPeriod.startDate.invalid.characters"
    ),
    DateFormErrors(
      "accountPeriod.endDate.required.all",
      "accountPeriod.endDate.required.day",
      "accountPeriod.endDate.required.month",
      "accountPeriod.endDate.required.year",
      "accountPeriod.endDate.invalid.date",
      "accountPeriod.endDate.invalid.characters"
    )
  )

  def viewModel(srn: Srn, mode: Mode): DateRangeViewModel = DateRangeViewModel(
    "accountPeriod.title",
    "accountPeriod.heading",
    Some("accountPeriod.description"),
    routes.AccountingPeriodController.onSubmit(srn, mode)
  )
}
