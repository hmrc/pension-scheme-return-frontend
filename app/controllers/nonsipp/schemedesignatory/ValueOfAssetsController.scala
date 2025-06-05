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

package controllers.nonsipp.schemedesignatory

import services.{SaveService, SchemeDateService}
import pages.nonsipp.schemedesignatory.{FinancialDetailsCheckYourAnswersPage, ValueOfAssetsPage}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import viewmodels.models.MultipleQuestionsViewModel.DoubleQuestion
import cats.implicits.toShow
import config.Constants.{maxAssetValue, minAssetValue}
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import cats.{Id, Monad}
import forms.mappings.errors.MoneyFormErrors
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, QuestionField}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class ValueOfAssetsController @Inject() (
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

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    usingSchemeDate[Id](srn) { period =>
      val form = ValueOfAssetsController.form(formProvider, period)
      val viewModel = ValueOfAssetsController.viewModel(
        srn,
        mode,
        period,
        form
      )

      Ok(view(request.userAnswers.fillForm(ValueOfAssetsPage(srn, mode), form), viewModel))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    usingSchemeDate(srn) { period =>
      val form = ValueOfAssetsController.form(formProvider, period)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val viewModel =
              ValueOfAssetsController.viewModel(srn, mode, period, form)

            Future.successful(BadRequest(view(formWithErrors, viewModel)))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.transformAndSet(ValueOfAssetsPage(srn, mode), value))
              _ <- saveService.save(updatedAnswers)
            } yield {
              mode match {
                case CheckMode =>
                  Redirect(navigator.nextPage(FinancialDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
                case NormalMode =>
                  Redirect(navigator.nextPage(ValueOfAssetsPage(srn, mode), mode, updatedAnswers))
                case ViewOnlyMode =>
                  Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
            }
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

object ValueOfAssetsController {

  def form(formProvider: MoneyFormProvider, period: DateRange): Form[(Money, Money)] =
    formProvider(
      MoneyFormErrors(
        "valueOfAssets.start.error.required",
        "valueOfAssets.start.error.nonNumeric",
        maxAssetValue -> "valueOfAssets.start.error.tooLarge",
        minAssetValue -> "valueOfAssets.start.error.tooSmall"
      ),
      MoneyFormErrors(
        "valueOfAssets.end.error.required",
        "valueOfAssets.end.error.nonNumeric",
        maxAssetValue -> "valueOfAssets.end.error.tooLarge",
        minAssetValue -> "valueOfAssets.end.error.tooSmall"
      ),
      Seq(period.from.show, period.to.show)
    )

  def viewModel(
    srn: Srn,
    mode: Mode,
    period: DateRange,
    form: Form[(Money, Money)]
  ): FormPageViewModel[DoubleQuestion[Money]] =
    FormPageViewModel(
      title = "valueOfAssets.title",
      heading = "valueOfAssets.heading",
      page = DoubleQuestion(
        form,
        QuestionField.currency(Message("valueOfAssets.start.label", period.from.show)),
        QuestionField.currency(Message("valueOfAssets.end.label", period.to.show))
      ),
      onSubmit = routes.ValueOfAssetsController.onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage("valueOfAssets.paragraph0") ++
        ParagraphMessage("valueOfAssets.paragraph1") ++
        ListMessage(
          ListType.Bullet,
          "valueOfAssets.bullet1",
          "valueOfAssets.bullet2"
        )
    )
}
