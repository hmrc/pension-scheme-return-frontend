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

package controllers.nonsipp.memberpensionpayments

import config.Constants
import config.Refined.Max300
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsController._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MoneyView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TotalAmountPensionPaymentsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalAmountPensionPaymentsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val memberNames = request.userAnswers.membersDetails(srn)
      val preparedForm =
        request.userAnswers.get(TotalAmountPensionPaymentsPage(srn, index)).fold(form)(form.fill)
      Ok(view(viewModel(srn, index, memberNames(index.value - 1).fullName, preparedForm, mode)))
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val memberNames = request.userAnswers.membersDetails(srn)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              BadRequest(
                view(viewModel(srn, index, memberNames(index.value - 1).fullName, formWithErrors, mode))
              )
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.transformAndSet(TotalAmountPensionPaymentsPage(srn, index), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(TotalAmountPensionPaymentsPage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object TotalAmountPensionPaymentsController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalAmountPensionPayments.error.required",
      "totalAmountPensionPayments.error.invalid",
      (Constants.maxMoneyValue, "totalAmountPensionPayments.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    memberName: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "totalAmountPensionPayments.title",
      Message("totalAmountPensionPayments.heading", memberName),
      SingleQuestion(
        form,
        QuestionField.input(Empty)
      ),
      controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
        .onSubmit(srn, index, mode)
    )
}
