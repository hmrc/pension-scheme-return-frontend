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

package controllers.nonsipp.sharesdisposal

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.{SharesDisposalCompleted, SharesDisposalPage}
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import controllers.nonsipp.sharesdisposal.SharesDisposalController._
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, SectionCompleted, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SharesDisposalController @Inject()(
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

  private val form = SharesDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(SharesDisposalPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, mode)))),
        addDisposal =>
          if (addDisposal) {
            for {
              updatedAnswers <- request.userAnswers
                .set(SharesDisposalPage(srn), true)
                .remove(SharesDisposalCompleted(srn))
                .mapK[Future]
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(SharesDisposalPage(srn), mode, updatedAnswers))
          } else {
            for {
              updatedAnswers <- request.userAnswers
                .set(SharesDisposalPage(srn), false)
                .set(SharesDisposalCompleted(srn), SectionCompleted)
                .mapK[Future]
              _ <- saveService.save(updatedAnswers)
              submissionResult <- psrSubmissionService.submitPsrDetails(srn, updatedAnswers)
            } yield submissionResult
              .getOrRecoverJourney(_ => Redirect(navigator.nextPage(SharesDisposalPage(srn), mode, updatedAnswers)))
          }
      )
  }
}

object SharesDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "sharesDisposal.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "sharesDisposal.title",
    Message("sharesDisposal.heading", schemeName),
    routes.SharesDisposalController.onSubmit(srn, mode)
  )
}
