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

package controllers.nonsipp.shares

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, CostOfSharesPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import controllers.nonsipp.shares.CostOfSharesController._
import controllers.actions._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CostOfSharesController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form: Form[Money] = CostOfSharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { nameOfSharesCompany =>
        Ok(
          view(
            form.fromUserAnswers(CostOfSharesPage(srn, index)),
            viewModel(srn, index, nameOfSharesCompany, form, mode)
          )
        )
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { nameOfSharesCompany =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    CostOfSharesController.viewModel(srn, index, nameOfSharesCompany, form, mode)
                  )
                )
              ),
            answer => {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(CostOfSharesPage(srn, index), answer))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(CostOfSharesPage(srn, index), mode, updatedAnswers))
            }
          )
      }
  }
}

object CostOfSharesController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "shares.costOfShares.error.required",
      "shares.costOfShares.error.invalid",
      (Constants.maxMoneyValue, "shares.costOfShares.error.tooLarge"),
      (Constants.minPosMoneyValue, "shares.costOfShares.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    nameOfSharesCompany: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "shares.costOfShares.title",
      Message(
        "shares.costOfShares.heading",
        Message(s"$nameOfSharesCompany")
      ),
      SingleQuestion(
        form,
        QuestionField.currency(Empty, Some("shares.costOfShares.hint"))
      ),
      controllers.nonsipp.shares.routes.CostOfSharesController
        .onSubmit(srn, index, mode)
    )
}
