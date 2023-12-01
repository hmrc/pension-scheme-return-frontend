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

package controllers.nonsipp.receivetransfer

import cats.implicits.toShow
import config.Refined._
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.receivetransfer.WhenWasTransferReceivedController._
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.SchemeId.Srn
import models._
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.receivetransfer.{TransferringSchemeNamePage, WhenWasTransferReceivedPage}
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.DatePageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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
      (
        for {
          member <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
          date <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney
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

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max5, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
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
      DatePageViewModel(),
      controllers.nonsipp.receivetransfer.routes.WhenWasTransferReceivedController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
