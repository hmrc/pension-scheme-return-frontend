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

import controllers.actions._
import models.SchemeId.Srn
import models.{Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.StartPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.ContentPageViewModel
import views.html.ContentPageView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartPageController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       allowAccess: AllowAccessActionProvider,
                                       getData: DataRetrievalAction,
                                       createData: DataCreationAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ContentPageView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn)) {
    implicit request =>
      Ok(view(StartPageController.viewModel(srn)))
  }

  def onSubmit(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen createData) {
    implicit request =>
      Redirect(navigator.nextPage(StartPage(srn), NormalMode, request.userAnswers))
  }
}

object StartPageController {

  def viewModel(srn: Srn): ContentPageViewModel = ContentPageViewModel(
    "startPage.title",
    "startPage.heading",
    "site.start",
    isStartButton = true,
    routes.StartPageController.onSubmit(srn),
    "startPage.paragraph1",
    "startPage.paragraph2",
    "startPage.paragraph3",
  )

}