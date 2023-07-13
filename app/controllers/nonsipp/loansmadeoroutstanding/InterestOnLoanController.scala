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

package controllers.nonsipp.loansmadeoroutstanding

import controllers.nonsipp.loansmadeoroutstanding.InterestOnLoanController._
import com.google.inject.Inject
import config.Constants.{maxCurrencyValue, maxPercentage}
import controllers.actions._
import forms.mappings.Mappings
import forms.MultipleQuestionFormProvider
import forms.mappings.errors.{DoubleFormErrors, MoneyFormErrors}
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.InterestOnLoanPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MultipleQuestionView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class InterestOnLoanController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  saveService: SaveService,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    {
      val form = InterestOnLoanController.form
      val viewModel = InterestOnLoanController.viewModel(
        srn,
        mode,
        request.schemeDetails.schemeName,
        request.userAnswers.fillForm(InterestOnLoanPage(srn), form)
      )

      Ok(view(viewModel))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = InterestOnLoanController.form

    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val viewModel =
            InterestOnLoanController.viewModel(srn, mode, request.schemeDetails.schemeName, formWithErrors)

          Future.successful(BadRequest(view(viewModel)))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.transformAndSet(InterestOnLoanPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(InterestOnLoanPage(srn), mode, updatedAnswers))
      )
  }
}

object InterestOnLoanController {

  private val field1Errors: MoneyFormErrors =
    MoneyFormErrors(
      "interestOnLoan.field1.error.required",
      "interestOnLoan.field1.error.nonNumeric",
      (maxCurrencyValue, "interestOnLoan.field1.error.max")
    )

  private val field2Errors: DoubleFormErrors =
    DoubleFormErrors(
      "interestOnLoan.field2.error.required",
      "interestOnLoan.field2.error.nonNumeric",
      (maxPercentage, "")
    )

  private val field3Errors: MoneyFormErrors =
    MoneyFormErrors(
      "interestOnLoan.field3.error.required",
      "interestOnLoan.field3.error.nonNumeric",
      (maxCurrencyValue, "interestOnLoan.field3.error.max")
    )

  def form(implicit messages: Messages): Form[(Money, Double, Money)] =
    MultipleQuestionFormProvider(
      Mappings.money(field1Errors),
      Mappings.double(field2Errors),
      Mappings.money(field3Errors)
    )

  def viewModel(
    srn: Srn,
    mode: Mode,
    schemeName: String,
    form: Form[(Money, Double, Money)]
  ): FormPageViewModel[TripleQuestion[Money, Double, Money]] =
    FormPageViewModel(
      "interestOnLoan.loanInterestRate.title",
      Message("interestOnLoan.loanInterestRate.heading", schemeName),
      page = TripleQuestion(
        form,
        QuestionField.currency(Message("interestOnLoan.field1.label")),
        QuestionField.percentage(
          Message("interestOnLoan.field2.label"),
          Option(Message("interestOnLoan.field2.hint"))
        ),
        QuestionField.currency(Message("interestOnLoan.field3.label"))
      ),
      routes.InterestOnLoanController.onSubmit(srn, mode)
    )
}
