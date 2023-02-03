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

import controllers.actions._
import forms.YesNoPageFormProvider
import models.{Mode, SchemeDetails}
import models.SchemeId.Srn
import navigation.Navigator
import pages.CheckReturnDatesPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.CurrentTaxYear
import utils.DateTimeUtils
import viewmodels.DisplayMessage
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckReturnDatesController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  taxYear: TaxYearService
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider("checkReturnDates.error.required", "checkReturnDates.error.invalid")

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(CheckReturnDatesPage(srn)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      val viewModel = CheckReturnDatesController.viewModel(srn, mode, taxYear.current.starts, taxYear.current.finishes)

      Ok(view(preparedForm, viewModel))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData).async {
    implicit request =>

      val viewModel = CheckReturnDatesController.viewModel(srn, mode, taxYear.current.starts, taxYear.current.finishes)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckReturnDatesPage(srn), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(CheckReturnDatesPage(srn), mode, updatedAnswers))
      )
  }
}

object CheckReturnDatesController {

  def viewModel(
    srn: Srn,
    mode: Mode,
    fromDate: LocalDate,
    toDate: LocalDate
  ): YesNoPageViewModel = {
    YesNoPageViewModel(
      DisplayMessage("checkReturnDates.title"),
      DisplayMessage("checkReturnDates.heading"),
      Some(
        DisplayMessage(
          "checkReturnDates.description",
          DateTimeUtils.formatHtml(fromDate),
          DateTimeUtils.formatHtml(toDate)
        )
      ),
      DisplayMessage("checkReturnDates.legend"),
      routes.CheckReturnDatesController.onSubmit(srn, mode)
    )
  }
}
