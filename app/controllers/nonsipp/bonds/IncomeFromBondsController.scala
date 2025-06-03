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

package controllers.nonsipp.bonds

import services.SaveService
import pages.nonsipp.bonds.{BondsCompleted, IncomeFromBondsPage}
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import controllers.PSRController
import config.Constants
import utils.IntUtils.toRefined5000
import controllers.actions._
import controllers.nonsipp.bonds.IncomeFromBondsController._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import views.html.MoneyView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IncomeFromBondsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form: Form[Money] = IncomeFromBondsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          form.fromUserAnswers(IncomeFromBondsPage(srn, index)),
          viewModel(srn, index, form, mode)
        )
      )

  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  IncomeFromBondsController.viewModel(srn, index, form, mode)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(IncomeFromBondsPage(srn, index), answer)
                    .set(BondsCompleted(srn, index), SectionCompleted)
                )
              nextPage = navigator.nextPage(IncomeFromBondsPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
          }
        )
  }
}

object IncomeFromBondsController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "bonds.incomeFromBonds.error.required",
      "bonds.incomeFromBonds.error.invalid",
      (Constants.maxMoneyValue, "bonds.incomeFromBonds.error.tooLarge"),
      (Constants.zeroMoneyValue, "bonds.incomeFromBonds.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Int,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "bonds.incomeFromBonds.title",
      Message("bonds.incomeFromBonds.heading"),
      SingleQuestion(
        form,
        QuestionField.currency(Empty, Some("bonds.incomeFromBonds.hint"))
      ),
      controllers.nonsipp.bonds.routes.IncomeFromBondsController
        .onSubmit(srn, index, mode)
    )
}
