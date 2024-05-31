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

package controllers.nonsipp.membersurrenderedbenefits

import services.{PsrSubmissionService, SaveService}
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsController._
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsJourneyStatus, SurrenderedBenefitsPage}
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, SectionStatus, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SurrenderedBenefitsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = SurrenderedBenefitsController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(SurrenderedBenefitsPage(srn), form)
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
            updatedAnswers <- request.userAnswers
              .set(SurrenderedBenefitsPage(srn), value)
              .setWhen(!value)(SurrenderedBenefitsJourneyStatus(srn), SectionStatus.Completed)
              .mapK[Future]
            _ <- saveService.save(updatedAnswers)
            submissionResult <- if (!value) {
              psrSubmissionService.submitPsrDetailsWithUA(
                srn,
                updatedAnswers,
                fallbackCall = controllers.nonsipp.membersurrenderedbenefits.routes.SurrenderedBenefitsController
                  .onPageLoad(srn, mode)
              )
            } else {
              Future.successful(Some(()))
            }
          } yield submissionResult
            .getOrRecoverJourney(_ => Redirect(navigator.nextPage(SurrenderedBenefitsPage(srn), mode, updatedAnswers)))
      )
  }
}

object SurrenderedBenefitsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "surrenderedBenefits.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "surrenderedBenefits.title",
    Message("surrenderedBenefits.heading", schemeName),
    routes.SurrenderedBenefitsController.onSubmit(srn, mode)
  )
}
