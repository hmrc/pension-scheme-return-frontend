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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import viewmodels.implicits._
import controllers.actions._
import pages.nonsipp.sharesdisposal.IndependentValuationPage
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.data.Form
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes._
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import controllers.nonsipp.sharesdisposal.IndependentValuationController._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IndependentValuationController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = IndependentValuationController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(IndependentValuationPage(srn, index, disposalIndex)).fold(form)(form.fill)
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { companyName =>
        Ok(view(preparedForm, viewModel(srn, companyName, index, disposalIndex, mode)))
      }
    }

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { companyName =>
              Future
                .successful(BadRequest(view(formWithErrors, viewModel(srn, companyName, index, disposalIndex, mode))))
            }
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(IndependentValuationPage(srn, index, disposalIndex), value))
              nextPage = navigator.nextPage(IndependentValuationPage(srn, index, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object IndependentValuationController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "sharesDisposal.independentValuation.error.required"
  )

  def viewModel(
    srn: Srn,
    companyName: String,
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "sharesDisposal.independentValuation.title",
      Message("sharesDisposal.independentValuation.heading", companyName),
      controllers.nonsipp.sharesdisposal.routes.IndependentValuationController.onSubmit(srn, index, disposalIndex, mode)
    )
}
