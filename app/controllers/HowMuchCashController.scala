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

import cats.implicits.toShow
import com.google.inject.Inject
import config.Constants.maxCashInBank
import controllers.actions._
import forms.MoneyFormProvider
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.HowMuchCashPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.DateTimeUtils.localDateShow
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.MoneyViewModel
import views.html.MoneyView

import scala.concurrent.{ExecutionContext, Future}

class HowMuchCashController @Inject()(
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView,
  formProvider: MoneyFormProvider,
  saveService: SaveService,
  taxYearService: TaxYearService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def taxYear = taxYearService.current

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val form = HowMuchCashController.form(formProvider, request.schemeDetails.schemeName, taxYear)
    val viewModel = HowMuchCashController.viewModel(srn, mode, request.schemeDetails.schemeName, taxYear)

    Ok(view(form.fromUserAnswers(HowMuchCashPage(srn)), viewModel))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = HowMuchCashController.form(formProvider, request.schemeDetails.schemeName, taxYear)
    val viewModel = HowMuchCashController.viewModel(srn, mode, request.schemeDetails.schemeName, taxYear)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HowMuchCashPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(HowMuchCashPage(srn), mode, updatedAnswers))
      )
  }
}

object HowMuchCashController {

  def form(formProvider: MoneyFormProvider, schemeName: String, taxYear: TaxYear): Form[Money] = formProvider(
    "howMuchCash.error.required",
    "howMuchCash.error.nonNumeric",
    maxCashInBank -> "howMuchCash.error.tooLarge",
    Seq(schemeName, taxYear.starts.show)
  )

  def viewModel(srn: Srn, mode: Mode, schemeName: String, taxYear: TaxYear): MoneyViewModel = MoneyViewModel(
    Message("howMuchCash.title", schemeName, taxYear.starts.show),
    Message("howMuchCash.heading", schemeName, taxYear.starts.show),
    routes.HowMuchCashController.onSubmit(srn, mode)
  )
}
