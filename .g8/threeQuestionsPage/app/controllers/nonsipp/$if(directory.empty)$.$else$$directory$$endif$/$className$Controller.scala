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
import config.RefinedTypes._
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
import play.api.i18n.MessagesApi
import views.html.MultipleQuestionView
import forms.mappings.Mappings
import forms.MultipleQuestionFormProvider

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
$! Generic imports end!$

class $className;format="cap"$Controller @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  saveService: SaveService
)(implicit ec: ExecutionContext) extends PSRController {

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
  private val field1Errors: $field1Type$FormErrors =
    $field1Type$FormErrors(
      "$className;format="decap"$.field1.error.required"
    )

  private val field2Errors: $field2Type$FormErrors =
    $field2Type$FormErrors(
      "$className;format="decap"$.field2.error.required",
      (123, "$className;format="decap"$.field2.error.required")
    )

  private val field3Errors: $field3Type$FormErrors =
    $field3Type$FormErrors(
      "$className;format="decap"$.field3.error.required",
      (123, "$className;format="decap"$.field3.error.required")
    )

  val form: Form[($field1Type$, $field2Type$, $field3Type$)] =
    MultipleQuestionFormProvider(
      Mappings.$field1Type;format="decap"$(field1Errors),
      Mappings.$field2Type;format="decap"$(field2Errors),
      Mappings.$field3Type;format="decap"$(field3Errors)
    )

  def viewModel(
     srn: Srn,
     $if(!index.empty)$index: $index$,$endif$
     form: Form[($field1Type$, $field2Type$, $field3Type$)],
     mode: Mode,
   ): FormPageViewModel[TripleQuestion[$field1Type$, $field2Type$, $field3Type$]] = FormPageViewModel(
    title = "$className;format="decap"$.title",
    heading = "$className;format="decap"$.heading",
    $if(paragraph.empty)$
    description = None,
    $else$
    description = Some("$className;format="decap"$.paragraph"),
    $endif$
    page = TripleQuestion(
      form,
      QuestionField.$field1Type;format="decap"$("$className;format="decap"$.field1.label"),
      QuestionField.$field2Type;format="decap"$("$className;format="decap"$.field2.label"),
      QuestionField.$field3Type;format="decap"$("$className;format="decap"$.field3.label"),
    ),
    refresh = None,
    buttonText = "site.saveAndContinue",
    details = None,
    $if(index.empty)$
    onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, mode)
    $else$
    onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, mode)
    $endif$
  )
}
