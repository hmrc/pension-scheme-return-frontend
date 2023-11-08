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

import config.Constants
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.landorproperty._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import LandOrPropertyTotalIncomeController._
import views.html.MoneyView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LandOrPropertyTotalIncomeController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyTotalIncomeController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(LandOrPropertyTotalIncomePage(srn, index), form)
        Ok(view(viewModel(srn, index, preparedForm, address.addressLine1, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(BadRequest(view(viewModel(srn, index, formWithErrors, address.addressLine1, mode))))
            }
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.transformAndSet(LandOrPropertyTotalIncomePage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(LandOrPropertyTotalIncomePage(srn, index), mode, updatedAnswers))
        )
  }
}

object LandOrPropertyTotalIncomeController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "landOrPropertyTotalIncome.error.required",
      "landOrPropertyTotalIncome.error.invalid",
      (Constants.maxMoneyValue, "landOrPropertyTotalIncome.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    form: Form[Money],
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "landOrPropertyTotalIncome.title",
      Message("landOrPropertyTotalIncome.heading", addressLine1),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("landOrPropertyTotalIncome.hint"))
      ),
      controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController.onSubmit(srn, index, mode)
    )
}
