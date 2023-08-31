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
import controllers.nonsipp.landorproperty.PropertyAcquiredFromController._
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.{IdentityType, Mode}
import navigation.Navigator
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPage, PropertyAcquiredFromPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PropertyAcquiredFromController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  formProvider: RadioListFormProvider,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form = PropertyAcquiredFromController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(PropertyAcquiredFromPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, address.addressLine1, mode)))
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
                  view(errors, viewModel(srn, index, address.addressLine1, mode))
                )
              )
            },
          success =>
            for {
              userAnswers <- Future
                .fromTry(request.userAnswers.set(PropertyAcquiredFromPage(srn, index), success))
              _ <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(PropertyAcquiredFromPage(srn, index), mode, userAnswers))
            }
        )
  }
}

object PropertyAcquiredFromController {

  def form(formProvider: RadioListFormProvider): Form[IdentityType] =
    formProvider("landOrPropertyAcquiredFrom.error.required")

  def viewModel(
    srn: Srn,
    index: Max5000,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "landOrPropertyAcquiredFrom.title",
      Message("landOrPropertyAcquiredFrom.heading", addressLine1),
      List(
        RadioListRowViewModel("landOrPropertyAcquiredFrom.option1", IdentityType.Individual.name),
        RadioListRowViewModel("landOrPropertyAcquiredFrom.option2", IdentityType.UKCompany.name),
        RadioListRowViewModel("landOrPropertyAcquiredFrom.option3", IdentityType.UKPartnership.name),
        RadioListRowViewModel("landOrPropertyAcquiredFrom.option4", IdentityType.Other.name)
      ),
      routes.PropertyAcquiredFromController.onSubmit(srn, index, mode)
    )
}