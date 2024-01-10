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

package controllers.nonsipp.membersurrenderedbenefits

import config.Constants.{maxInputAmount, minInputAmount}
import config.Refined.Max300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountController._
import forms.mappings.errors.{MoneyFormErrorProvider, MoneyFormErrors}
import models.{Mode, Money}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountPage
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

class SurrenderedBenefitsAmountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormErrorProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = SurrenderedBenefitsAmountController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
        val preparedForm = request.userAnswers.fillForm(SurrenderedBenefitsAmountPage(srn, memberIndex), form)

        Ok(view(viewModel(srn, memberName.fullName, memberIndex, preparedForm, mode)))
      }
    }

  def onSubmit(srn: Srn, memberIndex: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              Future.successful(
                BadRequest(view(viewModel(srn, memberName.fullName, memberIndex, formWithErrors, mode)))
              )
            },
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(SurrenderedBenefitsAmountPage(srn, memberIndex), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(SurrenderedBenefitsAmountPage(srn, memberIndex), mode, updatedAnswers)
              )
          )
      }
    }
}

object SurrenderedBenefitsAmountController {
  def form(formProvider: MoneyFormErrorProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      requiredKey = "surrenderedBenefits.amount.error.required",
      nonNumericKey = "surrenderedBenefits.amount.error.invalid",
      min = (minInputAmount, "surrenderedBenefits.amount.error.tooSmall"),
      max = (maxInputAmount, "surrenderedBenefits.amount.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    memberName: String,
    memberIndex: Max300,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      title = "surrenderedBenefits.amount.title",
      heading = Message("surrenderedBenefits.amount.heading", memberName),
      page = SingleQuestion(form, QuestionField.input(Empty)),
      onSubmit = routes.SurrenderedBenefitsAmountController.onSubmit(srn, memberIndex, mode)
    )
}
