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
$! Generic imports end!$

import controllers.actions._
import forms.TextFormProvider
import javax.inject.{Inject, Named}
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.implicits._
import play.api.i18n.MessagesApi
import play.api.mvc._
import views.html.TextAreaView
import services.SaveService
import controllers.PSRController
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

class $className;format="cap"$Controller @Inject()(
   override val messagesApi: MessagesApi,
   saveService: SaveService,
   @Named("non-sipp") navigator: Navigator,
   identifyAndRequireData: IdentifyAndRequireData,
   formProvider: TextFormProvider,
   val controllerComponents: MessagesControllerComponents,
   view: TextAreaView
)(implicit ec: ExecutionContext) extends PSRController {

  private val form = $className;format="cap"$Controller.form(formProvider)

  $! Generic (viewModel might accept extra params) !$
  def onPageLoad(srn:Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.get($className$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)))
  }

  def onSubmit(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), mode, updatedAnswers))
      )
  }
  $! Generic end !$
}

object $className;format="cap"$Controller {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "$className;format="decap"$.error.required",
    "$className;format="decap"$.error.length",
    "$className;format="decap"$.error.invalid",
  )

  def viewModel(srn: Srn, $if(!index.empty)$index: $index$, $endif$$if(!secondaryIndex.empty)$secondaryIndex: $secondaryIndex$, $endif$mode: Mode): FormPageViewModel[TextAreaViewModel] =
    FormPageViewModel(
      title = "$className;format="decap"$.title",
      heading = "$className;format="decap"$.heading",
      description = None,
      page = TextAreaViewModel(),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      $! Generic !$
      $if(directory.empty)$
      controllers.nonsipp.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
      $else$
      controllers.nonsipp.$directory$.routes.$className;format="cap"$Controller.onSubmit(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$mode)
      $endif$
      $! Generic end !$
    )
}