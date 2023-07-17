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

import controllers.nonsipp.loansmadeoroutstanding.AmountOfTheLoanController._
import cats.implicits.toShow
import cats.{Id, Monad}
import com.google.inject.Inject
import config.Constants.maxCurrencyValue
import config.Refined.Max9999999
import controllers.actions._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode, Money}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.AmountOfTheLoanPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MoneyView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class AmountOfTheLoanController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView,
  formProvider: MoneyFormProvider,
  saveService: SaveService,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      usingSchemeDate[Id](srn) { period =>
        val form = AmountOfTheLoanController.form(formProvider, period)
        val viewModel = AmountOfTheLoanController.viewModel(
          srn,
          index,
          mode,
          request.schemeDetails.schemeName,
          period,
          request.userAnswers.fillForm(AmountOfTheLoanPage(srn, index), form)
        )

        Ok(view(viewModel))
      }
  }

  def onSubmit(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      usingSchemeDate(srn) { period =>
        val form = AmountOfTheLoanController.form(formProvider, period)

        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val viewModel =
                AmountOfTheLoanController
                  .viewModel(srn, index, mode, request.schemeDetails.schemeName, period, formWithErrors)

              Future.successful(BadRequest(view(viewModel)))
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

  def form(formProvider: MoneyFormProvider, period: DateRange): Form[(Money, Money, Money)] =
    formProvider(
      MoneyFormErrors(
        "amountOfTheLoan.loanAmount.error.required",
        "amountOfTheLoan.loanAmount.error.nonNumeric",
        (maxCurrencyValue, "amountOfTheLoan.loanAmount.error.tooLarge")
      ),
      MoneyFormErrors(
        "amountOfTheLoan.capRepaymentCY.error.required",
        "amountOfTheLoan.capRepaymentCY.error.nonNumeric",
        (maxCurrencyValue, "amountOfTheLoan.capRepaymentCY.error.tooLarge")
      ),
      MoneyFormErrors(
        "amountOfTheLoan.amountOutstanding.error.required",
        "amountOfTheLoan.amountOutstanding.error.nonNumeric",
        (maxCurrencyValue, "amountOfTheLoan.amountOutstanding.error.tooLarge")
      ),
      Seq(period.to.show)
    )

  def viewModel(
    srn: Srn,
    index: Max9999999,
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
        QuestionField.input(Message("amountOfTheLoan.loanAmount.label")),
        QuestionField.input(Message("amountOfTheLoan.capRepaymentCY.label")),
        QuestionField.input(Message("amountOfTheLoan.amountOutstanding.label", period.to.show))
      ),
      routes.AmountOfTheLoanController.onSubmit(srn, index, mode)
    )
}
