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

package controllers.nonsipp.loansmadeoroutstanding

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import controllers.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingController.viewModel
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LoansMadeOrOutstandingController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form: Form[Boolean] =
    LoansMadeOrOutstandingController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    val preparedForm = request.userAnswers.fillForm(LoansMadeOrOutstandingPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, schemeName, mode)))
  }
  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, schemeName, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(LoansMadeOrOutstandingPage(srn), value))
            _ <- saveService.save(updatedAnswers)
            redirectTo <- if (value) {
              Future.successful(Redirect(navigator.nextPage(LoansMadeOrOutstandingPage(srn), mode, updatedAnswers)))
            } else {
              psrSubmissionService
                .submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall = controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
                    .onPageLoad(srn, mode)
                )(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                .map {
                  case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  case Some(_) => Redirect(navigator.nextPage(LoansMadeOrOutstandingPage(srn), mode, updatedAnswers))
                }
            }
          } yield redirectTo
      )
  }
}
object LoansMadeOrOutstandingController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "loansMadeOrOutstanding.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("loansMadeOrOutstanding.title"),
      Message("loansMadeOrOutstanding.heading", schemeName),
      routes.LoansMadeOrOutstandingController.onSubmit(srn, mode)
    )
}
