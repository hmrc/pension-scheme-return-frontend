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

package controllers.nonsipp.employercontributions

import pages.nonsipp.employercontributions.{EmployerNamePage, TotalEmployerContributionPage}
import controllers.nonsipp.employercontributions.TotalEmployerContributionController._
import config.Refined._
import controllers.actions._
import models._
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.models._
import viewmodels.implicits._
import services.SaveService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import config.Constants
import forms.mappings.errors.MoneyFormErrorProvider
import forms.mappings.errors.MoneyFormErrorValue
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.DisplayMessage._
import play.api.i18n.{I18nSupport, MessagesApi}
import views.html.MoneyView

class TotalEmployerContributionController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormErrorProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalEmployerContributionController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(TotalEmployerContributionPage(srn, index, secondaryIndex)).fold(form)(form.fill)
      (
        for {
          memberName <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
          employerName <- request.userAnswers.get(EmployerNamePage(srn, index, secondaryIndex)).getOrRecoverJourney
        } yield Ok(view(viewModel(srn, employerName, memberName.fullName, index, secondaryIndex, preparedForm, mode)))
      ).merge
    }

  def onSubmit(srn: Srn, index: Max300, secondaryIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            (
              for {
                memberName <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourneyT
                employerName <- request.userAnswers
                  .get(EmployerNamePage(srn, index, secondaryIndex))
                  .getOrRecoverJourneyT
              } yield BadRequest(
                view(viewModel(srn, employerName, memberName.fullName, index, secondaryIndex, formWithErrors, mode))
              )
            ).merge
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(TotalEmployerContributionPage(srn, index, secondaryIndex), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(TotalEmployerContributionPage(srn, index, secondaryIndex), mode, updatedAnswers)
            )
        )
    }
}

object TotalEmployerContributionController {
  def form(formProvider: MoneyFormErrorProvider): Form[Money] = formProvider(
    MoneyFormErrorValue(
      requiredKey = "totalEmployerContribution.error.required",
      nonNumericKey = "totalEmployerContribution.error.invalid",
      max = (Constants.maxMoneyValue, "totalEmployerContribution.error.tooLarge"),
      min = (Constants.minMoneyValue, "totalEmployerContribution.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    employerName: String,
    memberName: String,
    index: Max300,
    secondaryIndex: Max50,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "totalEmployerContribution.title",
      Message("totalEmployerContribution.heading", employerName, memberName),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("totalEmployerContribution.hint"))
      ),
      controllers.nonsipp.employercontributions.routes.TotalEmployerContributionController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
