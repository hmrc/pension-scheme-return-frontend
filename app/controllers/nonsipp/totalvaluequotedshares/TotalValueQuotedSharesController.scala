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

package controllers.nonsipp.totalvaluequotedshares

import services.{SaveService, SchemeDateService}
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import viewmodels.implicits._
import play.api.mvc._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import cats.implicits.toShow
import config.Constants.maxMoneyValue
import controllers.actions._
import controllers.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesController._
import navigation.Navigator
import forms.MoneyFormProvider
import cats.{Id, Monad}
import forms.mappings.errors.MoneyFormErrors
import views.html.MoneyView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Money, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalValueQuotedSharesController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    usingSchemeDate[Id](srn) { period =>
      val form = TotalValueQuotedSharesController.form(formProvider, period)
      val filledForm = request.userAnswers.fillForm(TotalValueQuotedSharesPage(srn), form)

      val containsZeroMoney = request.userAnswers.get(TotalValueQuotedSharesPage(srn)).contains(Money(0.00))
      val preparedForm = if (containsZeroMoney) form else filledForm

      Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, form, period)))
    }
  }

  def onSubmit(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    usingSchemeDate(srn) { period =>
      val form = TotalValueQuotedSharesController.form(formProvider, period)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel =
              TotalValueQuotedSharesController
                .viewModel(srn, request.schemeDetails.schemeName, form, period)

            Future.successful(BadRequest(view(formWithErrors, viewModel)))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.transformAndSet(TotalValueQuotedSharesPage(srn), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(TotalValueQuotedSharesPage(srn), NormalMode, updatedAnswers))
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

object TotalValueQuotedSharesController {
  def form(formProvider: MoneyFormProvider, period: DateRange): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalValueQuotedShares.error.required",
      "totalValueQuotedShares.error.invalid",
      (maxMoneyValue, "totalValueQuotedShares.error.tooLarge"),
      (Constants.minPosMoneyValue, "totalValueQuotedShares.error.tooSmall")
    ),
    Seq(period.to.show)
  )

  def viewModel(
    srn: Srn,
    schemeName: String,
    form: Form[Money],
    period: DateRange
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      Message("totalValueQuotedShares.title", period.to.show),
      Message("totalValueQuotedShares.heading", schemeName, period.to.show),
      SingleQuestion(
        form,
        QuestionField.currency(Empty)
      ),
      routes.TotalValueQuotedSharesController.onSubmit(srn)
    )
}
