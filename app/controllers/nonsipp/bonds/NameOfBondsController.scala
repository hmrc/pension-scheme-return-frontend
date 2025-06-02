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

package controllers.nonsipp.bonds

import pages.nonsipp.bonds.NameOfBondsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.toRefined5000
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import viewmodels.models.{FormPageViewModel, TextAreaViewModel}
import play.api.data.Form
import services.SaveService
import controllers.nonsipp.bonds.NameOfBondsController._
import views.html.TextAreaView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class NameOfBondsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextAreaView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = NameOfBondsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(NameOfBondsPage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NameOfBondsPage(srn, index), value))
              nextPage = navigator.nextPage(NameOfBondsPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }
}

object NameOfBondsController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "bonds.nameOfBonds.error.required",
    "bonds.nameOfBonds.error.length",
    "error.textarea.invalid"
  )

  def viewModel(srn: Srn, index: Int, mode: Mode): FormPageViewModel[TextAreaViewModel] = FormPageViewModel(
    "bonds.nameOfBonds.title",
    "bonds.nameOfBonds.heading",
    TextAreaViewModel(),
    routes.NameOfBondsController.onSubmit(srn, index, mode)
  )
}
