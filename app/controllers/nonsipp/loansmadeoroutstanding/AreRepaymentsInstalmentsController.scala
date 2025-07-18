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

package controllers.nonsipp.loansmadeoroutstanding

import services.SaveService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import pages.nonsipp.loansmadeoroutstanding.AreRepaymentsInstalmentsPage
import play.api.data.Form
import views.html.YesNoPageView
import models.SchemeId.Srn
import navigation.Navigator
import controllers.nonsipp.loansmadeoroutstanding.AreRepaymentsInstalmentsController.viewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class AreRepaymentsInstalmentsController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = AreRepaymentsInstalmentsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(AreRepaymentsInstalmentsPage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- request.userAnswers.set(AreRepaymentsInstalmentsPage(srn, index), value).mapK
              nextPage = navigator.nextPage(AreRepaymentsInstalmentsPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }
}

object AreRepaymentsInstalmentsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "areRepaymentsInstalments.equalInstallments.error.required"
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("areRepaymentsInstalments.equalInstallments.title"),
      Message("areRepaymentsInstalments.equalInstallments.heading"),
      routes.AreRepaymentsInstalmentsController.onSubmit(srn, index, mode)
    )
}
