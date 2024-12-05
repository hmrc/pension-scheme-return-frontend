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

package controllers.nonsipp.moneyborrowed

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.moneyborrowed.ValueOfSchemeAssetsWhenMoneyBorrowedController._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import pages.nonsipp.moneyborrowed.{ValueOfSchemeAssetsWhenMoneyBorrowedPage, WhenBorrowedPage}
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import java.util.Locale
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Named}

class ValueOfSchemeAssetsWhenMoneyBorrowedController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = ValueOfSchemeAssetsWhenMoneyBorrowedController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(WhenBorrowedPage(srn, index)).sync { date =>
        val preparedForm = request.userAnswers.fillForm(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), form)

        Ok(view(preparedForm, viewModel(srn, index, request.schemeDetails.schemeName, formatDate(date), form, mode)))
      }
  }

  def formatDate(
    date: LocalDate
  ): String = {
    val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val outputFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
    val dateParsed = LocalDate.parse(date.toString, inputFormat)
    dateParsed.format(outputFormat)

  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.usingAnswer(WhenBorrowedPage(srn, index)).async { date =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, index, request.schemeDetails.schemeName, formatDate(date), form, mode)
                  )
                )
              )
            }
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.transformAndSet(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index), mode, updatedAnswers)
            )
        )
  }
}

object ValueOfSchemeAssetsWhenMoneyBorrowedController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "valueOfSchemeAssetsWhenMoneyBorrowed.error.required",
      "valueOfSchemeAssetsWhenMoneyBorrowed.error.invalid",
      (Constants.maxMoneyValue, "valueOfSchemeAssetsWhenMoneyBorrowed.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    date: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "valueOfSchemeAssetsWhenMoneyBorrowed.title",
      Message("valueOfSchemeAssetsWhenMoneyBorrowed.heading", schemeName, date),
      SingleQuestion(
        form,
        QuestionField.currency(Empty)
      ),
      routes.ValueOfSchemeAssetsWhenMoneyBorrowedController.onSubmit(srn, index, mode)
    )
}
