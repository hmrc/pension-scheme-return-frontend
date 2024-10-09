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

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(CompanyNameOfOtherAssetSellerPage(srn, index), form)
      Ok(view(preparedForm, CompanyNameOfOtherAssetSellerController.viewModel(srn, index, mode)))
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  CompanyNameOfOtherAssetSellerController.viewModel(srn, index, mode)
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CompanyNameOfOtherAssetSellerPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(CompanyNameOfOtherAssetSellerPage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object CompanyNameOfOtherAssetSellerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "companyNameOfOtherAssetsSeller.error.required",
    "companyNameOfOtherAssetsSeller.error.tooLong",
    "companyNameOfOtherAssetsSeller.error.invalid"
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
