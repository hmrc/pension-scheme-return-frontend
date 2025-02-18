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

import services.SaveService
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import pages.nonsipp.loansmadeoroutstanding.PartnershipRecipientNamePage
import play.api.data.Form
import views.html.TextInputView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PartnershipRecipientNameController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form = PartnershipRecipientNameController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          form.fromUserAnswers(PartnershipRecipientNamePage(srn, index)),
          PartnershipRecipientNameController.viewModel(srn, index, mode)
        )
      )
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, PartnershipRecipientNameController.viewModel(srn, index, mode)))
            ),
          answer => {
            for {
              updatedAnswers <- request.userAnswers.set(PartnershipRecipientNamePage(srn, index), answer).mapK
              nextPage = navigator.nextPage(PartnershipRecipientNamePage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(navigator.nextPage(PartnershipRecipientNamePage(srn, index), mode, updatedAnswers))
          }
        )
  }
}

object PartnershipRecipientNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "partnershipRecipientName.error.required",
    "partnershipRecipientName.error.length",
    "error.textarea.invalid"
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("partnershipRecipientName.title"),
      Message("partnershipRecipientName.heading"),
      TextInputViewModel(true),
      routes.PartnershipRecipientNameController.onSubmit(srn, index, mode)
    )
}
