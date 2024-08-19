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

package controllers.nonsipp.membertransferout

import services.{SaveService, SchemeDateService}
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.{Max300, Max5}
import controllers.PSRController
import cats.implicits.toShow
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.DatePageFormProvider
import cats.{Id, Monad}
import forms.mappings.errors.DateFormErrors
import views.html.DatePageView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Mode}
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePage, TransfersOutSectionCompleted, WhenWasTransferMadePage}
import play.api.i18n.{Messages, MessagesApi}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel, SectionCompleted}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class WhenWasTransferMadeController @Inject()(
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
    WhenWasTransferMadeController.form(formProvider, date)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      usingSchemeDate[Id](srn) { date =>
        (
          for {
            member <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
            schemeName <- request.userAnswers
              .get(ReceivingSchemeNamePage(srn, index, secondaryIndex))
              .getOrRecoverJourney
          } yield {
            val preparedForm = request.userAnswers
              .get(WhenWasTransferMadePage(srn, index, secondaryIndex))
              .fold(form(date))(form(date).fill)
            Ok(
              view(
                preparedForm,
                WhenWasTransferMadeController.viewModel(srn, index, secondaryIndex, schemeName, member.fullName, mode)
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
                      .get(ReceivingSchemeNamePage(srn, index, secondaryIndex))
                      .getOrRecoverJourney
                  } yield {
                    BadRequest(
                      view(
                        formWithErrors,
                        WhenWasTransferMadeController
                          .viewModel(srn, index, secondaryIndex, schemeName, member.fullName, mode)
                      )
                    )
                  }
                ).merge
              )
            },
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .set(WhenWasTransferMadePage(srn, index, secondaryIndex), value)
                      .set(TransfersOutSectionCompleted(srn, index, secondaryIndex), SectionCompleted)
                  )
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(WhenWasTransferMadePage(srn, index, secondaryIndex), mode, updatedAnswers)
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

object WhenWasTransferMadeController {
  def form(formProvider: DatePageFormProvider, date: DateRange)(implicit messages: Messages): Form[LocalDate] =
    formProvider(
      DateFormErrors(
        required = "transferOut.transferMade.error.required.all",
        requiredDay = "transferOut.transferMade.error.required.day",
        requiredMonth = "transferOut.transferMade.error.required.month",
        requiredYear = "transferOut.transferMade.error.required.year",
        requiredTwo = "transferOut.transferMade.error.required.two",
        invalidDate = "transferOut.transferMade.error.invalid.date",
        invalidCharacters = "transferOut.transferMade.error.invalid.chars",
        validators = List(
          DateFormErrors
            .failIfDateAfter(date.to, messages("transferOut.transferMade.error.date.after", date.to.show)),
          DateFormErrors
            .failIfDateBefore(
              date.from,
              messages("transferOut.transferMade.error.date.before", date.from.show)
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
      "transferOut.transferMade.title",
      Message("transferOut.transferMade.heading", schemeName, memberName),
      DatePageViewModel(
        None,
        Message("transferOut.transferMade.heading", schemeName, memberName),
        Some("transferOut.transferMade.hint")
      ),
      controllers.nonsipp.membertransferout.routes.WhenWasTransferMadeController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
