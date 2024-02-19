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

package controllers.nonsipp.employercontributions

import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.employercontributions.EmployerContributionsController.viewModel
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.employercontributions.EmployerContributionsPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class EmployerContributionsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form: Form[Boolean] =
    EmployerContributionsController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(EmployerContributionsPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(
              request.userAnswers
                .set(EmployerContributionsPage(srn), value)
            )
            _ <- saveService.save(updatedAnswers)
            submissionResult <- if (value) Future.successful(Some(()))
            else psrSubmissionService.submitPsrDetails(srn, updatedAnswers)
          } yield submissionResult.getOrRecoverJourney(
            _ => Redirect(navigator.nextPage(EmployerContributionsPage(srn), mode, updatedAnswers))
          )
      )
  }
}

object EmployerContributionsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "employerContributions.error.required"
  )

  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("employerContributions.title"),
      Message("employerContributions.heading"),
      routes.EmployerContributionsController.onSubmit(srn, mode)
    )
}
