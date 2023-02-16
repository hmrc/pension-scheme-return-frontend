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
import config.Constants.MaxSchemeBankAccounts
import config.Refined.OneToTen
import controllers.SchemeBankAccountSummaryController._
import controllers.actions._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{BankAccount, Mode}
import navigation.Navigator
import pages.{SchemeBankAccountPage, SchemeBankAccountSummaryPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.ComplexMessageElement.Message
import viewmodels.Delimiter
import viewmodels.DisplayMessage.{ComplexMessage, SimpleMessage}
import viewmodels.implicits._
import viewmodels.models.{SummaryRow, SummaryViewModel}
import views.html.SummaryView

import scala.concurrent.ExecutionContext

class SchemeBankAccountSummaryController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    navigator: Navigator,
                                                    identify: IdentifierAction,
                                                    allowAccess: AllowAccessActionProvider,
                                                    getData: DataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    view: SummaryView,
                                                    formProvider: YesNoPageFormProvider
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = SchemeBankAccountSummaryController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      val bankAccounts = request.userAnswers.schemeBankAccounts(srn)
      if (bankAccounts.isEmpty) {
        Redirect(controllers.routes.SchemeBankAccountController.onPageLoad(srn, refineMV[OneToTen](1), mode))
      } else {
        Ok(view(form, viewModel(srn, mode, bankAccounts)))
      }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      val bankAccounts = request.userAnswers.schemeBankAccounts(srn)
      if (bankAccounts.length == MaxSchemeBankAccounts) {
        Redirect(navigator.nextPage(SchemeBankAccountSummaryPage(srn, addBankAccount = false), mode, request.userAnswers))
      } else {
        form.bindFromRequest().fold(
          formWithErrors => BadRequest(view(formWithErrors, viewModel(srn, mode, bankAccounts))),
          value => Redirect(navigator.nextPage(SchemeBankAccountSummaryPage(srn, value), mode, request.userAnswers))
        )
      }
  }
}

object SchemeBankAccountSummaryController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "schemeBankDetailsSummary.error.required"
  )

  def viewModel(srn: Srn, mode: Mode, bankAccounts: List[BankAccount]): SummaryViewModel = {
    val rows: List[SummaryRow] = bankAccounts.zipWithIndex.flatMap { case (bankAccount, index) =>
      val text = ComplexMessage(List(
        Message(bankAccount.bankName),
        Message("schemeBankDetailsSummary.accountNumber", bankAccount.accountNumber)
      ), Delimiter.Newline)

      refineV[OneToTen](index + 1) match {
        case Left(_) => Nil
        case Right(nextIndex) => List(
          SummaryRow(
            text,
            changeUrl = controllers.routes.SchemeBankAccountController.onPageLoad(srn, nextIndex, mode).url,
            removeUrl = controllers.routes.UnauthorisedController.onPageLoad.url
          )
        )
      }
    }

    SummaryViewModel(
      SimpleMessage("schemeBankDetailsSummary.title", bankAccounts.length),
      SimpleMessage("schemeBankDetailsSummary.heading", bankAccounts.length),
      rows,
      "site.saveAndContinue",
      "schemeBankDetailsSummary.radio",
      "schemeBankDetailsSummary.inset",
      showRadios = bankAccounts.length < 10,
      "site.change",
      "site.remove",
      onSubmit = controllers.routes.SchemeBankAccountSummaryController.onSubmit(srn)
    )
  }
}
