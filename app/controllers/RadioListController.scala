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

import controllers.actions.{AllowAccessActionProvider, DataCreationAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.RadioListPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.routing.Router.empty.routes
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{ContentPageViewModel, RadioListViewModel}
import views.html.{ContentPageView, RadioListView}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.reflect.internal.util.NoSourceFile.content

class RadioListController {class StartPageController @Inject()(
                                                                override val messagesApi: MessagesApi,
                                                                navigator: Navigator,
                                                                identify: IdentifierAction,
                                                                allowAccess: AllowAccessActionProvider,
                                                                getData: DataRetrievalAction,
                                                                createData: DataCreationAction,
                                                                val controllerComponents: MessagesControllerComponents,
                                                                view: RadioListView
                                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn)) {
    implicit request =>
      Ok(view(RadioListController.viewModel(srn)))
  }

  def onSubmit(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen createData) {
    implicit request =>
      Redirect(navigator.nextPage(RadioListPage(srn), NormalMode, request.userAnswers))
  }
}

  object RadioListController {

    def viewModel(srn: Srn): RadioListViewModel = RadioListViewModel(
      "startPage.title",
      "startPage.heading",
      "site.saveAndContinue",
        routes.RadioListController.onSubmit(srn),
      List(
        RadioItem(content = Text("yea1"))
    )


    )

  }

}
