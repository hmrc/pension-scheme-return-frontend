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

package controllers.nonsipp.otherassetsdisposal

import services.SaveService
import pages.nonsipp.otherassetsdisposal.CompanyNameOfAssetBuyerPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.otherassetsdisposal.CompanyNameOfAssetBuyerController._
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import viewmodels.models._
import play.api.data.Form
import config.RefinedTypes._
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CompanyNameOfAssetBuyerController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = CompanyNameOfAssetBuyerController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, assetIndex, disposalIndex, mode)))
    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, assetIndex, disposalIndex, mode))))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object CompanyNameOfAssetBuyerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "companyNameOfAssetBuyer.error.required",
    "companyNameOfAssetBuyer.error.tooLong",
    "error.textarea.invalid"
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      title = "companyNameOfAssetBuyer.title",
      heading = "companyNameOfAssetBuyer.heading",
      description = None,
      page = TextInputViewModel(true),
      refresh = None,
      buttonText = "site.saveAndContinue",
      details = None,
      onSubmit = controllers.nonsipp.otherassetsdisposal.routes.CompanyNameOfAssetBuyerController
        .onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
