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
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandOrPropertyTotalCostPage}
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import models.{Mode, Money}
import controllers.nonsipp.landorproperty.LandOrPropertyTotalCostController._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandOrPropertyTotalCostController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyTotalCostController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(LandOrPropertyTotalCostPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, address.addressLine1, form, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, index, address.addressLine1, form, mode))
                )
              )
            }
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.transformAndSet(LandOrPropertyTotalCostPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(LandOrPropertyTotalCostPage(srn, index), mode, updatedAnswers))
        )
  }
}

object LandOrPropertyTotalCostController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "landOrPropertyTotalCost.error.required",
      "landOrPropertyTotalCost.error.invalid",
      (Constants.maxMoneyValue, "landOrPropertyTotalCost.error.tooLarge"),
      (Constants.minPosMoneyValue, "landOrPropertyTotalCost.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    addressLine1: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "landOrPropertyTotalCost.title",
      Message("landOrPropertyTotalCost.heading", addressLine1),
      SingleQuestion(
        form,
        QuestionField.currency(Empty, Some("landOrPropertyTotalCost.hint"))
      ),
      routes.LandOrPropertyTotalCostController.onSubmit(srn, index, mode)
    )
}
