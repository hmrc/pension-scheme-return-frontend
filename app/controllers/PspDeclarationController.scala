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

package controllers

import controllers.actions.{AllowAccessActionProvider, DataCreationAction, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.PsaDeclarationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models.ContentPageViewModel
import views.html.ContentPageView

import javax.inject.Inject

class PspDeclarationController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          navigator: Navigator,
                                          identify: IdentifierAction,
                                          allowAccess: AllowAccessActionProvider,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: ContentPageView
                                        ) extends FrontendBaseController with I18nSupport {


  def onPageLoad(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn)) {
    implicit request =>
      Ok(view(PspDeclarationController.viewModel(srn)))
  }

  def onSubmit(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(PsaDeclarationPage(srn), NormalMode, request.userAnswers))
  }
}

object PspDeclarationController {

  def viewModel(srn: Srn): ContentPageViewModel = ContentPageViewModel(
    SimpleMessage("pspDeclaration.title"),
    SimpleMessage("pspDeclaration.heading"),
    List(SimpleMessage("pspDeclaration.paragraph")),
    List(SimpleMessage("pspDeclaration.listItem1"),
         SimpleMessage("pspDeclaration.listItem2"),
         SimpleMessage("pspDeclaration.listItem3"),
         SimpleMessage("pspDeclaration.listItem4"),
         SimpleMessage("pspDeclaration.listItem5")),
    SimpleMessage("site.agreeAndContinue"),
    isStartButton = false,
    routes.PspDeclarationController.onSubmit(srn)

  )

}