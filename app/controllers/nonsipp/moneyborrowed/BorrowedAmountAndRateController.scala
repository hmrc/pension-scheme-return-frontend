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

package controllers.nonsipp.moneyborrowed

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.DoubleDifferentQuestion
import config.RefinedTypes.Max5000
import config.Constants.{borrowMaxPercentage, borrowMinPercentage, maxCurrencyValue}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.MultipleQuestionFormProvider
import models.{Mode, Money, Percentage}
import play.api.data.Form
import forms.mappings.errors.{MoneyFormErrors, PercentageFormErrors}
import forms.mappings.Mappings
import com.google.inject.Inject
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.moneyborrowed.BorrowedAmountAndRatePage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

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
        val form = BorrowedAmountAndRateController.form()
        val viewModel = BorrowedAmountAndRateController.viewModel(
          srn,
          index,
          mode,
          request.schemeDetails.schemeName,
          form
        )

        Ok(view(request.userAnswers.fillForm(BorrowedAmountAndRatePage(srn, index), form), viewModel))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val form = BorrowedAmountAndRateController.form()

      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel =
              BorrowedAmountAndRateController
                .viewModel(srn, index, mode, request.schemeDetails.schemeName, form)

            Future.successful(BadRequest(view(formWithErrors, viewModel)))
          },
          value =>
            for {
              updatedAnswers <- request.userAnswers
                .transformAndSet(BorrowedAmountAndRatePage(srn, index), value)
                .mapK[Future]
              nextPage = navigator.nextPage(BorrowedAmountAndRatePage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield {
              Redirect(nextPage)
            }
        )
  }
}
object BorrowedAmountAndRateController {

  private val field1Errors: MoneyFormErrors =
    MoneyFormErrors(
      "moneyBorrowed.borrowedAmountAndRate.borrowAmount.error.required",
      "moneyBorrowed.borrowedAmountAndRate.borrowAmount.error.nonNumeric",
      (maxCurrencyValue, "moneyBorrowed.borrowedAmountAndRate.borrowAmount.error.tooHigh")
    )

  private val field2Errors: PercentageFormErrors =
    PercentageFormErrors(
      "moneyBorrowed.borrowedAmountAndRate.borrowInterestRate.error.required",
      "moneyBorrowed.borrowedAmountAndRate.borrowInterestRate.error.nonNumeric",
      (borrowMaxPercentage, "moneyBorrowed.borrowedAmountAndRate.borrowInterestRate.error.tooHigh"),
      (borrowMinPercentage, "moneyBorrowed.borrowedAmountAndRate.borrowInterestRate.error.tooLow")
    )
  def form(): Form[(Money, Percentage)] =
    MultipleQuestionFormProvider(Mappings.money(field1Errors), Mappings.percentage(field2Errors))

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    schemeName: String,
    form: Form[(Money, Percentage)]
  ): FormPageViewModel[DoubleDifferentQuestion[Money, Percentage]] =
    FormPageViewModel(
      "moneyBorrowed.borrowedAmountAndRate.title",
      Message("moneyBorrowed.borrowedAmountAndRate.heading", schemeName),
      page = DoubleDifferentQuestion(
        form,
        QuestionField.currency(Message("moneyBorrowed.borrowedAmountAndRate.borrowAmount.label")),
        QuestionField.percentage(
          Message("moneyBorrowed.borrowedAmountAndRate.borrowInterestRate.label"),
          Option(Message("moneyBorrowed.borrowedAmountAndRate.borrowInterestRate.hint"))
        )
      ),
      controllers.nonsipp.moneyborrowed.routes.BorrowedAmountAndRateController.onSubmit(srn, index, mode)
    )
}
