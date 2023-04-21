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

package controllers.nonsipp.unregulatedorconnectedbonds

import controllers.actions._
import controllers.nonsipp.unregulatedorconnectedbonds.UnregulatedOrConnectedBondsHeldController._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.unregulatedorconnectedbonds.UnregulatedOrConnectedBondsHeldPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UnregulatedOrConnectedBondsHeldController @Inject()(
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

  private val form = UnregulatedOrConnectedBondsHeldController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(UnregulatedOrConnectedBondsHeldPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UnregulatedOrConnectedBondsHeldPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(UnregulatedOrConnectedBondsHeldPage(srn), mode, updatedAnswers))
      )
  }
}

object UnregulatedOrConnectedBondsHeldController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "unregulatedOrConnectedBondsHeld.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): YesNoPageViewModel = YesNoPageViewModel(
    "unregulatedOrConnectedBondsHeld.title",
    Message("unregulatedOrConnectedBondsHeld.heading", schemeName),
    routes.UnregulatedOrConnectedBondsHeldController.onSubmit(srn, mode)
  )
}
