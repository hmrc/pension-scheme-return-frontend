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

package controllers.nonsipp.loansmadeoroutstanding

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Constants.{maxCurrencyValue, maxPercentage, minPercentage}
import controllers.actions._
import navigation.Navigator
import forms.MultipleQuestionFormProvider
import models.{Mode, Money, Percentage}
import pages.nonsipp.loansmadeoroutstanding.InterestOnLoanPage
import play.api.data.Form
import forms.mappings.errors.{MoneyFormErrors, PercentageFormErrors}
import forms.mappings.Mappings
import com.google.inject.Inject
import config.Refined.Max5000
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class InterestOnLoanController @Inject()(
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
        val form = InterestOnLoanController.form()
        val viewModel = InterestOnLoanController.viewModel(
          srn,
          index,
          mode,
          request.schemeDetails.schemeName,
          form
        )

        Ok(view(request.userAnswers.fillForm(InterestOnLoanPage(srn, index), form), viewModel))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val form = InterestOnLoanController.form()

      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel =
              InterestOnLoanController.viewModel(srn, index, mode, request.schemeDetails.schemeName, form)

            Future.successful(BadRequest(view(formWithErrors, viewModel)))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.transformAndSet(InterestOnLoanPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(InterestOnLoanPage(srn, index), mode, updatedAnswers))
        )
  }
}

object InterestOnLoanController {

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

  private val field3Errors: MoneyFormErrors =
    MoneyFormErrors(
      "interestOnLoan.intReceivedCY.error.required",
      "interestOnLoan.intReceivedCY.error.nonNumeric",
      (maxCurrencyValue, "interestOnLoan.intReceivedCY.error.max")
    )

  def form(): Form[(Money, Percentage, Money)] =
    MultipleQuestionFormProvider(
      Mappings.money(field1Errors),
      Mappings.percentage(field2Errors),
      Mappings.money(field3Errors)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    schemeName: String,
    form: Form[(Money, Percentage, Money)]
  ): FormPageViewModel[TripleQuestion[Money, Percentage, Money]] =
    FormPageViewModel(
      "interestOnLoan.loanInterestRate.title",
      Message("interestOnLoan.loanInterestRate.heading", schemeName),
      page = TripleQuestion(
        form,
        QuestionField.currency(Message("interestOnLoan.loanInterestAmount.label")),
        QuestionField.percentage(
          Message("interestOnLoan.loanInterestRate.label"),
          Option(Message("interestOnLoan.loanInterestRate.hint"))
        ),
        QuestionField.currency(Message("interestOnLoan.intReceivedCY.label"))
      ),
      routes.InterestOnLoanController.onSubmit(srn, index, mode)
    )
}
