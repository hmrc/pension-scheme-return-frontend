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

package controllers.nonsipp.otherassetsheld

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.CompanyNameOfOtherAssetSellerPage
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import viewmodels.models._
import play.api.data.Form
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CompanyNameOfOtherAssetSellerController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = CompanyNameOfOtherAssetSellerController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(CompanyNameOfOtherAssetSellerPage(srn, index.refined), form)
      Ok(view(preparedForm, CompanyNameOfOtherAssetSellerController.viewModel(srn, index.refined, mode)))
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  CompanyNameOfOtherAssetSellerController.viewModel(srn, index.refined, mode)
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CompanyNameOfOtherAssetSellerPage(srn, index.refined), value))
              nextPage = navigator.nextPage(CompanyNameOfOtherAssetSellerPage(srn, index.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object CompanyNameOfOtherAssetSellerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "companyNameOfOtherAssetsSeller.error.required",
    "companyNameOfOtherAssetsSeller.error.tooLong",
    "error.textarea.invalid"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      "companyNameOfOtherAssetsSeller.title",
      "companyNameOfOtherAssetsSeller.heading",
      TextInputViewModel(isFixedLength = true),
      routes.CompanyNameOfOtherAssetSellerController.onSubmit(srn, index, mode)
    )
}
