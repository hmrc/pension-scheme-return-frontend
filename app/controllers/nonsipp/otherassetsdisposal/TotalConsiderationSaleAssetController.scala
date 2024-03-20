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

package controllers.nonsipp.otherassetsdisposal

import config.Constants.{maxTotalConsiderationAmount, minTotalConsiderationAmount}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetController._
import forms.mappings.errors.{MoneyFormErrorProvider, MoneyFormErrors}
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField}
import views.html.MoneyView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TotalConsiderationSaleAssetController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormErrorProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalConsiderationSaleAssetController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), form)

      Ok(view(viewModel(srn, assetIndex, disposalIndex, preparedForm, mode)))
    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              BadRequest(
                view(viewModel(srn, assetIndex, disposalIndex, formWithErrors, mode))
              )
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object TotalConsiderationSaleAssetController {
  def form(formProvider: MoneyFormErrorProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      requiredKey = "totalConsiderationSaleAsset.error.required",
      nonNumericKey = "totalConsiderationSaleAsset.error.invalid.characters",
      min = (minTotalConsiderationAmount, "totalConsiderationSaleAsset.error.tooSmall"),
      max = (maxTotalConsiderationAmount, "totalConsiderationSaleAsset.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      title = Message("totalConsiderationSaleAsset.title"),
      heading = Message("totalConsiderationSaleAsset.heading"),
      page = SingleQuestion(form, QuestionField.input(Empty)),
      onSubmit = routes.TotalConsiderationSaleAssetController.onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
