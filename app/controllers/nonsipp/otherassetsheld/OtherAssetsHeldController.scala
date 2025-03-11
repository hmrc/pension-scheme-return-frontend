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

package controllers.nonsipp.otherassetsheld

import controllers.nonsipp.otherassetsheld.OtherAssetsHeldController.viewModel
import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.OtherAssetsHeldPage
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherAssetsHeldController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = OtherAssetsHeldController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.get(OtherAssetsHeldPage(srn)).fold(form)(form.fill)
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set(OtherAssetsHeldPage(srn), value))
            _ <- saveService.save(updatedAnswers)
            redirectTo <- if (value) {
              Future.successful(
                Redirect(navigator.nextPage(OtherAssetsHeldPage(srn), mode, updatedAnswers))
              )
            } else {
              psrSubmissionService
                .submitPsrDetailsWithUA(
                  srn,
                  updatedAnswers,
                  fallbackCall =
                    controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController.onPageLoad(srn, mode)
                )(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                .map {
                  case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  case Some(_) =>
                    Redirect(navigator.nextPage(OtherAssetsHeldPage(srn), mode, updatedAnswers))
                }
            }
          } yield redirectTo
      )
  }
}
object OtherAssetsHeldController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "otherAssetsHeld.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      "otherAssetsHeld.title",
      Message("otherAssetsHeld.heading"),
      YesNoPageViewModel(
        legend = Some(Message("otherAssetsHeld.legend", schemeName))
      ),
      routes.OtherAssetsHeldController.onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage("otherAssetsHeld.paragraph") ++ ParagraphMessage("otherAssetsHeld.paragraph1")
    )
}
