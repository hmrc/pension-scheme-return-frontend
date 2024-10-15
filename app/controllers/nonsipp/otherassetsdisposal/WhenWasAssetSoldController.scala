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

package controllers.nonsipp.otherassetsdisposal

import services.{SaveService, SchemeDateService}
import pages.nonsipp.otherassetsdisposal.WhenWasAssetSoldPage
import play.api.mvc._
import controllers.nonsipp.otherassetsdisposal.WhenWasAssetSoldController._
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.DatePageFormProvider
import cats.{Id, Monad}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import forms.mappings.errors.DateFormErrors
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class WhenWasAssetSoldController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: DatePageFormProvider,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: DatePageView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form(date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    WhenWasAssetSoldController.form(formProvider, date)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      usingSchemeDate[Id](srn) { date =>
        val preparedForm = request.userAnswers
          .get(WhenWasAssetSoldPage(srn, assetIndex, disposalIndex))
          .fold(form(date))(form(date).fill)
        Ok(
          view(
            preparedForm,
            viewModel(srn, assetIndex, disposalIndex, mode)
          )
        )
      }
    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      usingSchemeDate(srn) { date =>
        form(date)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, assetIndex, disposalIndex, mode)
                  )
                )
              ),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(WhenWasAssetSoldPage(srn, assetIndex, disposalIndex), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(WhenWasAssetSoldPage(srn, assetIndex, disposalIndex), mode, updatedAnswers)
              )
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

object WhenWasAssetSoldController {
  def form(formProvider: DatePageFormProvider, date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    formProvider(
      DateFormErrors(
        required = "whenWasAssetSold.error.required.all",
        requiredDay = "whenWasAssetSold.error.required.day",
        requiredMonth = "whenWasAssetSold.error.required.month",
        requiredYear = "whenWasAssetSold.error.required.year",
        requiredTwo = "whenWasAssetSold.error.required.two",
        invalidDate = "whenWasAssetSold.error.invalid.date",
        invalidCharacters = "whenWasAssetSold.error.invalid.characters",
        validators = List(
          DateFormErrors
            .failIfDateAfter(date.to, messages("whenWasAssetSold.error.date.after", date.to.show)),
          DateFormErrors
            .failIfDateBefore(
              date.from,
              messages("whenWasAssetSold.error.date.before", date.from.show)
            )
        )
      )
    )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      title = Message("whenWasAssetSold.title"),
      heading = Message("whenWasAssetSold.heading"),
      page = DatePageViewModel(
        None,
        Message("whenWasAssetSold.heading"),
        Some(Message("whenWasAssetSold.hint"))
      ),
      onSubmit = routes.WhenWasAssetSoldController.onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
