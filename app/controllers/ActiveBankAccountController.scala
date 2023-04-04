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

import controllers.ActiveBankAccountController.viewModel
import controllers.actions._
import forms.YesNoPageFormProvider

import javax.inject.Inject
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.{ActiveBankAccountPage, HowManyMembersPage}
import play.api.data.Form
import viewmodels.models.YesNoPageViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import services.{SaveService, SchemeDateService}
import viewmodels.implicits._

import scala.concurrent.{ExecutionContext, Future}
import utils.FormUtils._
import viewmodels.DisplayMessage.Message

class ActiveBankAccountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  dateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(memberName: String): Form[Boolean] =
    ActiveBankAccountController.form(formProvider, memberName)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    Ok(view(form(schemeName), viewModel(srn, schemeName, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    form(request.schemeDetails.schemeName)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, schemeName, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ActiveBankAccountPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(ActiveBankAccountPage(srn), mode, updatedAnswers))
      )
  }
}

object ActiveBankAccountController {
  def form(formProvider: YesNoPageFormProvider, memberName: String): Form[Boolean] = formProvider(
    "activeAccountDetails.error.required",
    List(memberName)
  )

  def viewModel(srn: Srn, memberName: String, mode: Mode): YesNoPageViewModel =
    YesNoPageViewModel(
      Message("activeAccountDetails.title"),
      Message("activeAccountDetails.heading", memberName),
      controllers.routes.ActiveBankAccountController.onSubmit(srn, mode)
    )
}
