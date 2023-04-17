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

package controllers.nonsipp.loansmadeoroutstanding

import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingController.viewModel
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LoansMadeOrOutstandingController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(memberName: String): Form[Boolean] =
    LoansMadeOrOutstandingController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    val preparedForm = request.userAnswers.fillForm(LoansMadeOrOutstandingPage(srn), form(schemeName))
    Ok(view(preparedForm, viewModel(srn, schemeName, mode)))
  }
  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    form(schemeName)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, schemeName, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(LoansMadeOrOutstandingPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(LoansMadeOrOutstandingPage(srn), mode, updatedAnswers))
      )
  }
}
object LoansMadeOrOutstandingController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "loansMadeOrOutstanding.error.required"
  )

  def viewModel(srn: Srn, memberName: String, mode: Mode): YesNoPageViewModel =
    YesNoPageViewModel(
      Message("loansMadeOrOutstanding.title"),
      Message("loansMadeOrOutstanding.heading", memberName),
      routes.LoansMadeOrOutstandingController.onSubmit(srn, mode)
    )
}
