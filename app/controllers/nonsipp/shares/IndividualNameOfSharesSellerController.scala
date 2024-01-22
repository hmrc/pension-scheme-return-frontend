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

package controllers.nonsipp.shares

import config.Refined.{Max300, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.shares.IndividualNameOfSharesSellerController._
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.shares.IndividualNameOfSharesSellerPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.implicits._
import viewmodels.models._
import views.html.TextInputView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IndividualNameOfSharesSellerController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = IndividualNameOfSharesSellerController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm = request.userAnswers.fillForm(IndividualNameOfSharesSellerPage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(IndividualNameOfSharesSellerPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(IndividualNameOfSharesSellerPage(srn, index), mode, updatedAnswers))
        )
    }
}

object IndividualNameOfSharesSellerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "individualNameOfSharesSeller.error.required",
    "individualNameOfSharesSeller.error.tooLong",
    "individualNameOfSharesSeller.error.invalid"
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      "individualNameOfSharesSeller.title",
      "individualNameOfSharesSeller.heading",
      TextInputViewModel(),
      routes.IndividualNameOfSharesSellerController.onSubmit(srn, index, mode)
    )
}
