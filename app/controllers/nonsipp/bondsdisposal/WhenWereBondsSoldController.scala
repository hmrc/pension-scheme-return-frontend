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

package controllers.nonsipp.bondsdisposal

import play.api.mvc._
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.DatePageFormProvider
import cats.{Id, Monad}
import forms.mappings.errors.DateFormErrors
import services.{SaveService, SchemeDateService}
import controllers.nonsipp.bondsdisposal.WhenWereBondsSoldController._
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import pages.nonsipp.bondsdisposal.WhenWereBondsSoldPage
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class WhenWereBondsSoldController @Inject()(
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
    WhenWereBondsSoldController.form(formProvider, date)

  def onPageLoad(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      usingSchemeDate[Id](srn) { date =>
        val preparedForm = request.userAnswers
          .get(WhenWereBondsSoldPage(srn, bondIndex, disposalIndex))
          .fold(form(date))(form(date).fill)
        Ok(
          view(
            preparedForm,
            viewModel(srn, bondIndex, disposalIndex, mode)
          )
        )
      }
    }

  def onSubmit(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
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
                    viewModel(srn, bondIndex, disposalIndex, mode)
                  )
                )
              ),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(WhenWereBondsSoldPage(srn, bondIndex, disposalIndex), value))
                nextPage = navigator
                  .nextPage(WhenWereBondsSoldPage(srn, bondIndex, disposalIndex), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, bondIndex, disposalIndex, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
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

object WhenWereBondsSoldController {
  def form(formProvider: DatePageFormProvider, date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    formProvider(
      DateFormErrors(
        required = "bondsDisposal.whenWereBondsSold.error.required.all",
        requiredDay = "bondsDisposal.whenWereBondsSold.error.required.day",
        requiredMonth = "bondsDisposal.whenWereBondsSold.error.required.month",
        requiredYear = "bondsDisposal.whenWereBondsSold.error.required.year",
        requiredTwo = "bondsDisposal.whenWereBondsSold.error.required.two",
        invalidDate = "bondsDisposal.whenWereBondsSold.error.invalid.date",
        invalidCharacters = "bondsDisposal.whenWereBondsSold.error.invalid.characters",
        validators = List(
          DateFormErrors
            .failIfDateAfter(date.to, messages("bondsDisposal.whenWereBondsSold.error.date.after", date.to.show)),
          DateFormErrors
            .failIfDateBefore(
              date.from,
              messages("bondsDisposal.whenWereBondsSold.error.date.before", date.from.show)
            )
        )
      )
    )

  def viewModel(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      title = Message("bondsDisposal.whenWereBondsSold.title"),
      heading = Message("bondsDisposal.whenWereBondsSold.heading"),
      page = DatePageViewModel(
        None,
        Message("bondsDisposal.whenWereBondsSold.heading"),
        Some(Message("bondsDisposal.whenWereBondsSold.hint"))
      ),
      onSubmit = routes.WhenWereBondsSoldController.onSubmit(srn, bondIndex, disposalIndex, mode)
    )
}
