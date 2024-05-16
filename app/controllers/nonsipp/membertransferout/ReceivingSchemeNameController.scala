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

package controllers.nonsipp.membertransferout

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.{Max300, Max5}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import viewmodels.models.{FormPageViewModel, TextInputViewModel}
import play.api.data.Form
import views.html.TextInputViewWidth40
import models.SchemeId.Srn
import pages.nonsipp.membertransferout.ReceivingSchemeNamePage
import play.api.i18n.MessagesApi

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class ReceivingSchemeNameController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputViewWidth40
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = ReceivingSchemeNameController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, transferIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm = request.userAnswers.fillForm(ReceivingSchemeNamePage(srn, index, transferIndex), form)
      Ok(view(preparedForm, ReceivingSchemeNameController.viewModel(srn, index, transferIndex, mode)))
    }

  def onSubmit(srn: Srn, index: Max300, transferIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(formWithErrors, ReceivingSchemeNameController.viewModel(srn, index, transferIndex, mode))
                )
              ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(ReceivingSchemeNamePage(srn, index, transferIndex), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(ReceivingSchemeNamePage(srn, index, transferIndex), mode, updatedAnswers)
            )
        )
    }
}

object ReceivingSchemeNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "TransferOut.receivingSchemeName.error.required",
    "TransferOut.receivingSchemeName.error.tooLong",
    "TransferOut.receivingSchemeName.error.invalid"
  )

  def viewModel(srn: Srn, index: Max300, transferIndex: Max5, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      "TransferOut.receivingSchemeName.title",
      "TransferOut.receivingSchemeName.heading",
      TextInputViewModel(isFixedLength = true),
      controllers.nonsipp.membertransferout.routes.ReceivingSchemeNameController
        .onSubmit(srn, index, transferIndex, mode)
    )
}
