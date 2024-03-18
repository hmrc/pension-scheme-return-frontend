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

package controllers.nonsipp.otherassetsheld

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.CostOfOtherAssetPage
import controllers.PSRController
import config.Constants
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import controllers.nonsipp.otherassetsheld.CostOfOtherAssetController._
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.Refined.Max5000
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import views.html.MoneyView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CostOfOtherAssetController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form = CostOfOtherAssetController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          viewModel(srn, index, form.fromUserAnswers(CostOfOtherAssetPage(srn, index)), mode)
        )
      )

  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  CostOfOtherAssetController.viewModel(srn, index, formWithErrors, mode)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CostOfOtherAssetPage(srn, index), answer))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(CostOfOtherAssetPage(srn, index), mode, updatedAnswers))
          }
        )
  }
}

object CostOfOtherAssetController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "otherAssets.costOfOtherAsset.error.required",
      "otherAssets.costOfOtherAsset.error.invalid",
      (Constants.maxMoneyValue, "otherAssets.costOfOtherAsset.error.tooLarge"),
      (Constants.minPosMoneyValue, "otherAssets.costOfOtherAsset.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "otherAssets.costOfOtherAsset.title",
      Message("otherAssets.costOfOtherAsset.heading"),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("otherAssets.costOfOtherAsset.hint"))
      ),
      controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
        .onSubmit(srn, index, mode)
    )
}
