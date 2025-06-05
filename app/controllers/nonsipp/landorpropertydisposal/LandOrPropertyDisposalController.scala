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

package controllers.nonsipp.landorpropertydisposal

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalPage
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import play.api.i18n.MessagesApi
import views.html.YesNoPageView
import models.SchemeId.Srn
import models.Mode
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandOrPropertyDisposalController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(LandOrPropertyDisposalPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode)))
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(LandOrPropertyDisposalPage(srn), value))
            _ <- saveService.save(updatedAnswers)
            redirectTo <-
              if (value) {
                Future.successful(Redirect(navigator.nextPage(LandOrPropertyDisposalPage(srn), mode, updatedAnswers)))
              } else {
                psrSubmissionService
                  .submitPsrDetailsWithUA(
                    srn,
                    updatedAnswers,
                    fallbackCall = controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalController
                      .onPageLoad(srn, mode)
                  )(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) => Redirect(navigator.nextPage(LandOrPropertyDisposalPage(srn), mode, updatedAnswers))
                  }
              }
          } yield redirectTo
      )
  }
}

object LandOrPropertyDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "landOrPropertyDisposal.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "landOrPropertyDisposal.title",
      Message("landOrPropertyDisposal.heading", schemeName),
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalController.onSubmit(srn, mode)
    )
}
