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

package controllers.nonsipp.bonds

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds.UnregulatedOrConnectedBondsHeldPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import controllers.nonsipp.bonds.UnregulatedOrConnectedBondsHeldController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class UnregulatedOrConnectedBondsHeldController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = UnregulatedOrConnectedBondsHeldController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(UnregulatedOrConnectedBondsHeldPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, config.urls.incomeTaxAct, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, config.urls.incomeTaxAct, mode))
            )
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UnregulatedOrConnectedBondsHeldPage(srn), value))
            _ <- saveService.save(updatedAnswers)
            redirectTo <-
              if (value) {
                Future.successful(
                  Redirect(navigator.nextPage(UnregulatedOrConnectedBondsHeldPage(srn), mode, updatedAnswers))
                )
              } else {
                psrSubmissionService
                  .submitPsrDetailsWithUA(
                    srn,
                    updatedAnswers,
                    fallbackCall =
                      controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController.onPageLoad(srn, mode)
                  )(using implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) =>
                      Redirect(navigator.nextPage(UnregulatedOrConnectedBondsHeldPage(srn), mode, updatedAnswers))
                  }
              }
          } yield redirectTo
      )
  }
}

object UnregulatedOrConnectedBondsHeldController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "unregulatedOrConnectedBondsHeld.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, incomeTaxAct: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      "unregulatedOrConnectedBondsHeld.title",
      Message("unregulatedOrConnectedBondsHeld.heading", schemeName),
      YesNoPageViewModel(
        legend = Some(Message("unregulatedOrConnectedBondsHeld.heading2"))
      ),
      controllers.nonsipp.bonds.routes.UnregulatedOrConnectedBondsHeldController
        .onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage("unregulatedOrConnectedBondsHeld.paragraph1") ++
        ListMessage(
          ListType.Bullet,
          "unregulatedOrConnectedBondsHeld.listItem1",
          "unregulatedOrConnectedBondsHeld.listItem2"
        ) ++
        ParagraphMessage(
          "unregulatedOrConnectedBondsHeld.paragraph2",
          LinkMessage(
            "unregulatedOrConnectedBondsHeld.paragraph2.link",
            incomeTaxAct,
            Map("rel" -> "noreferrer noopener", "target" -> "_blank")
          )
        )
    )
}
