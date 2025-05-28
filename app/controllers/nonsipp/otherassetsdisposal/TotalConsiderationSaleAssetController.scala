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
import pages.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import utils.IntUtils.{toInt, IntOpts}
import config.Constants.{maxTotalConsiderationAmount, minTotalConsiderationAmount}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import controllers.nonsipp.otherassetsdisposal.TotalConsiderationSaleAssetController._
import forms.MoneyFormProvider
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalConsiderationSaleAssetController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalConsiderationSaleAssetController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers
          .fillForm(TotalConsiderationSaleAssetPage(srn, assetIndex.refined, disposalIndex.refined), form)

      Ok(view(preparedForm, viewModel(srn, assetIndex.refined, disposalIndex.refined, form, mode)))
    }

  def onSubmit(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  viewModel(srn, assetIndex.refined, disposalIndex.refined, form, mode)
                )
              )
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(TotalConsiderationSaleAssetPage(srn, assetIndex.refined, disposalIndex.refined), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(
                  TotalConsiderationSaleAssetPage(srn, assetIndex.refined, disposalIndex.refined),
                  mode,
                  updatedAnswers
                )
            )
        )
    }
}

object TotalConsiderationSaleAssetController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
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
      page = SingleQuestion(form, QuestionField.currency(Empty)),
      onSubmit = routes.TotalConsiderationSaleAssetController.onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
