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
import pages.nonsipp.bonds.CostOfBondsPage
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import config.Constants
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import controllers.nonsipp.bonds.CostOfBondsController._
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.Refined.Max5000
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import views.html.MoneyView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CostOfBondsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form: Form[Money] = CostOfBondsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          form.fromUserAnswers(CostOfBondsPage(srn, index)),
          viewModel(srn, index, form, mode)
        )
      )
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  CostOfBondsController.viewModel(srn, index, form, mode)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CostOfBondsPage(srn, index), answer))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(CostOfBondsPage(srn, index), mode, updatedAnswers))
          }
        )
  }
}

object CostOfBondsController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "bonds.costOfBonds.error.required",
      "bonds.costOfBonds.error.invalid",
      (Constants.maxMoneyValue, "bonds.costOfBonds.error.tooLarge"),
      (Constants.minPosMoneyValue, "bonds.costOfBonds.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "bonds.costOfBonds.title",
      Message("bonds.costOfBonds.heading"),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("bonds.costOfBonds.hint"))
      ),
      controllers.nonsipp.bonds.routes.CostOfBondsController
        .onSubmit(srn, index, mode)
    )
}
