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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.PartnershipOtherAssetSellerNamePage
import config.Refined.Max5000
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import controllers.nonsipp.otherassetsheld.PartnershipNameOfOtherAssetsSellerController._
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PartnershipNameOfOtherAssetsSellerController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = PartnershipNameOfOtherAssetsSellerController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(PartnershipOtherAssetSellerNamePage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
    }
  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(PartnershipOtherAssetSellerNamePage(srn, index), value)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(PartnershipOtherAssetSellerNamePage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object PartnershipNameOfOtherAssetsSellerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "partnershipNameOfOtherAssetsSeller.error.required",
    "partnershipNameOfOtherAssetsSeller.error.tooLong",
    "partnershipNameOfOtherAssetsSeller.error.invalid"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] = FormPageViewModel(
    Message("partnershipNameOfOtherAssetsSeller.title"),
    Message("partnershipNameOfOtherAssetsSeller.heading"),
    TextInputViewModel(isFixedLength = true),
    routes.PartnershipNameOfOtherAssetsSellerController.onSubmit(srn, index, mode)
  )
}
