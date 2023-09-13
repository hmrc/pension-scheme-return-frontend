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

$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

import controllers.actions._
import forms.MoneyFormProvider
import models.{Mode, Money}
import forms.mappings.errors.MoneyFormErrors
import config.Constants
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel, QuestionField}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.DisplayMessage.{Empty, Message}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import views.html.MoneyView
import services.SaveService
import viewmodels.implicits._
import $className;format="cap"$Controller._
$if(directory.empty)$
import pages.$className$Page
$else$
import pages.nonsipp.$directory$.$className$Page
$endif$
$if(!index.empty)$
import config.Refined.$index$
$endif$

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class $className;format="cap"$Controller @Inject()(
    override val messagesApi: MessagesApi,
    saveService: SaveService,
    @Named("non-sipp") navigator: Navigator,
    identifyAndRequireData: IdentifyAndRequireData,
    formProvider: MoneyFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: MoneyView,
)(implicit ec: ExecutionContext) extends PSRController {

  private val form = $className;format="cap"$Controller.form(formProvider)

  $if(index.empty)$
  def onPageLoad(srn:Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn), form)
      Ok(view(viewModel(srn, preparedForm, mode)))
  }
  $else$
  def onPageLoad(srn:Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn, index), form)
      Ok(view(viewModel(srn, index, preparedForm, mode)))
  }
  $endif$

  $if(index.empty)$
  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
  $else$
  def onSubmit(srn: Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
  $endif$
    implicit request =>
      form.bindFromRequest().fold(
        $if(index.empty)$
        formWithErrors => Future.successful(BadRequest(view(viewModel(srn, formWithErrors, mode)))),
        $else$
        formWithErrors => Future.successful(BadRequest(view(viewModel(srn, index, formWithErrors, mode)))),
        $endif$
        value =>
          for {
            $if(index.empty)$
            updatedAnswers <- Future.fromTry(request.userAnswers.transformAndSet($className$Page(srn), value))
            $else$
            updatedAnswers <- Future.fromTry(request.userAnswers.transformAndSet($className$Page(srn, index), value))
            $endif$
            _              <- saveService.save(updatedAnswers)
          $if(index.empty)$
          } yield Redirect(navigator.nextPage($className$Page(srn), mode, updatedAnswers))
          $else$
          } yield Redirect(navigator.nextPage($className$Page(srn, index), mode, updatedAnswers))
          $endif$
      )
  }
}

object $className;format="cap"$Controller {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "$className;format="decap"$.error.required",
      "$className;format="decap"$.error.invalid",
      (Constants.maxMoneyValue, "$className;format="decap"$.error.tooLarge")
    )
  )

  $if(index.empty)$
  def viewModel(srn: Srn, form: Form[Money], mode: Mode): FormPageViewModel[SingleQuestion[Money]] = {
  $else$
  def viewModel(srn: Srn, index: $index$, form: Form[Money], mode: Mode): FormPageViewModel[SingleQuestion[Money]] = {
  $endif$
    FormPageViewModel(
      "$className;format="decap"$.title",
      "$className;format="decap"$.heading",
      SingleQuestion(
        form,
        $if(hint.empty)$
        QuestionField.input(Empty)
        $else$
        QuestionField.input(Empty, Some("$className;format="decap"$.hint"))
        $endif$
      ),
     $if(index.empty)$
      $if(directory.empty)$
      controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, mode)
      $else$
      controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, mode)
      $endif$
    $else$
    $if(directory.empty)$
      controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, index, mode)
      $else$
      controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, index, mode)
      $endif$
    $endif$
    )
  }
}