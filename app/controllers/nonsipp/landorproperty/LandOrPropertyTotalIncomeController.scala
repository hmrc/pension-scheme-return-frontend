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

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import services.SaveService
import controllers.nonsipp.landorproperty.LandOrPropertyTotalIncomeController._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField, SectionCompleted}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandOrPropertyTotalIncomeController @Inject() (
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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(LandOrPropertyTotalIncomePage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, form, address.addressLine1, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, index, form, address.addressLine1, mode))
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(LandOrPropertyTotalIncomePage(srn, index), value)
                    .set(LandOrPropertyCompleted(srn, index), SectionCompleted)
                )
              nextPage = navigator.nextPage(LandOrPropertyTotalIncomePage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
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
        QuestionField.currency(Empty, Some("landOrPropertyTotalIncome.hint"))
      ),
      controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController.onSubmit(srn, index, mode)
    )
}
