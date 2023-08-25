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

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.Max5000
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.AreRepaymentsInstalmentsController.viewModel
import forms.YesNoPageFormProvider
import models.{CheckMode, Mode, NormalMode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{AreRepaymentsInstalmentsPage, LoansCYAPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class AreRepaymentsInstalmentsController @Inject()(
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

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(AreRepaymentsInstalmentsPage(srn, index, mode), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(AreRepaymentsInstalmentsPage(srn, index, mode), value))
              _ <- saveService.save(updatedAnswers)
            } yield {
              mode match {
                case CheckMode =>
                  Redirect(navigator.nextPage(LoansCYAPage(srn, index, mode), mode, updatedAnswers))
                case NormalMode =>
                  Redirect(navigator.nextPage(AreRepaymentsInstalmentsPage(srn, index, mode), mode, updatedAnswers))

              }
            }
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
