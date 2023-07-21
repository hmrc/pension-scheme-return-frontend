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

package controllers.nonsipp.schemedesignatory

import cats.implicits.toShow
import cats.{Id, Monad}
import com.google.inject.Inject
import config.Constants.maxCashInBank
import controllers.actions._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode, Money}
import navigation.Navigator
import pages.nonsipp.schemeDesignatory.HowMuchCashPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDateService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.DoubleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MoneyView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class HowMuchCashController @Inject()(
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

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    usingSchemeDate[Id](srn) { period =>
      val form = HowMuchCashController.form(formProvider, request.schemeDetails.schemeName, period)
      val viewModel = HowMuchCashController.viewModel(
        srn,
        mode,
        request.schemeDetails.schemeName,
        period,
        request.userAnswers.fillForm(HowMuchCashPage(srn), form)
      )

      Ok(view(viewModel))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    usingSchemeDate(srn) { period =>
      val form = HowMuchCashController.form(formProvider, request.schemeDetails.schemeName, period)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel =
              HowMuchCashController.viewModel(srn, mode, request.schemeDetails.schemeName, period, formWithErrors)

            Future.successful(BadRequest(view(viewModel)))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.transformAndSet(HowMuchCashPage(srn), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(HowMuchCashPage(srn), mode, updatedAnswers))
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

object HowMuchCashController {

  def form(formProvider: MoneyFormProvider, schemeName: String, period: DateRange): Form[(Money, Money)] =
    formProvider(
      MoneyFormErrors(
        "howMuchCash.start.error.required",
        "howMuchCash.start.error.nonNumeric",
        maxCashInBank -> "howMuchCash.start.error.tooLarge"
      ),
      MoneyFormErrors(
        "howMuchCash.end.error.required",
        "howMuchCash.end.error.nonNumeric",
        maxCashInBank -> "howMuchCash.end.error.tooLarge"
      ),
      Seq(period.from.show, period.to.show)
    )

  def viewModel(
    srn: Srn,
    mode: Mode,
    schemeName: String,
    period: DateRange,
    form: Form[(Money, Money)]
  ): FormPageViewModel[DoubleQuestion[Money]] =
    FormPageViewModel(
      "howMuchCash.title",
      Message("howMuchCash.heading", schemeName),
      page = DoubleQuestion(
        form,
        QuestionField.input(Message("howMuchCash.start.label", period.from.show)),
        QuestionField.input(Message("howMuchCash.end.label", period.to.show))
      ),
      routes.HowMuchCashController.onSubmit(srn, mode)
    ).withDescription(ParagraphMessage("howMuchCash.paragraph"))
}
