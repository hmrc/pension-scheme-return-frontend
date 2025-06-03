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
import models.ConditionalYesNo._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import config.RefinedTypes.Max5000
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.MultipleQuestionFormProvider
import models.{Mode, Money, Percentage}
import controllers.nonsipp.loansmadeoroutstanding.InterestOnLoanController._
import pages.nonsipp.loansmadeoroutstanding.{InterestOnLoanPage, OutstandingArrearsOnLoanPage, SecurityGivenForLoanPage}
import play.api.data.Form
import forms.mappings.errors.{MoneyFormErrors, PercentageFormErrors}
import forms.mappings.Mappings
import com.google.inject.Inject
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import utils.nonsipp.PrePopulationUtils.isPrePopulation
import config.Constants.{maxCurrencyValue, maxPercentage, minPercentage}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val previousAnswer = request.userAnswers.get(InterestOnLoanPage(srn, index))
      val form = InterestOnLoanController.form()

      val preparedForm = if (isPrePopulation) {
        previousAnswer.fold(partialAnswersForm)(
          interestOnLoan => partialAnswersForm.fill(interestOnLoan.asTuple)
        )
      } else {
        request.userAnswers.fillForm(InterestOnLoanPage(srn, index), form)
      }

      val viewModel = InterestOnLoanController.viewModel(
        srn,
        index,
        mode,
        request.schemeDetails.schemeName,
        form
      )

      Ok(view(preparedForm, viewModel))
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
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
              updatedAnswers <- request.userAnswers.transformAndSet(InterestOnLoanPage(srn, index), value).mapK
              nextPage = navigator.nextPage(InterestOnLoanPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield (
              isPrePopulation,
              request.userAnswers.get(SecurityGivenForLoanPage(srn, index)),
              request.userAnswers.get(OutstandingArrearsOnLoanPage(srn, index))
            ) match {
              case (true, Some(_), None) =>
                Redirect(
                  controllers.nonsipp.loansmadeoroutstanding.routes.OutstandingArrearsOnLoanController
                    .onPageLoad(srn, index, mode)
                )
              case _ =>
                Redirect(navigator.nextPage(InterestOnLoanPage(srn, index), mode, updatedAnswers))
            }
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

  val partialAnswersForm: Form[(Money, Percentage, Option[Money])] =
    MultipleQuestionFormProvider(
      Mappings.money(field1Errors),
      Mappings.percentage(field2Errors),
      Mappings.optionalMoney(field3Errors)
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
        QuestionField.currency(
          Message("interestOnLoan.loanInterestAmount.label"),
          Option(Message("interestOnLoan.loanInterestAmount.hint"))
        ),
        QuestionField.percentage(
          Message("interestOnLoan.loanInterestRate.label"),
          Option(Message("interestOnLoan.loanInterestRate.hint"))
        ),
        QuestionField.currency(Message("interestOnLoan.intReceivedCY.label"))
      ),
      routes.InterestOnLoanController.onSubmit(srn, index, mode)
    )
}
