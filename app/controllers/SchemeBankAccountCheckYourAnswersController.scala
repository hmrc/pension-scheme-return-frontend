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
import config.Refined.Max10
import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.SchemeId.Srn
import models.{BankAccount, NormalMode}
import navigation.Navigator
import pages.{SchemeBankAccountCheckYourAnswersPage, SchemeBankAccountPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersViewModel, SummaryAction}
import views.html.CheckYourAnswersView

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

  def onPageLoad(srn: Srn, index: Max10): Action[AnyContent] =
    (identify andThen allowAccess(srn) andThen getData andThen requireData) {
      implicit request =>

        request.userAnswers.get(SchemeBankAccountPage(srn, index)) match {
          case None =>
            Redirect(routes.SchemeBankAccountController.onPageLoad(srn, index, NormalMode))
          case Some(bankAccount) =>
            Ok(view(SchemeBankAccountCheckYourAnswersController.viewModel(srn, index, bankAccount)))
        }
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    (identify andThen allowAccess(srn) andThen getData andThen requireData) {
      implicit request =>
        Redirect(navigator.nextPage(SchemeBankAccountCheckYourAnswersPage(srn), NormalMode, request.userAnswers))
    }
}

object SchemeBankAccountCheckYourAnswersController {

  private def action(srn: Srn, index: Max10): SummaryAction = {
    SummaryAction("site.change", routes.SchemeBankAccountController.onPageLoad(srn, index, NormalMode).url)
  }

  def bankAccountAnswers(srn: Srn, index: Max10, bankAccount: BankAccount) =
    Seq(
      CheckYourAnswersRowViewModel("schemeBankDetails.bankName.heading", bankAccount.bankName)
        .withAction(action(srn, index).withVisuallyHiddenContent("schemeBankDetails.bankName.heading.vh")),
      CheckYourAnswersRowViewModel("schemeBankDetails.accountNumber.heading", bankAccount.accountNumber)
        .withAction(action(srn, index).withVisuallyHiddenContent("schemeBankDetails.accountNumber.heading.vh")),
      CheckYourAnswersRowViewModel("schemeBankDetails.sortCode.heading", bankAccount.sortCode)
        .withAction(action(srn, index).withVisuallyHiddenContent("schemeBankDetails.sortCode.heading.vh"))
    )

  def viewModel(srn: Srn, index: Max10, bankAccount: BankAccount): CheckYourAnswersViewModel = CheckYourAnswersViewModel(
    bankAccountAnswers(srn, index, bankAccount),
    routes.SchemeBankAccountCheckYourAnswersController.onSubmit(srn)
  )
}

