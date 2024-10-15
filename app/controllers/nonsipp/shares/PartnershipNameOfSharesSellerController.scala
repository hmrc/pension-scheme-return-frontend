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

package controllers.nonsipp.shares

import services.SaveService
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import controllers.nonsipp.shares.PartnershipNameOfSharesSellerController._
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.shares.PartnershipShareSellerNamePage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PartnershipNameOfSharesSellerController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = PartnershipNameOfSharesSellerController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(PartnershipShareSellerNamePage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
    }
  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(PartnershipShareSellerNamePage(srn, index), value)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(PartnershipShareSellerNamePage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object PartnershipNameOfSharesSellerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "partnershipNameOfSharesSeller.error.required",
    "partnershipNameOfSharesSeller.error.tooLong",
    "partnershipNameOfSharesSeller.error.invalid"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] = FormPageViewModel(
    Message("partnershipNameOfSharesSeller.title"),
    Message("partnershipNameOfSharesSeller.heading"),
    TextInputViewModel(isFixedLength = true),
    routes.PartnershipNameOfSharesSellerController.onSubmit(srn, index, mode)
  )
}
