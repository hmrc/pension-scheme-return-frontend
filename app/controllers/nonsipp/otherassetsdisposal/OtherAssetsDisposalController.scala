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

package controllers.nonsipp.otherassetsdisposal

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.otherassetsdisposal.{OtherAssetsDisposalCompleted, OtherAssetsDisposalPage}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import controllers.nonsipp.otherassetsdisposal.OtherAssetsDisposalController.viewModel
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, SectionCompleted, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherAssetsDisposalController @Inject()(
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

  private val form = OtherAssetsDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.get(OtherAssetsDisposalPage(srn)).fold(form)(form.fill)
    Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode)))),
        addDisposal =>
          if (addDisposal) {
            for {
              updatedAnswers <- request.userAnswers
                .set(OtherAssetsDisposalPage(srn), true)
                .remove(OtherAssetsDisposalCompleted(srn))
                .mapK[Future]
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(OtherAssetsDisposalPage(srn), mode, updatedAnswers))
          } else {
            for {
              updatedAnswers <- request.userAnswers
                .set(OtherAssetsDisposalPage(srn), false)
                .set(OtherAssetsDisposalCompleted(srn), SectionCompleted)
                .mapK[Future]
              _ <- saveService.save(updatedAnswers)
              submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                srn,
                updatedAnswers,
                fallbackCall =
                  controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController.onPageLoad(srn, mode)
              )
            } yield submissionResult
              .getOrRecoverJourney(
                _ => Redirect(navigator.nextPage(OtherAssetsDisposalPage(srn), mode, updatedAnswers))
              )
          }
      )
  }
}

object OtherAssetsDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "otherAssetsDisposal.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      "otherAssetsDisposal.title",
      Message("otherAssetsDisposal.heading", schemeName),
      YesNoPageViewModel(
        hint = Some(Message("otherAssetsDisposal.hint"))
      ),
      routes.OtherAssetsDisposalController.onSubmit(srn, mode)
    )
}
