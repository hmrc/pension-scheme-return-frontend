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
import play.api.mvc._
import config.Refined._
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import controllers.nonsipp.employercontributions.OtherEmployeeDescriptionController._
import pages.nonsipp.employercontributions.{EmployerNamePage, OtherEmployeeDescriptionPage}
import services.SaveService
import views.html.TextAreaView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherEmployeeDescriptionController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextAreaView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = OtherEmployeeDescriptionController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(OtherEmployeeDescriptionPage(srn, index, secondaryIndex)).fold(form)(form.fill)
      request.userAnswers.get(EmployerNamePage(srn, index, secondaryIndex)).getOrRecoverJourney { employerName =>
        Ok(view(preparedForm, viewModel(srn, employerName, index, secondaryIndex, mode)))
      }
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(EmployerNamePage(srn, index, secondaryIndex)).getOrRecoverJourney { employerName =>
              Future
                .successful(BadRequest(view(formWithErrors, viewModel(srn, employerName, index, secondaryIndex, mode))))
            },
          value =>
            for {
              updatedAnswers <- request.userAnswers
                .set(OtherEmployeeDescriptionPage(srn, index, secondaryIndex), value)
                .mapK
              nextPage = navigator
                .nextPage(OtherEmployeeDescriptionPage(srn, index, secondaryIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, secondaryIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object OtherEmployeeDescriptionController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "otherEmployeeDescription.error.required",
    "otherEmployeeDescription.error.length",
    "otherEmployeeDescription.error.invalid"
  )

  def viewModel(
    srn: Srn,
    employerName: String,
    index: Max300,
    secondaryIndex: Max50,
    mode: Mode
  ): FormPageViewModel[TextAreaViewModel] =
    FormPageViewModel(
      title = "otherEmployeeDescription.title",
      heading = Message("otherEmployeeDescription.heading", employerName),
      description = None,
      page = TextAreaViewModel(),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      controllers.nonsipp.employercontributions.routes.OtherEmployeeDescriptionController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
