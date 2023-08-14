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

package controllers.nonsipp.loansmadeoroutstanding

import cats.implicits.toShow
import config.Constants.{maxAssetValue, maxLoanPeriod}
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import forms.MultipleQuestionFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.{DateFormErrors, IntFormErrors, MoneyFormErrors}
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.DatePeriodLoanPage
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.TripleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MultipleQuestionView
import controllers.nonsipp.loansmadeoroutstanding.DatePeriodLoanController._

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DatePeriodLoanController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        Ok(
          view(
            viewModel(
              srn,
              index,
              request.schemeDetails.schemeName,
              mode,
              request.userAnswers.fillForm(DatePeriodLoanPage(srn, index), form(date.to))
            )
          )
        )
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney { date =>
        form(date.to)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful {
                BadRequest(view(viewModel(srn, index, request.schemeDetails.schemeName, mode, formWithErrors)))
              },
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.transformAndSet(DatePeriodLoanPage(srn, index), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(DatePeriodLoanPage(srn, index), mode, updatedAnswers))
          )
      }
  }
}

object DatePeriodLoanController {

  private def field1Errors(date: LocalDate)(implicit messages: Messages): DateFormErrors =
    DateFormErrors(
      "datePeriodLoan.field1.error.required.all",
      "datePeriodLoan.field1.error.required.day",
      "datePeriodLoan.field1.error.required.month",
      "datePeriodLoan.field1.error.required.year",
      "datePeriodLoan.field1.error.required.two",
      "datePeriodLoan.field1.error.invalid.date",
      "datePeriodLoan.field1.error.invalid.characters",
      List(DateFormErrors.failIfDateAfter(date, messages("datePeriodLoan.field1.error.future", date.show)))
    )

  private val field2Errors: MoneyFormErrors =
    MoneyFormErrors(
      "datePeriodLoan.field2.error.required",
      "datePeriodLoan.field2.error.invalid",
      (maxAssetValue, "datePeriodLoan.field2.error.max")
    )

  private val field3Errors: IntFormErrors =
    IntFormErrors(
      "datePeriodLoan.field3.error.required",
      "datePeriodLoan.field3.error.invalid",
      (maxLoanPeriod, "datePeriodLoan.field3.error.max")
    )

  def form(endDate: LocalDate)(implicit messages: Messages): Form[(LocalDate, Money, Int)] =
    MultipleQuestionFormProvider(
      Mappings.localDate(field1Errors(endDate)),
      Mappings.money(field2Errors),
      Mappings.int(field3Errors)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    mode: Mode,
    form: Form[(LocalDate, Money, Int)]
  ): FormPageViewModel[TripleQuestion[LocalDate, Money, Int]] = FormPageViewModel(
    "datePeriodLoan.title",
    "datePeriodLoan.heading",
    TripleQuestion(
      form,
      QuestionField.date("datePeriodLoan.field1"),
      QuestionField.currency(Message("datePeriodLoan.field2", schemeName)),
      QuestionField.input("datePeriodLoan.field3", hint = Some("datePeriodLoan.field3.hint"))
    ),
    details = None,
    routes.DatePeriodLoanController.onSubmit(srn, index, mode)
  )
}
