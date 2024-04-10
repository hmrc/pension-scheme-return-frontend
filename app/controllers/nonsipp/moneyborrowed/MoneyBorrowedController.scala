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

package controllers.nonsipp.moneyborrowed

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import controllers.nonsipp.moneyborrowed.MoneyBorrowedController.viewModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class MoneyBorrowedController @Inject()(
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
    MoneyBorrowedController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    val preparedForm = request.userAnswers.fillForm(MoneyBorrowedPage(srn), form)
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set(MoneyBorrowedPage(srn), value))
            _ <- saveService.save(updatedAnswers)
            redirectTo <- if (value) {
              Future.successful(Redirect(navigator.nextPage(MoneyBorrowedPage(srn), mode, updatedAnswers)))
            } else {
              psrSubmissionService
                .submitPsrDetails(srn)(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                .map {
                  case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  case Some(_) => Redirect(navigator.nextPage(MoneyBorrowedPage(srn), mode, updatedAnswers))
                }
            }
          } yield redirectTo
      )
  }
}
object MoneyBorrowedController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "moneyBorrowed.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("moneyBorrowed.title"),
      Message("moneyBorrowed.heading", schemeName),
      routes.MoneyBorrowedController.onSubmit(srn, mode)
    )
}
