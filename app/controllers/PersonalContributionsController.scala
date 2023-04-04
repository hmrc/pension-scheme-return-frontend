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
import forms.YesNoPageFormProvider
import javax.inject.Inject
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.PersonalContributionsPage
import play.api.data.Form
import viewmodels.models.YesNoPageViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import services.SaveService
import PersonalContributionsController._
import viewmodels.implicits._

import scala.concurrent.{ExecutionContext, Future}

class PersonalContributionsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = PersonalContributionsController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(PersonalContributionsPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PersonalContributionsPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(PersonalContributionsPage(srn), mode, updatedAnswers))
      )
  }
}

object PersonalContributionsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "personalContributions.error.required"
  )

  def viewModel(srn: Srn, mode: Mode): YesNoPageViewModel = YesNoPageViewModel(
    "personalContributions.title",
    "personalContributions.heading",
    controllers.routes.PersonalContributionsController.onSubmit(srn, mode)
  )
}
