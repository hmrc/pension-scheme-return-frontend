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

package controllers.nonsipp.membercontributions

import services.SaveService
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money, NameDOB}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import viewmodels.implicits._
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import controllers.nonsipp.membercontributions.TotalMemberContributionController._
import play.api.mvc._
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

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

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)

      val preparedForm =
        request.userAnswers
          .get(TotalMemberContributionPage(srn, index))
          .fold(form)(value => if (value.isZero) form else form.fill(value))

      optionList(index.value - 1)
        .map(_.fullName)
        .getOrRecoverJourney
        .map(memberName => Ok(view(preparedForm, viewModel(srn, index, memberName, form, mode))))
        .merge
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              optionList(index.value - 1)
                .map(_.fullName)
                .getOrRecoverJourney
                .map(
                  memberName =>
                    BadRequest(
                      view(formWithErrors, viewModel(srn, index, memberName, form, mode))
                    )
                )
                .merge
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.transformAndSet(TotalMemberContributionPage(srn, index), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(TotalMemberContributionPage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object TotalMemberContributionController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalMemberContribution.error.required",
      "totalMemberContribution.error.invalid",
      (Constants.maxMoneyValue, "totalMemberContribution.error.tooLarge"),
      (Constants.minPosMoneyValue, "totalMemberContribution.error.tooSmall")
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
      "totalMemberContribution.title",
      Message("totalMemberContribution.heading", memberName),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("totalMemberContribution.hint"))
      ),
      controllers.nonsipp.membercontributions.routes.TotalMemberContributionController
        .onSubmit(srn, index, mode)
    )
}
