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

package controllers.nonsipp.receivetransfer

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import pages.nonsipp.receivetransfer.DidSchemeReceiveTransferPage
import controllers.actions._
import controllers.nonsipp.receivetransfer.DidSchemeReceiveTransferController._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class DidSchemeReceiveTransferController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  psrSubmissionService: PsrSubmissionService,
  @Named("non-sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = DidSchemeReceiveTransferController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(DidSchemeReceiveTransferPage(srn)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode)))
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode)))),
          value =>
            for {
              updatedAnswers <- request.userAnswers
                .set(DidSchemeReceiveTransferPage(srn), value)
                .mapK[Future]
              _ <- saveService.save(updatedAnswers)
              submissionResult <-
                if (value) {
                  Future.successful(Some(()))
                } else {
                  psrSubmissionService.submitPsrDetailsWithUA(
                    srn,
                    updatedAnswers,
                    fallbackCall = controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController
                      .onPageLoad(srn, mode)
                  )
                }
            } yield submissionResult.getOrRecoverJourney(_ =>
              Redirect(navigator.nextPage(DidSchemeReceiveTransferPage(srn), mode, updatedAnswers))
            )
        )
    }
}

object DidSchemeReceiveTransferController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "didSchemeReceiveTransfer.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "didSchemeReceiveTransfer.title",
    Message("didSchemeReceiveTransfer.heading", schemeName),
    controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController.onSubmit(srn, mode)
  )
}
