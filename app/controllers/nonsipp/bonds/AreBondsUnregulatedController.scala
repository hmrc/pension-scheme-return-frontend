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

import services.SaveService
import pages.nonsipp.bonds.AreBondsUnregulatedPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined._
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.data.Form
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import controllers.nonsipp.bonds.AreBondsUnregulatedController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class AreBondsUnregulatedController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = AreBondsUnregulatedController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(AreBondsUnregulatedPage(srn, index)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, index, mode)))

    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode))))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(AreBondsUnregulatedPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(AreBondsUnregulatedPage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object AreBondsUnregulatedController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "bonds.areBondsUnregulated.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "bonds.areBondsUnregulated.title",
      Message("bonds.areBondsUnregulated.heading"),
      controllers.nonsipp.bonds.routes.AreBondsUnregulatedController.onSubmit(srn, index, mode)
    )
}
