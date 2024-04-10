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

package controllers.nonsipp.employercontributions

import viewmodels.implicits._
import controllers.nonsipp.employercontributions.EmployerNameController._
import play.api.mvc._
import config.Refined.{Max300, Max50}
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.employercontributions.EmployerNamePage
import services.SaveService
import views.html.TextInputView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class EmployerNameController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = EmployerNameController.form(formProvider)

  private val currentPage: (Srn, Max300, Max50, Mode) => Call = routes.EmployerNameController.onSubmit

  def onPageLoad(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm = request.userAnswers.fillForm(EmployerNamePage(srn, memberIndex, index), form)
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
              updatedAnswers <- request.userAnswers.set(EmployerNamePage(srn, memberIndex, index), value).mapK
              nextPage = navigator.nextPage(EmployerNamePage(srn, memberIndex, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, memberIndex, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object EmployerNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "employerName.error.required",
    "employerName.error.tooLong",
    "employerName.error.invalid"
  )

  def viewModel(srn: Srn, memberIndex: Max300, index: Max50, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      "employerName.title",
      "employerName.heading",
      TextInputViewModel(),
      routes.EmployerNameController.onSubmit(srn, memberIndex, index, mode)
    )
}
