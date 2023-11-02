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

package controllers.nonsipp.memberpayments

import config.Constants
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.mappings.errors.{MoneyFormErrorProvider, MoneyFormErrorValue}
import models.{Mode, Money}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MoneyView
import viewmodels.implicits._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UnallocatedEmployerAmountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormErrorProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = UnallocatedEmployerAmountController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm = request.userAnswers.get(UnallocatedEmployerAmountPage(srn)).fold(form)(form.fill)

      Ok(
        view(
          UnallocatedEmployerAmountController
            .viewModel(srn, request.schemeDetails.schemeName, preparedForm, mode)
        )
      )

    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel =
              UnallocatedEmployerAmountController
                .viewModel(srn, request.schemeDetails.schemeName, formWithErrors, mode)

            Future.successful(BadRequest(view(viewModel)))
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
  def form(formProvider: MoneyFormErrorProvider): Form[Money] = formProvider(
    MoneyFormErrorValue(
      requiredKey = "unallocatedEmployerAmount.error.required",
      nonNumericKey = "unallocatedEmployerAmount.error.invalid",
      max = (Constants.maxMoneyValue, "unallocatedEmployerAmount.error.tooLarge"),
      min = (0d, "unallocatedEmployerAmount.error.invalid")
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
        QuestionField.input(Empty)
      ),
      controllers.nonsipp.memberpayments.routes.UnallocatedEmployerAmountController.onSubmit(srn, mode)
    )
}
