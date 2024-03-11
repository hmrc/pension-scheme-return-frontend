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

package controllers.nonsipp.totalvaluequotedshares

import cats.implicits.toShow
import cats.{Id, Monad}
import config.Constants.maxMoneyValue
import controllers.actions._
import controllers.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesController._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Money, NormalMode}
import navigation.Navigator
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MoneyView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TotalValueQuotedSharesController @Inject()(
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
      val preparedForm = request.userAnswers.fillForm(TotalValueQuotedSharesPage(srn), form)
      Ok(view(viewModel(srn, request.schemeDetails.schemeName, preparedForm, period)))
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
                .viewModel(srn, request.schemeDetails.schemeName, formWithErrors, period)

            Future.successful(BadRequest(view(viewModel)))
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
      ("totalValueQuotedShares.error.required"),
      "totalValueQuotedShares.error.invalid",
      (maxMoneyValue, "totalValueQuotedShares.error.tooLarge")
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
        QuestionField.input(Empty, Some("totalValueQuotedShares.hint"))
      ),
      routes.TotalValueQuotedSharesController.onSubmit(srn)
    )
}
