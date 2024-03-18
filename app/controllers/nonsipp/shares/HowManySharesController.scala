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

package controllers.nonsipp.shares

import services.SaveService
import utils.FormUtils._
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.IntFormProvider
import models.Mode
import play.api.data.Form
import forms.mappings.errors.IntFormErrors
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, HowManySharesPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.Max5000
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import views.html.IntView
import models.SchemeId.Srn
import controllers.nonsipp.shares.HowManySharesController._
import config.Constants.{maxShares, minShares}
import play.api.i18n.MessagesApi
import viewmodels.InputWidth
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class HowManySharesController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: IntFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: IntView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form = HowManySharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { nameOfSharesCompany =>
        Ok(
          view(
            viewModel(srn, index, nameOfSharesCompany, mode, form.fromUserAnswers(HowManySharesPage(srn, index)))
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
                    HowManySharesController.viewModel(srn, index, nameOfSharesCompany, mode, formWithErrors)
                  )
                )
              ),
            answer => {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(HowManySharesPage(srn, index), answer))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(HowManySharesPage(srn, index), mode, updatedAnswers))
            }
          )
      }
  }
}

object HowManySharesController {
  def form(formProvider: IntFormProvider): Form[Int] = formProvider(
    IntFormErrors(
      "shares.totalShares.error.required",
      "shares.totalShares.error.decimal",
      "shares.totalShares.error.invalid.characters",
      (maxShares, "shares.totalShares.error.size"),
      (minShares, "shares.totalShares.error.zero")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    nameOfSharesCompany: String,
    mode: Mode,
    form: Form[Int]
  ): FormPageViewModel[SingleQuestion[Int]] =
    FormPageViewModel(
      Message("shares.totalShares.title"),
      Message(
        "shares.totalShares.heading",
        Message(s"$nameOfSharesCompany")
      ),
      SingleQuestion(form, QuestionField.input(Empty).withWidth(InputWidth.Fixed10)),
      routes.HowManySharesController.onSubmit(srn, index, mode)
    )
}
