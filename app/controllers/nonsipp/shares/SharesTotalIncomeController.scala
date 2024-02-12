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

package controllers.nonsipp.shares

import config.Constants
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.shares.SharesTotalIncomeController._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesCompleted, SharesTotalIncomePage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField, SectionCompleted}
import views.html.MoneyView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class SharesTotalIncomeController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = SharesTotalIncomeController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { companyName =>
        val preparedForm = request.userAnswers.fillForm(SharesTotalIncomePage(srn, index), form)
        Ok(view(viewModel(srn, index, companyName, preparedForm, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { companyName =>
              Future.successful(BadRequest(view(viewModel(srn, index, companyName, formWithErrors, mode))))
            }
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .transformAndSet(SharesTotalIncomePage(srn, index), value)
                    .set(SharesCompleted(srn, index), SectionCompleted)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(SharesTotalIncomePage(srn, index), mode, updatedAnswers))
        )
  }
}

object SharesTotalIncomeController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "sharesTotalIncome.error.required",
      "sharesTotalIncome.error.invalid",
      (Constants.maxMoneyValue, "sharesTotalIncome.error.tooLarge"),
      (Constants.minPosMoneyValue, "sharesTotalIncome.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    companyName: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "sharesTotalIncome.title",
      Message("sharesTotalIncome.heading", companyName),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("sharesTotalIncome.hint"))
      ),
      routes.SharesTotalIncomeController.onSubmit(srn, index, mode)
    )
}
