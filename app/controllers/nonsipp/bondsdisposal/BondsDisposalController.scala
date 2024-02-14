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

import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import controllers.PSRController
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.YesNoPageView
import services.SaveService
import BondsDisposalController._
import viewmodels.implicits._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import pages.nonsipp.bondsdisposal.BondsDisposalPage
import viewmodels.DisplayMessage.Message

class BondsDisposalController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = BondsDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.get(BondsDisposalPage(srn)).fold(form)(form.fill)
    Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode))))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BondsDisposalPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(BondsDisposalPage(srn), mode, updatedAnswers))
      )
  }
}

object BondsDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "bondsDisposal.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "bondsDisposal.title",
    Message("bondsDisposal.heading", schemeName),
    controllers.nonsipp.bondsdisposal.routes.BondsDisposalController.onSubmit(srn, mode)
  )
}
