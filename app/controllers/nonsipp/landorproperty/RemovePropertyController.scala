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

package controllers.nonsipp.landorproperty

import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.Mode
import navigation.Navigator

import pages.nonsipp.landorproperty.{
  LandOrPropertyAddressLookupPage,
  RemovePropertyPage
}

import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemovePropertyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemovePropertyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(RemovePropertyPage(srn, index), form)
        Ok(view(preparedForm, RemovePropertyController.viewModel(srn, index, mode, address.addressLine1)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(
                BadRequest(
                  view(errors, RemovePropertyController.viewModel(srn, index, mode, address.addressLine1))
                )
              )
            },
          success =>
            for {
              userAnswers <- Future
                .fromTry(request.userAnswers.set(RemovePropertyPage(srn, index), success))
              _ <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(RemovePropertyPage(srn, index), mode, userAnswers))
            }
        )
  }
}

object RemovePropertyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeLandOrProperty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    addressLine1: String
  ): FormPageViewModel[YesNoPageViewModel] =

    YesNoPageViewModel(
      "removeLandOrProperty.title",
      Message("removeLandOrProperty.heading", addressLine1),
      routes.RemovePropertyController.onSubmit(srn, index, mode)
    )
}
