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

package controllers.nonsipp.declaration

import services.{PsrSubmissionService, SaveService, SchemeDateService}
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import config.Constants.{RETURN_PERIODS, SUBMISSION_DATE}
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.{NormalMode, UserAnswers}
import play.api.data.Form
import views.html.PsaIdInputView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import pages.nonsipp.declaration.PspDeclarationPage
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PspDeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  formProvider: TextFormProvider,
  view: PsaIdInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      def form: Form[String] = PspDeclarationController.form(formProvider, request.schemeDetails.authorisingPSAID)
      Ok(
        view(
          form.fromUserAnswers(PspDeclarationPage(srn)),
          PspDeclarationController.viewModel(srn)
        )
      )
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      PspDeclarationController
        .form(formProvider, request.schemeDetails.authorisingPSAID)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  PspDeclarationController.viewModel(srn)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(PspDeclarationPage(srn), answer))
              _ <- saveService.save(updatedAnswers)
              _ <- psrSubmissionService.submitPsrDetails(
                srn = srn,
                isSubmitted = true,
                fallbackCall = controllers.nonsipp.declaration.routes.PspDeclarationController.onPageLoad(srn)
              )
              _ <- saveService.save(UserAnswers(request.userAnswers.id))
            } yield {
              Redirect(navigator.nextPage(PspDeclarationPage(srn), NormalMode, request.userAnswers))
                .addingToSession((RETURN_PERIODS, schemeDateService.returnPeriodsAsJsonString(srn)))
                .addingToSession((SUBMISSION_DATE, schemeDateService.submissionDateAsString(schemeDateService.now())))
            }
          }
        )
    }
}

object PspDeclarationController {
  def form(formProvider: TextFormProvider, authorisingPsaId: Option[String]): Form[String] = formProvider.psaId(
    "pspDeclaration.psaId.error.required",
    "pspDeclaration.psaId.error.invalid.characters",
    "pspDeclaration.psaId.error.invalid.characters",
    "pspDeclaration.psaId.error.invalid.noMatch",
    authorisingPsaId
  )

  def viewModel(srn: Srn): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("pspDeclaration.title"),
      Message("pspDeclaration.heading"),
      TextInputViewModel(Some(Message("pspDeclaration.psaId.label")), isFixedLength = true),
      routes.PspDeclarationController.onSubmit(srn)
    ).withButtonText(Message("site.agreeAndContinue"))
      .withDescription(
        ParagraphMessage("pspDeclaration.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "pspDeclaration.listItem1",
            "pspDeclaration.listItem2",
            "pspDeclaration.listItem3",
            "pspDeclaration.listItem4",
            "pspDeclaration.listItem5"
          )
      )

}
