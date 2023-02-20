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
import models.{BankAccount, CheckMode, NormalMode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.{SchemeBankAccountCheckYourAnswersPage, SchemeBankAccountPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryListRow, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersViewModel}
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

      request.userAnswers.get(SchemeBankAccountPage(srn)) match {
        case None =>
          Redirect(routes.SchemeBankAccountController.onPageLoad(srn, NormalMode))
        case Some(bankAccount) => Ok(view(SchemeBankAccountCheckYourAnswersController.viewModel(srn, bankAccount)))
      }
  }

  def onSubmit(srn: Srn): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      Redirect(navigator.nextPage(SchemeBankAccountCheckYourAnswersPage(srn), NormalMode, request.userAnswers))
  }
}

object SchemeBankAccountCheckYourAnswersController {

  private def action(srn: Srn): (SimpleMessage, String) =
    SimpleMessage("site.change") -> routes.SchemeBankAccountController.onPageLoad(srn, NormalMode).url

  def bankAccountAnswers(srn: Srn, bankAccount: BankAccount) =
    Seq(
      CheckYourAnswersRowViewModel("schemeBankAccountCheckYourAnswers.bankName", bankAccount.bankName).withAction(action(srn)),
      CheckYourAnswersRowViewModel("schemeBankAccountCheckYourAnswers.accountNumber", bankAccount.accountNumber).withAction(action(srn)),
      CheckYourAnswersRowViewModel("schemeBankAccountCheckYourAnswers.sortCode", bankAccount.sortCode).withAction(action(srn))
    )

  def viewModel(srn: Srn, bankAccount: BankAccount): CheckYourAnswersViewModel = CheckYourAnswersViewModel(
    "schemeBankAccountCheckYourAnswers.title",
    "schemeBankAccountCheckYourAnswers.heading",
    bankAccountAnswers(srn, bankAccount),
    routes.SchemeBankAccountCheckYourAnswersController.onSubmit(srn)
  )
}

