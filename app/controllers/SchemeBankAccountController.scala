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
import controllers.SchemeBankAccountController._
import controllers.actions._
import forms.BankAccountFormProvider
import models.{BankAccount, Mode, NormalMode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.SchemeBankAccountPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.implicits._
import viewmodels.models.BankAccountViewModel
import views.html.BankAccountView

import scala.concurrent.{ExecutionContext, Future}

class SchemeBankAccountController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             navigator: Navigator,
                                             identify: IdentifierAction,
                                             allowAccess: AllowAccessActionProvider,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: BankAccountView,
                                             formProvider: BankAccountFormProvider,
                                             saveService: SaveService,
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = SchemeBankAccountController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request =>
      Ok(view(form.fromUserAnswers(SchemeBankAccountPage(srn)), viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SchemeBankAccountPage(srn), value))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(SchemeBankAccountPage(srn), mode, updatedAnswers))
      )
  }
}

object SchemeBankAccountController {

  def form(formProvider: BankAccountFormProvider): Form[BankAccount] = formProvider(
    "schemeBankDetails.bankName.error.required",
    "schemeBankDetails.bankName.error.invalid",
    "schemeBankDetails.bankName.error.length",
    "schemeBankDetails.accountNumber.error.required",
    "schemeBankDetails.accountNumber.error.invalid",
    "schemeBankDetails.accountNumber.error.length",
    "schemeBankDetails.sortCode.error.required",
    "schemeBankDetails.sortCode.error.invalid",
    "schemeBankDetails.sortCode.error.format.invalid",
    "schemeBankDetails.sortCode.error.length"
  )

  def viewModel(srn: Srn, mode: Mode): BankAccountViewModel = BankAccountViewModel(
    "schemeBankDetails.title",
    "schemeBankDetails.heading",
    "schemeBankDetails.paragraph",
    "schemeBankDetails.bankName.heading",
    "schemeBankDetails.accountNumber.heading",
    "schemeBankDetails.accountNumber.hint",
    "schemeBankDetails.sortCode.heading",
    "schemeBankDetails.sortCode.hint",
    "site.saveAndContinue",
    routes.SchemeBankAccountController.onSubmit(srn, mode)
  )
}
