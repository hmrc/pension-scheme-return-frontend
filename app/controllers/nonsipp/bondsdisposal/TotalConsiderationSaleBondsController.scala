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

package controllers.nonsipp.bondsdisposal

import services.SaveService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import utils.IntUtils.{toInt, IntOpts}
import config.Constants.{maxTotalConsiderationAmount, minTotalConsiderationAmount}
import controllers.actions.IdentifyAndRequireData
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import controllers.nonsipp.bondsdisposal.TotalConsiderationSaleBondsController._
import navigation.Navigator
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal.TotalConsiderationSaleBondsPage
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalConsiderationSaleBondsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalConsiderationSaleBondsController.form(formProvider)

  def onPageLoad(srn: Srn, bondIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers
          .fillForm(TotalConsiderationSaleBondsPage(srn, bondIndex.refined, disposalIndex.refined), form)

      Ok(view(preparedForm, viewModel(srn, bondIndex.refined, disposalIndex.refined, form, mode)))
    }

  def onSubmit(srn: Srn, bondIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  viewModel(srn, bondIndex.refined, disposalIndex.refined, form, mode)
                )
              )
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(TotalConsiderationSaleBondsPage(srn, bondIndex.refined, disposalIndex.refined), value)
                )
              nextPage = navigator
                .nextPage(
                  TotalConsiderationSaleBondsPage(srn, bondIndex.refined, disposalIndex.refined),
                  mode,
                  updatedAnswers
                )
              updatedProgressAnswers <- saveProgress(
                srn,
                bondIndex.refined,
                disposalIndex.refined,
                updatedAnswers,
                nextPage
              )
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object TotalConsiderationSaleBondsController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      requiredKey = "bondsDisposal.totalConsiderationBondsSold.error.required",
      nonNumericKey = "bondsDisposal.totalConsiderationBondsSold.error.invalid.characters",
      min = (minTotalConsiderationAmount, "bondsDisposal.totalConsiderationBondsSold.error.tooSmall"),
      max = (maxTotalConsiderationAmount, "bondsDisposal.totalConsiderationBondsSold.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      title = Message("bondsDisposal.totalConsiderationBondsSold.title"),
      heading = Message("bondsDisposal.totalConsiderationBondsSold.heading"),
      page = SingleQuestion(form, QuestionField.currency(Empty)),
      onSubmit = routes.TotalConsiderationSaleBondsController.onSubmit(srn, bondIndex, disposalIndex, mode)
    )
}
