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

package controllers.nonsipp.receivetransfer

import services.{SaveService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc._
import controllers.actions._
import navigation.Navigator
import forms.DatePageFormProvider
import cats.{Id, Monad}
import play.api.i18n.{Messages, MessagesApi}
import forms.mappings.errors.DateFormErrors
import pages.nonsipp.memberdetails.MemberDetailsPage
import controllers.nonsipp.receivetransfer.WhenWasTransferReceivedController._
import config.RefinedTypes._
import controllers.PSRController
import views.html.DatePageView
import models.SchemeId.Srn
import cats.implicits.toShow
import pages.nonsipp.receivetransfer.{TransferringSchemeNamePage, WhenWasTransferReceivedPage}
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage.Message
import viewmodels.models._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class WhenWasTransferReceivedController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: DatePageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  schemeDateService: SchemeDateService,
  view: DatePageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form(date: DateRange)(implicit messages: Messages) =
    WhenWasTransferReceivedController.form(formProvider, date)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      usingSchemeDate[Id](srn) { date =>
        (
          for {
            member <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
            schemeName <- request.userAnswers
              .get(TransferringSchemeNamePage(srn, index, secondaryIndex))
              .getOrRecoverJourney
          } yield {
            val preparedForm = request.userAnswers
              .get(WhenWasTransferReceivedPage(srn, index, secondaryIndex))
              .fold(form(date))(form(date).fill)
            Ok(
              view(
                preparedForm,
                viewModel(srn, index, secondaryIndex, schemeName, member.fullName, mode)
              )
            )
          }
        ).merge
      }
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      usingSchemeDate(srn) { date =>
        form(date)
          .bindFromRequest()
          .fold(
            formWithErrors => {
              Future.successful(
                (
                  for {
                    member <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
                    schemeName <- request.userAnswers
                      .get(TransferringSchemeNamePage(srn, index, secondaryIndex))
                      .getOrRecoverJourney
                  } yield {
                    BadRequest(
                      view(
                        formWithErrors,
                        viewModel(srn, index, secondaryIndex, schemeName, member.fullName, mode)
                      )
                    )
                  }
                ).merge
              )
            },
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(WhenWasTransferReceivedPage(srn, index, secondaryIndex), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(WhenWasTransferReceivedPage(srn, index, secondaryIndex), mode, updatedAnswers)
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

object WhenWasTransferReceivedController {
  def form(formProvider: DatePageFormProvider, date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    formProvider(
      DateFormErrors(
        required = "transferReceived.error.required.all",
        requiredDay = "transferReceived.error.required.day",
        requiredMonth = "transferReceived.error.required.month",
        requiredYear = "transferReceived.error.required.year",
        requiredTwo = "transferReceived.error.required.two",
        invalidDate = "transferReceived.error.invalid.date",
        invalidCharacters = "transferReceived.error.invalid.chars",
        validators = List(
          DateFormErrors
            .failIfDateAfter(date.to, messages("transferReceived.error.date.after", date.to.show)),
          DateFormErrors
            .failIfDateBefore(
              date.from,
              messages("transferReceived.error.date.before", date.from.show)
            )
        )
      )
    )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max5,
    schemeName: String,
    memberName: String,
    mode: Mode
  ): FormPageViewModel[DatePageViewModel] =
    FormPageViewModel(
      "transferReceived.title",
      Message("transferReceived.heading", schemeName, memberName),
      DatePageViewModel(
        None,
        Message("transferReceived.heading", schemeName, memberName),
        Some("transferReceived.hint")
      ),
      controllers.nonsipp.receivetransfer.routes.WhenWasTransferReceivedController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
