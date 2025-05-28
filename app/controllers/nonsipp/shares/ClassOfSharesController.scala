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
import controllers.nonsipp.shares.ClassOfSharesController._
import utils.FormUtils._
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.shares.{ClassOfSharesPage, CompanyNameRelatedSharesPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class ClassOfSharesController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form = ClassOfSharesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index.refined)).getOrRecoverJourney {
        nameOfSharesCompany =>
          Ok(
            view(
              form.fromUserAnswers(ClassOfSharesPage(srn, index.refined)),
              viewModel(srn, index.refined, nameOfSharesCompany, mode)
            )
          )
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index.refined)).getOrRecoverJourney {
        nameOfSharesCompany =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      ClassOfSharesController.viewModel(srn, index.refined, nameOfSharesCompany, mode)
                    )
                  )
                ),
              answer => {
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(ClassOfSharesPage(srn, index.refined), answer))
                  nextPage = navigator.nextPage(ClassOfSharesPage(srn, index.refined), mode, updatedAnswers)
                  updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
                  _ <- saveService.save(updatedProgressAnswers)
                } yield Redirect(nextPage)
              }
            )
      }
  }
}

object ClassOfSharesController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "shares.classOfShares.error.required",
    "shares.classOfShares.error.length",
    "error.textarea.invalid"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    nameOfSharesCompany: String,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("shares.classOfShares.title"),
      Message(
        "shares.classOfShares.heading",
        Message(s"$nameOfSharesCompany")
      ),
      TextInputViewModel(true),
      routes.ClassOfSharesController.onSubmit(srn, index, mode)
    )
}
