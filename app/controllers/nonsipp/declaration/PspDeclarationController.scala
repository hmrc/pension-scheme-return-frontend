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

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions._
import navigation.Navigator
import models.NormalMode
import views.html.ContentPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import pages.nonsipp.declaration.PspDeclarationPage
import viewmodels.DisplayMessage._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}

import javax.inject.{Inject, Named}

class PspDeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(view(PspDeclarationController.viewModel(srn)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(PspDeclarationPage(srn), NormalMode, request.userAnswers))
    }
}

object PspDeclarationController {

  def viewModel(srn: Srn): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("pspDeclaration.title"),
      Message("pspDeclaration.heading"),
      ContentPageViewModel(),
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
