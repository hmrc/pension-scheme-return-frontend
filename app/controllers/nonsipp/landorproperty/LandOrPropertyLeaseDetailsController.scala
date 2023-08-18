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

package controllers.nonsipp.landorproperty

import cats.implicits._
import config.Constants
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorproperty.LandOrPropertyLeaseDetailsController._
import forms.MultipleQuestionFormProvider
import forms.mappings.Mappings
import forms.mappings.errors._
import models.SchemeId.Srn
import models._
import navigation.Navigator
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPage, LandOrPropertyLeaseDetailsPage}
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService}
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel._
import viewmodels.models._
import views.html.MultipleQuestionView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val result = for {
        endDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourneyT
        address <- request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourneyT
        preparedForm = request.userAnswers.fillForm(LandOrPropertyLeaseDetailsPage(srn, index), form(endDate.to))
      } yield Ok(view(viewModel(srn, index, address.addressLine1, preparedForm, mode)))

      result.merge
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val result = for {
        endDate <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourneyT
        address <- request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourneyT
      } yield {
        form(endDate.to)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(viewModel(srn, index, address.addressLine1, formWithErrors, mode)))),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(LandOrPropertyLeaseDetailsPage(srn, index), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(LandOrPropertyLeaseDetailsPage(srn, index), mode, updatedAnswers))
          )
      }

      result.leftMap(_.pure[Future]).merge.flatten
  }
}

object LandOrPropertyLeaseDetailsController {
  private val field1Errors: InputFormErrors =
    InputFormErrors(
      "landOrPropertyLeaseDetails.field1.error.required",
      "landOrPropertyLeaseDetails.field1.error.invalid",
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
      List(DateFormErrors.failIfDateAfter(date, messages("landOrPropertyLeaseDetails.field3.error.future", date.show)))
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
    description = Some("landOrPropertyLeaseDetails.paragraph"),
    page = TripleQuestion(
      form,
      QuestionField.input("landOrPropertyLeaseDetails.field1.label"),
      QuestionField.money("landOrPropertyLeaseDetails.field2.label"),
      QuestionField.localDate("landOrPropertyLeaseDetails.field3.label")
    ),
    refresh = None,
    buttonText = "site.saveAndContinue",
    details = None,
    onSubmit = routes.LandOrPropertyLeaseDetailsController.onSubmit(srn, index, mode)
  )
}
