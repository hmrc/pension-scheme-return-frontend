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

package controllers.nonsipp.membertransferout

import cats.implicits.toShow
import config.Refined.{Max300, Max5}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.{DateRange, Mode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePage, TransfersOutCompletedPage, WhenWasTransferMadePage}
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DatePageViewModel, FormPageViewModel, SectionCompleted}
import views.html.DatePageView
import viewmodels.implicits._

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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
      (
        for {
          member <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
          date <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney
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
                      .set(TransfersOutCompletedPage(srn, index, secondaryIndex), SectionCompleted)
                  )
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(WhenWasTransferMadePage(srn, index, secondaryIndex), mode, updatedAnswers)
              )
          )
      }
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
      DatePageViewModel(),
      controllers.nonsipp.membertransferout.routes.WhenWasTransferMadeController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
