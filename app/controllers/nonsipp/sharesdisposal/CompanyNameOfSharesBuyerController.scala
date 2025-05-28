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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions._
import pages.nonsipp.sharesdisposal.CompanyBuyerNamePage
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import viewmodels.models._
import config.RefinedTypes._
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn
import controllers.nonsipp.sharesdisposal.CompanyNameOfSharesBuyerController._
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CompanyNameOfSharesBuyerController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = CompanyNameOfSharesBuyerController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, secondaryIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(CompanyBuyerNamePage(srn, index.refined, secondaryIndex.refined)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, index.refined, secondaryIndex.refined, mode)))
    }

  def onSubmit(srn: Srn, index: Int, secondaryIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, index.refined, secondaryIndex.refined, mode))))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(CompanyBuyerNamePage(srn, index.refined, secondaryIndex.refined), value)
                )
              nextPage = navigator
                .nextPage(CompanyBuyerNamePage(srn, index.refined, secondaryIndex.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(
                srn,
                index.refined,
                secondaryIndex.refined,
                updatedAnswers,
                nextPage
              )
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object CompanyNameOfSharesBuyerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "companyNameOfSharesBuyer.error.required",
    "companyNameOfSharesBuyer.error.tooLong",
    "error.textarea.invalid"
  )

  def viewModel(srn: Srn, index: Max5000, secondaryIndex: Max50, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      title = "companyNameOfSharesBuyer.title",
      heading = "companyNameOfSharesBuyer.heading",
      description = None,
      page = TextInputViewModel(true),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      onSubmit = controllers.nonsipp.sharesdisposal.routes.CompanyNameOfSharesBuyerController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
