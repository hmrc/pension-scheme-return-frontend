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

package controllers.nonsipp.landorproperty

import services.{SaveService, SchemeDateService}
import controllers.nonsipp.landorproperty.LandOrPropertyLeaseDetailsController._
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import viewmodels.models.MultipleQuestionsViewModel._
import config.Constants
import cats.implicits._
import controllers.actions._
import navigation.Navigator
import forms.MultipleQuestionFormProvider
import play.api.i18n.{Messages, MessagesApi}
import play.api.data.Form
import forms.mappings.errors._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, IntOpts}
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandOrPropertyLeaseDetailsPage}
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.{Inject, Named}

class LandOrPropertyLeaseDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  saveService: SaveService,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val result = for {
        endDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourneyT
        address <- request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index.refined)).getOrRecoverJourneyT
        preparedForm = request.userAnswers
          .fillForm(LandOrPropertyLeaseDetailsPage(srn, index.refined), form(endDate.to))
      } yield Ok(
        view(
          preparedForm,
          viewModel(srn, index.refined, address.addressLine1, form(endDate.to), mode)
        )
      )

      result.merge
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val result = for {
        endDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourneyT
        address <- request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index.refined)).getOrRecoverJourneyT
      } yield {
        form(endDate.to)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, index.refined, address.addressLine1, form(endDate.to), mode)
                  )
                )
              ),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(LandOrPropertyLeaseDetailsPage(srn, index.refined), value))
                nextPage = navigator.nextPage(LandOrPropertyLeaseDetailsPage(srn, index.refined), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
          )
      }

      result.leftMap(_.pure[Future]).merge.flatten
  }
}

object LandOrPropertyLeaseDetailsController {
  private val field1Errors: InputFormErrors =
    InputFormErrors.textArea(
      "landOrPropertyLeaseDetails.field1.error.required",
      "error.textarea.invalid",
      "landOrPropertyLeaseDetails.field1.error.length"
    )

  private val field2Errors: MoneyFormErrors =
    MoneyFormErrors(
      "landOrPropertyLeaseDetails.field2.error.required",
      "landOrPropertyLeaseDetails.field2.error.invalid",
      (Constants.maxMoneyValue, "landOrPropertyLeaseDetails.field2.error.max")
    )

  private def field3Errors(date: LocalDate)(implicit messages: Messages): DateFormErrors =
    DateFormErrors(
      "landOrPropertyLeaseDetails.field3.error.required.all",
      "landOrPropertyLeaseDetails.field3.error.required.day",
      "landOrPropertyLeaseDetails.field3.error.required.month",
      "landOrPropertyLeaseDetails.field3.error.required.year",
      "landOrPropertyLeaseDetails.field3.error.required.two",
      "landOrPropertyLeaseDetails.field3.error.invalid.date",
      "landOrPropertyLeaseDetails.field3.error.invalid.characters",
      List(
        DateFormErrors.failIfDateAfter(date, messages("landOrPropertyLeaseDetails.field3.error.future", date.show)),
        DateFormErrors
          .failIfDateBefore(
            Constants.earliestDate,
            messages(
              "landOrPropertyLeaseDetails.field3.error.after",
              Constants.earliestDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            )
          )
      )
    )

  def form(accountingPeriodEndDate: LocalDate)(implicit messages: Messages): Form[(String, Money, LocalDate)] =
    MultipleQuestionFormProvider(
      Mappings.input(field1Errors),
      Mappings.money(field2Errors),
      Mappings.localDate(field3Errors(accountingPeriodEndDate))
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    addressLine1: String,
    form: Form[(String, Money, LocalDate)],
    mode: Mode
  ): FormPageViewModel[TripleQuestion[String, Money, LocalDate]] = FormPageViewModel(
    title = "landOrPropertyLeaseDetails.title",
    heading = Message("landOrPropertyLeaseDetails.heading", addressLine1),
    description = None,
    page = TripleQuestion(
      form,
      QuestionField
        .input("landOrPropertyLeaseDetails.field1.label", Some("landOrPropertyLeaseDetails.field1.label.hint")),
      QuestionField
        .currency("landOrPropertyLeaseDetails.field2.label", Some("landOrPropertyLeaseDetails.field2.label.hint")),
      QuestionField
        .date("landOrPropertyLeaseDetails.field3.label", Some("landOrPropertyLeaseDetails.field3.label.hint"))
    ),
    refresh = None,
    buttonText = "site.saveAndContinue",
    details = None,
    onSubmit = routes.LandOrPropertyLeaseDetailsController.onSubmit(srn, index, mode)
  )
}
