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
import pages.nonsipp.otherassetsdisposal.AssetSaleIndependentValuationPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import viewmodels.implicits._
import controllers.nonsipp.otherassetsdisposal.AssetSaleIndependentValuationController._
import config.RefinedTypes._
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class AssetSaleIndependentValuationController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = AssetSaleIndependentValuationController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex)).fold(form)(form.fill)
      Ok(view(preparedForm, viewModel(srn, assetIndex, disposalIndex, mode)))
    }

  def onSubmit(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, assetIndex, disposalIndex, mode)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), value)
                )
              nextPage = navigator
                .nextPage(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, assetIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object AssetSaleIndependentValuationController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "assetSaleIndependentValuation.error.required"
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "assetSaleIndependentValuation.title",
      Message("assetSaleIndependentValuation.heading"),
      controllers.nonsipp.otherassetsdisposal.routes.AssetSaleIndependentValuationController
        .onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
