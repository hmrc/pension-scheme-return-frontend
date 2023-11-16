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

package controllers.nonsipp.membercontributions

import config.Constants
import config.Refined.{Max300, Max50}
import controllers.nonsipp.membercontributions.TotalMemberContributionController._
import controllers.PSRController
import controllers.actions._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.membercontributions.TotalMemberContributionPage
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

class TotalMemberContributionController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalMemberContributionController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(TotalMemberContributionPage(srn, index, secondaryIndex)).fold(form)(form.fill)
      Ok(view(viewModel(srn, index, secondaryIndex, preparedForm, mode)))
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(BadRequest(view(viewModel(srn, index, secondaryIndex, formWithErrors, mode))))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.transformAndSet(TotalMemberContributionPage(srn, index, secondaryIndex), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(TotalMemberContributionPage(srn, index, secondaryIndex), mode, updatedAnswers)
            )
        )
    }
}

object TotalMemberContributionController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalMemberContribution.error.required",
      "totalMemberContribution.error.invalid",
      (Constants.maxMoneyValue, "totalMemberContribution.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max50,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "totalMemberContribution.title",
      Message("totalMemberContribution.heading"),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("totalMemberContribution.hint"))
      ),
      controllers.nonsipp.membercontributions.routes.TotalMemberContributionController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
