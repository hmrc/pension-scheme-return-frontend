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

package controllers.nonsipp.memberpayments

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import controllers.PSRController
import config.Constants
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.MoneyFormProvider
import controllers.nonsipp.memberpayments.UnallocatedEmployerAmountController._
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import views.html.MoneyView
import models.SchemeId.Srn
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class UnallocatedEmployerAmountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = UnallocatedEmployerAmountController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm = request.userAnswers.get(UnallocatedEmployerAmountPage(srn)).fold(form)(form.fill)

      Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, form, mode)))
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  viewModel(srn, request.schemeDetails.schemeName, form, mode)
                )
              )
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.transformAndSet(UnallocatedEmployerAmountPage(srn), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(UnallocatedEmployerAmountPage(srn), mode, updatedAnswers))
        )
    }
}

object UnallocatedEmployerAmountController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      requiredKey = "unallocatedEmployerAmount.error.required",
      nonNumericKey = "unallocatedEmployerAmount.error.invalid",
      max = (Constants.maxMoneyValue, "unallocatedEmployerAmount.error.tooLarge"),
      min = (Constants.minPosMoneyValue, "unallocatedEmployerAmount.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    schemeName: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "unallocatedEmployerAmount.title",
      Message("unallocatedEmployerAmount.heading", schemeName),
      SingleQuestion(
        form,
        QuestionField.currency(Empty)
      ),
      controllers.nonsipp.memberpayments.routes.UnallocatedEmployerAmountController.onSubmit(srn, mode)
    )
}
