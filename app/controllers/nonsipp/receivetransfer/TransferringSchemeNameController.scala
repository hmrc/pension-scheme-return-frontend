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

package controllers.nonsipp.receivetransfer

import config.Refined.{Max300, Max50}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.receivetransfer.TransferringSchemeNameController._
import controllers.nonsipp.memberpayments.routes
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.receiveTransfer.TransferringSchemeNamePage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.implicits._
import viewmodels.models._
import views.html.TextInputView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TransferringSchemeNameController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TransferringSchemeNameController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm = request.userAnswers.fillForm(TransferringSchemeNamePage(srn, memberIndex, index), form)
      Ok(view(preparedForm, viewModel(srn, memberIndex, index, mode)))
    }

  def onSubmit(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, viewModel(srn, memberIndex, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(TransferringSchemeNamePage(srn, memberIndex, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(TransferringSchemeNamePage(srn, memberIndex, index), mode, updatedAnswers)
            )
        )
    }
}

object TransferringSchemeNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "transferringSchemeName.error.required",
    "transferringSchemeName.error.tooLong",
    "transferringSchemeName.error.invalid"
  )

  def viewModel(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      "transferringSchemeName.title",
      "transferringSchemeName.heading",
      TextInputViewModel(),
      controllers.nonsipp.receivetransfer.routes.TransferringSchemeNameController
        .onSubmit(srn, memberIndex, index, mode)
    )
}
