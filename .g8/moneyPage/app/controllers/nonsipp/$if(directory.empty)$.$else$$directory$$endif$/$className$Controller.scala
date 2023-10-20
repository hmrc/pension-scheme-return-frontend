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

$! Generic imports !$
$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

$if(directory.empty)$
import pages.nonsipp.$className$Page
import controllers.nonsipp.$className;format="cap"$Controller._
$else$
import pages.nonsipp.$directory$.$className$Page
import controllers.nonsipp.$directory$.$className;format="cap"$Controller._
$endif$

$if(!index.empty)$
import config.Refined._
$endif$

import controllers.actions._
import models._
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.models._
import viewmodels.implicits._
import services.SaveService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
$! Generic imports end!$

import config.Constants
import forms.mappings.errors.MoneyFormErrorProvider
import forms.mappings.errors.MoneyFormErrorValue
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.DisplayMessage._
import play.api.i18n.{I18nSupport, MessagesApi}
import views.html.MoneyView

class $className;format="cap"$Controller @Inject()(
    override val messagesApi: MessagesApi,
    saveService: SaveService,
    @Named("non-sipp") navigator: Navigator,
    identifyAndRequireData: IdentifyAndRequireData,
    formProvider: MoneyFormErrorProvider,
    val controllerComponents: MessagesControllerComponents,
    view: MoneyView,
)(implicit ec: ExecutionContext) extends PSRController {

  private val form = $className;format="cap"$Controller.form(formProvider)

  $! Generic functions (viewmodel might accept extra params) !$
  def onPageLoad(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.get($className$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$)).fold(form)(form.fill)
      $if(!requiredPage)$
      request.userAnswers.get($requiredPage$(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$)).getOrRecoverJourney { requiredPage =>
      $endif$
      Ok(view(viewModel(srn, $if(!requiredPage)$requiredPage, $endif$$if(!index.empty) $index, $endif$$if(!secondaryIndex.empty) $secondaryIndex, $endif$preparedForm, mode)))
      $if(!requiredPage) $
      }
      $endif$
  }

  def onSubmit(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          $if(!requiredPage)$
          request.userAnswers.get($requiredPage$(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$)).getOrRecoverJourney { requiredPage =>
          $endif$
          Future.successful(BadRequest(view(viewModel(srn, $if(!requiredPage)$requiredPage, $endif$$if(!index.empty) $index, $endif$$if(!secondaryIndex.empty) $secondaryIndex, $endif$formWithErrors, mode))))
          $if(!requiredPage) $
          }
          $endif$
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), mode, updatedAnswers))
      )
  }
  $! Generic functions end !$
}

object $className;format="cap"$Controller {
  def form(formProvider: MoneyFormErrorProvider): Form[Money] = formProvider(
    MoneyFormErrorValue(
      requiredKey = "$className;format="decap"$.error.required",
      nonNumericKey = "$className;format="decap"$.error.invalid",
      max = (Constants.maxMoneyValue, "$className;format="decap"$.error.tooLarge"),
      min = (Constants.minMoneyValue, "$className;format="decap"$.error.tooSmall")
    )
  )

  def viewModel(srn: Srn, $if(!requiredPage.empty)$requiredPage: ???, $endif$$if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$form: Form[Money], mode: Mode): FormPageViewModel[SingleQuestion[Money]] = {
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
      $! Generic onSubmit !$
      $if(directory.empty)$
      controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
      $else$
      controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
      $endif$
      $! Generic onSubmit end !$
    )
  }
}