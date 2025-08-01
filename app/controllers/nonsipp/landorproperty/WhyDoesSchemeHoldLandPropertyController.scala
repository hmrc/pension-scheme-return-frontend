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

package controllers.nonsipp.landorproperty

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.landorproperty.WhyDoesSchemeHoldLandPropertyController._
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{Mode, SchemeHoldLandProperty}
import play.api.i18n.MessagesApi
import play.api.data.Form
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, WhyDoesSchemeHoldLandPropertyPage}
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class WhyDoesSchemeHoldLandPropertyController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  formProvider: RadioListFormProvider,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form: Form[SchemeHoldLandProperty] = WhyDoesSchemeHoldLandPropertyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(WhyDoesSchemeHoldLandPropertyPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, request.schemeDetails.schemeName, address.addressLine1, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(
                BadRequest(
                  view(errors, viewModel(srn, index, request.schemeDetails.schemeName, address.addressLine1, mode))
                )
              )
            },
          success =>
            for {
              userAnswers <- Future
                .fromTry(request.userAnswers.set(WhyDoesSchemeHoldLandPropertyPage(srn, index), success))
              nextPage = navigator.nextPage(WhyDoesSchemeHoldLandPropertyPage(srn, index), mode, userAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, userAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }
}

object WhyDoesSchemeHoldLandPropertyController {

  def form(formProvider: RadioListFormProvider): Form[SchemeHoldLandProperty] =
    formProvider("whyDoesSchemeHoldLandProperty.error.required")

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "whyDoesSchemeHoldLandProperty.title",
      Message("whyDoesSchemeHoldLandProperty.heading", schemeName, addressLine1),
      List(
        RadioListRowViewModel(
          Message("whyDoesSchemeHoldLandProperty.option1"),
          SchemeHoldLandProperty.Acquisition.name,
          Message("whyDoesSchemeHoldLandProperty.option1.hint")
        ),
        RadioListRowViewModel("whyDoesSchemeHoldLandProperty.option2", SchemeHoldLandProperty.Contribution.name),
        RadioListRowViewModel("whyDoesSchemeHoldLandProperty.option3", SchemeHoldLandProperty.Transfer.name)
      ),
      routes.WhyDoesSchemeHoldLandPropertyController.onSubmit(srn, index, mode)
    )
}
