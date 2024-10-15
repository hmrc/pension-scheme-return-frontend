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

import services.SaveService
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants.{maxSurrenderedBenefitAmount, minPosMoneyValue}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.MoneyFormProvider
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountPage
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountController._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SurrenderedBenefitsAmountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = SurrenderedBenefitsAmountController.form(formProvider)

  def onPageLoad(srn: Srn, memberIndex: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, memberIndex)).getOrRecoverJourney { memberName =>
        val preparedForm = request.userAnswers.fillForm(SurrenderedBenefitsAmountPage(srn, memberIndex), form)

        Ok(view(preparedForm, viewModel(srn, memberName.fullName, memberIndex, form, mode)))
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
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, memberName.fullName, memberIndex, form, mode)
                  )
                )
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
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      requiredKey = "surrenderedBenefits.amount.error.required",
      nonNumericKey = "surrenderedBenefits.amount.error.invalid",
      min = (minPosMoneyValue, "surrenderedBenefits.amount.error.tooSmall"),
      max = (maxSurrenderedBenefitAmount, "surrenderedBenefits.amount.error.tooLarge")
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
