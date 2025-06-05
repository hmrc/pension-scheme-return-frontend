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

package controllers.nonsipp.shares

import services.SaveService
import viewmodels.implicits._
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import controllers.nonsipp.shares.SharesIndependentValuationController._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesIndependentValuationPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes._
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SharesIndependentValuationController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = SharesIndependentValuationController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.get(SharesIndependentValuationPage(srn, index)).fold(form)(form.fill)
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { companyName =>
        Ok(view(preparedForm, viewModel(srn, companyName, index, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CompanyNameRelatedSharesPage(srn, index)).getOrRecoverJourney { companyName =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, companyName, index, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(SharesIndependentValuationPage(srn, index), value))
              nextPage = navigator.nextPage(SharesIndependentValuationPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }
}

object SharesIndependentValuationController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "sharesIndependentValuation.error.required"
  )

  def viewModel(srn: Srn, companyName: String, index: Max5000, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "sharesIndependentValuation.title",
      Message("sharesIndependentValuation.heading", companyName),
      controllers.nonsipp.shares.routes.SharesIndependentValuationController.onSubmit(srn, index, mode)
    )
}
