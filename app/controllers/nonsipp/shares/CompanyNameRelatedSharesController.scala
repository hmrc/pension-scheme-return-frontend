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
import utils.FormUtils._
import controllers.nonsipp.shares.CompanyNameRelatedSharesController._
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, TypeOfSharesHeldPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CompanyNameRelatedSharesController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form = CompanyNameRelatedSharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(TypeOfSharesHeldPage(srn, index)).getOrRecoverJourney { typeOfShares =>
        Ok(
          view(
            form.fromUserAnswers(CompanyNameRelatedSharesPage(srn, index)),
            viewModel(srn, index, typeOfShares.name, mode)
          )
        )
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.userAnswers.get(TypeOfSharesHeldPage(srn, index)).getOrRecoverJourney { typeOfShares =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    CompanyNameRelatedSharesController.viewModel(srn, index, typeOfShares.name, mode)
                  )
                )
              ),
            answer => {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(CompanyNameRelatedSharesPage(srn, index), answer))
                nextPage = navigator.nextPage(CompanyNameRelatedSharesPage(srn, index), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
            }
          )
      }
  }
}

object CompanyNameRelatedSharesController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "shares.companyName.error.required",
    "shares.companyName.error.length",
    "error.textarea.invalid"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    typeOfShares: String,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("shares.companyName.title"),
      Message(
        "shares.companyName.heading",
        Message(s"shares.companyName.heading.type.$typeOfShares")
      ),
      TextInputViewModel(true),
      routes.CompanyNameRelatedSharesController.onSubmit(srn, index, mode)
    )
}
