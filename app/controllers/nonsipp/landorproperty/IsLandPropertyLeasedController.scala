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
import controllers.nonsipp.landorproperty.IsLandPropertyLeasedController._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.landorproperty.{IsLandPropertyLeasedPage, LandOrPropertyAddressLookupPage}
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

class IsLandPropertyLeasedController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = IsLandPropertyLeasedController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(IsLandPropertyLeasedPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, address.addressLine1, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, address.addressLine1, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(IsLandPropertyLeasedPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(IsLandPropertyLeasedPage(srn, index), mode, updatedAnswers))
        )
  }
}

object IsLandPropertyLeasedController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "isLandPropertyLeased.error.required"
  )

  def viewModel(srn: Srn, index: Max5000, addressLine1: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "IsLandPropertyLeased.title",
      Message("IsLandPropertyLeased.heading", addressLine1),
      routes.IsLandPropertyLeasedController.onSubmit(srn, index, mode)
    )
}
