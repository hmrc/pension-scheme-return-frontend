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

package controllers.nonsipp.memberpensionpayments

import controllers.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsController._
import services.SaveService
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc._
import _root_.config.RefinedTypes._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import controllers.PSRController
import utils.IntUtils.{toInt, toRefined300}
import _root_.config.Constants
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import views.html.MoneyView
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import controllers.actions._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalAmountPensionPaymentsController @Inject() (
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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        val preparedForm =
          request.userAnswers
            .get(TotalAmountPensionPaymentsPage(srn, index))
            .fold(form)(value => if (value.isZero) form else form.fill(value))

        Ok(view(preparedForm, viewModel(srn, index, memberName.fullName, form, mode)))
      }
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, index, memberName.fullName, form, mode)
                  )
                )
              ),
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
}

object TotalAmountPensionPaymentsController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalAmountPensionPayments.error.required",
      "totalAmountPensionPayments.error.invalid",
      (Constants.maxMoneyValue, "totalAmountPensionPayments.error.tooLarge"),
      (Constants.minPosMoneyValue, "totalAmountPensionPayments.error.tooSmall")
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
        QuestionField.currency(Empty)
      ),
      controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
        .onSubmit(srn, index, mode)
    )
}
