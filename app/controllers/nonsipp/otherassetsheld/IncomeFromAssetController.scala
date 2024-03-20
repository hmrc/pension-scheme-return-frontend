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

import config.Constants
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.otherassetsheld.IncomeFromAssetController._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{Mode, Money}
import navigation.Navigator
import pages.nonsipp.otherassetsheld.{IncomeFromAssetPage, OtherAssetsCompleted}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import viewmodels.models.{FormPageViewModel, QuestionField, SectionCompleted}
import views.html.MoneyView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IncomeFromAssetController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private def form = IncomeFromAssetController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          viewModel(srn, index, form.fromUserAnswers(IncomeFromAssetPage(srn, index)), mode)
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
                  IncomeFromAssetController.viewModel(srn, index, formWithErrors, mode)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(IncomeFromAssetPage(srn, index), answer)
                    .set(OtherAssetsCompleted(srn, index), SectionCompleted)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(IncomeFromAssetPage(srn, index), mode, updatedAnswers))
          }
        )
  }
}

object IncomeFromAssetController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "otherAssets.incomeFromAsset.error.required",
      "otherAssets.incomeFromAsset.error.invalid",
      (Constants.maxMoneyValue, "otherAssets.incomeFromAsset.error.tooLarge"),
      (Constants.zeroMoneyValue, "otherAssets.incomeFromAsset.error.invalid")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "otherAssets.incomeFromAsset.title",
      Message("otherAssets.incomeFromAsset.heading"),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("otherAssets.incomeFromAsset.hint"))
      ),
      controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
        .onSubmit(srn, index, mode)
    )
}
