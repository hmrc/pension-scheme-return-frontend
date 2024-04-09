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

package controllers.nonsipp.declaration

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.NormalMode
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
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  formProvider: TextFormProvider,
  view: PsaIdInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      def form = PspDeclarationController.form(formProvider, request.schemeDetails.authorisingPSAID)
      Ok(
        view(
          form.fromUserAnswers(PspDeclarationPage(srn)),
          PspDeclarationController.viewModel(srn)
        )
      )
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
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
            } yield Redirect(navigator.nextPage(PspDeclarationPage(srn), NormalMode, request.userAnswers))
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
      TextInputViewModel(Some(Message("pspDeclaration.psaId.label")), true),
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
