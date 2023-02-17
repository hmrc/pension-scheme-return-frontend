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

import com.google.inject.Inject
import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import navigation.Navigator
import pages.CheckYourAnswersPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView
import viewmodels.models.CheckYourAnswersViewModel
import views.html

class SchemeBankAccountCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      Ok(view(SchemeBankAccountCheckYourAnswersController.viewModel(srn)))
  }

  def onSubmit(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
     Redirect(navigator.nextPage(CheckYourAnswersPage(srn), NormalMode, request.userAnswers))
  }
}

object SchemeBankAccountCheckYourAnswersController {

  val list = SummaryListViewModel(
    rows = Seq(SummaryListRow.apply())
  )
  def viewModel(srn: Srn, bankAccount: BankAccount): CheckYourAnswersViewModel = CheckYourAnswersViewModel (
    "schemeBankAccountCheckYourAnswers.title",
    "schemeBankAccountCheckYourAnswers.heading",
    routes.CheckYourAnswersController.onSubmit(srn),
    list
  )

}

