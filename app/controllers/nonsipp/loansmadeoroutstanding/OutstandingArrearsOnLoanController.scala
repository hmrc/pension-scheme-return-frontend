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
import models.ConditionalYesNo._
import play.api.mvc._
import forms.mappings.Mappings
import controllers.nonsipp.loansmadeoroutstanding.OutstandingArrearsOnLoanController._
import config.RefinedTypes.Max5000
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import viewmodels.models._
import forms.mappings.errors.MoneyFormErrors
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import utils.nonsipp.PrePopulationUtils.isPrePopulation
import config.Constants.maxCurrencyValue
import utils.DateTimeUtils.localDateShow
import models._
import pages.nonsipp.loansmadeoroutstanding.{ArrearsPrevYears, OutstandingArrearsOnLoanPage}
import cats.{Id, Monad}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OutstandingArrearsOnLoanController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConditionalYesNoPageView,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      usingSchemeDate[Id](srn) { period =>
        val storedYesNoAnswer = request.userAnswers.get(ArrearsPrevYears(srn, index))
        val form = OutstandingArrearsOnLoanController.form(formProvider, period)

        val preparedForm = if (isPrePopulation && storedYesNoAnswer.isEmpty) {
          form
        } else {
          request.userAnswers.fillForm(OutstandingArrearsOnLoanPage(srn, index), form)
        }

        Ok(view(preparedForm, viewModel(srn, index, mode, period)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      usingSchemeDate(srn) { period =>
        val form = OutstandingArrearsOnLoanController.form(formProvider, period)
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode, period)))),
            value =>
              for {
                yesNoAnswer <- Future
                  .fromTry(request.userAnswers.set(ArrearsPrevYears(srn, index), if (value.isRight) true else false))
                updatedAnswers <- Future
                  .fromTry(yesNoAnswer.set(OutstandingArrearsOnLoanPage(srn, index), ConditionalYesNo(value)))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(OutstandingArrearsOnLoanPage(srn, index), mode, updatedAnswers))
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

object OutstandingArrearsOnLoanController {

  def form(formProvider: YesNoPageFormProvider, period: DateRange): Form[Either[Unit, Money]] =
    formProvider.conditionalYes[Money](
      "outstandingArrearsOnLoan.error.required",
      mappingYes = Mappings.money(
        MoneyFormErrors(
          "outstandingArrearsOnLoan.yes.error.required",
          "outstandingArrearsOnLoan.yes.error.nonNumeric",
          (maxCurrencyValue, "outstandingArrearsOnLoan.yes.error.max")
        )
      ),
      period.to.show
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    period: DateRange
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "outstandingArrearsOnLoan.title",
      ("outstandingArrearsOnLoan.heading", period.to.show),
      page = ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional("outstandingArrearsOnLoan.yes.conditional", FieldType.Currency),
        no = YesNoViewModel.Unconditional
      ),
      routes.OutstandingArrearsOnLoanController.onSubmit(srn, index, mode)
    )
}
