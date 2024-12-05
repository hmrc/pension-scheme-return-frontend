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

import services.{SaveService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc._
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import config.RefinedTypes.Max5000
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import forms.{MoneyFormProvider, MultipleQuestionFormProvider}
import forms.mappings.errors.MoneyFormErrors
import controllers.nonsipp.loansmadeoroutstanding.AmountOfTheLoanController._
import forms.mappings.Mappings
import com.google.inject.Inject
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import utils.nonsipp.PrePopulationUtils.isPrePopulation
import config.Constants.maxCurrencyValue
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode, Money}
import pages.nonsipp.loansmadeoroutstanding.AmountOfTheLoanPage
import cats.{Id, Monad}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, QuestionField}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class AmountOfTheLoanController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  formProvider: MoneyFormProvider,
  saveService: SaveService,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      usingSchemeDate[Id](srn) { period =>
        val previousAnswer = request.userAnswers.get(AmountOfTheLoanPage(srn, index))
        val form = AmountOfTheLoanController.form(formProvider, period)

        val preparedForm = if (isPrePopulation) {
          previousAnswer.fold(partialAnswersForm)(
            amountOfTheLoan => partialAnswersForm.fill(amountOfTheLoan.asTuple)
          )
        } else {
          request.userAnswers.fillForm(AmountOfTheLoanPage(srn, index), form)
        }

        val viewModel = AmountOfTheLoanController.viewModel(
          srn,
          index,
          mode,
          request.schemeDetails.schemeName,
          period,
          form
        )

        Ok(view(preparedForm, viewModel))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      usingSchemeDate(srn) { period =>
        val form = AmountOfTheLoanController.form(formProvider, period)

        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val viewModel =
                AmountOfTheLoanController
                  .viewModel(srn, index, mode, request.schemeDetails.schemeName, period, form)

              Future.successful(BadRequest(view(formWithErrors, viewModel)))
            },
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.transformAndSet(AmountOfTheLoanPage(srn, index), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(AmountOfTheLoanPage(srn, index), mode, updatedAnswers))
          )
      }
  }

  private def usingSchemeDate[F[_]: Monad](
    srn: Srn
  )(body: DateRange => F[Result])(implicit request: DataRequest[_]): F[Result] =
    schemeDateService.schemeDate(srn) match {
      case Some(period) => body(period)
      case None => Monad[F].pure(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
}

object AmountOfTheLoanController {

  private val field1Errors: MoneyFormErrors =
    MoneyFormErrors(
      "amountOfTheLoan.loanAmount.error.required",
      "amountOfTheLoan.loanAmount.error.nonNumeric",
      (maxCurrencyValue, "amountOfTheLoan.loanAmount.error.tooLarge")
    )

  private val field2Errors: MoneyFormErrors =
    MoneyFormErrors(
      "amountOfTheLoan.capRepaymentCY.error.required",
      "amountOfTheLoan.capRepaymentCY.error.nonNumeric",
      (maxCurrencyValue, "amountOfTheLoan.capRepaymentCY.error.tooLarge")
    )

  private val field3Errors: MoneyFormErrors =
    MoneyFormErrors(
      "amountOfTheLoan.amountOutstanding.error.required",
      "amountOfTheLoan.amountOutstanding.error.nonNumeric",
      (maxCurrencyValue, "amountOfTheLoan.amountOutstanding.error.tooLarge")
    )

  def form(formProvider: MoneyFormProvider, period: DateRange): Form[(Money, Money, Money)] =
    formProvider(
      field1Errors,
      field2Errors,
      field3Errors,
      Seq(period.to.show)
    )

  val partialAnswersForm: Form[(Money, Option[Money], Option[Money])] =
    MultipleQuestionFormProvider(
      Mappings.money(field1Errors),
      Mappings.optionalMoney(field2Errors),
      Mappings.optionalMoney(field3Errors)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    schemeName: String,
    period: DateRange,
    form: Form[(Money, Money, Money)]
  ): FormPageViewModel[TripleQuestion[Money, Money, Money]] =
    FormPageViewModel(
      "amountOfTheLoan.title",
      Message("amountOfTheLoan.heading", schemeName),
      page = TripleQuestion(
        form,
        QuestionField.currency(Message("amountOfTheLoan.loanAmount.label")),
        QuestionField.currency(Message("amountOfTheLoan.capRepaymentCY.label")),
        QuestionField.currency(Message("amountOfTheLoan.amountOutstanding.label", period.to.show))
      ),
      routes.AmountOfTheLoanController.onSubmit(srn, index, mode)
    )
}
