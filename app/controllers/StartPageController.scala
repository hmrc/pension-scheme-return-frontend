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
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.StartPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class StartPageController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)) { implicit request =>
    Ok(view(StartPageController.viewModel(srn)))
  }

  def onSubmit(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData) {
    implicit request =>
      Redirect(navigator.nextPage(StartPage(srn), NormalMode, request.userAnswers))
  }
}

object StartPageController {

  def viewModel(srn: Srn): FormPageViewModel[ContentPageViewModel] = ContentPageViewModel.startPage(
    "startPage.title",
    "startPage.heading",
    List(
      "startPage.paragraph1",
      "startPage.paragraph2",
      "startPage.paragraph3"
    ),
    routes.StartPageController.onSubmit(srn)
  )

}
