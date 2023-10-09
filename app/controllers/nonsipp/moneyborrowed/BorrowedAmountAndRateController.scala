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

package controllers.nonsipp.moneyborrowed

import com.google.inject.Inject
import config.Constants.{maxCurrencyValue, maxPercentage, minPercentage}
import config.Refined.Max5000
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.loansmadeoroutstanding.routes
import forms.MultipleQuestionFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.{MoneyFormErrors, PercentageFormErrors}
import models.{Mode, Money, Percentage}
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.moneyborrowed.BorrowedAmountAndRatePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.MultipleQuestionsViewModel.DoubleDifferentQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MultipleQuestionView
import viewmodels.DisplayMessage._
import viewmodels.implicits._

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class BorrowedAmountAndRateController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      {
        val form = BorrowedAmountAndRateController.form
        val viewModel = BorrowedAmountAndRateController.viewModel(
          srn,
          index,
          mode,
          request.schemeDetails.schemeName,
          request.userAnswers.fillForm(BorrowedAmountAndRatePage(srn, index), form)
        )

        Ok(view(viewModel))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val form = BorrowedAmountAndRateController.form

      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel =
              BorrowedAmountAndRateController
                .viewModel(srn, index, mode, request.schemeDetails.schemeName, formWithErrors)

            Future.successful(BadRequest(view(viewModel)))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.transformAndSet(BorrowedAmountAndRatePage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(BorrowedAmountAndRatePage(srn, index), mode, updatedAnswers))
        )
  }
}
object BorrowedAmountAndRateController {

  private val field1Errors: MoneyFormErrors =
    MoneyFormErrors(
      "interestOnLoan.loanInterestAmount.error.required",
      "interestOnLoan.loanInterestAmount.error.nonNumeric",
      (maxCurrencyValue, "interestOnLoan.loanInterestAmount.error.max")
    )

  private val field2Errors: PercentageFormErrors =
    PercentageFormErrors(
      "interestOnLoan.loanInterestRate.error.required",
      "interestOnLoan.loanInterestRate.error.nonNumeric",
      (maxPercentage, "interestOnLoan.loanInterestRate.error.tooLarge"),
      (minPercentage, "interestOnLoan.loanInterestRate.error.tooLow")
    )
  def form(implicit messages: Messages): Form[(Money, Percentage)] =
    MultipleQuestionFormProvider(Mappings.money(field1Errors), Mappings.percentage(field2Errors))

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    schemeName: String,
    form: Form[(Money, Percentage)]
  ): FormPageViewModel[DoubleDifferentQuestion[Money, Percentage]] =
    FormPageViewModel(
      "interestOnLoan.loanInterestRate.title",
      Message("interestOnLoan.loanInterestRate.heading", schemeName),
      page = DoubleDifferentQuestion(
        form,
        QuestionField.currency(Message("interestOnLoan.loanInterestAmount.label")),
        QuestionField.percentage(
          Message("interestOnLoan.loanInterestRate.label"),
          Option(Message("interestOnLoan.loanInterestRate.hint"))
        )
      ),
      controllers.nonsipp.moneyborrowed.routes.BorrowedAmountAndRateController.onSubmit(srn, index, mode)
    )
}
