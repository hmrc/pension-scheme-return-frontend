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
import models.{BankAccount, Mode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.{RemoveSchemeBankAccountPage, SchemeBankAccountPage}
import play.api.data.Form
import viewmodels.models.YesNoPageViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import RemoveSchemeBankAccountController._
import config.Refined.Max10
import models.requests.DataRequest
import services.SaveService
import viewmodels.DisplayMessage.SimpleMessage

import scala.concurrent.{ExecutionContext, Future}

class RemoveSchemeBankAccountController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         allowAccess: AllowAccessActionProvider,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: YesNoPageFormProvider,
                                         saveService: SaveService,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: YesNoPageView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = RemoveSchemeBankAccountController.form(formProvider)

  def onPageLoad(srn:Srn, index: Max10, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData) {
    implicit request => withBankAccountAtIndex(srn, index)(bankAccount => Ok(view(form, viewModel(srn, index, bankAccount, mode))))
  }

  def onSubmit(srn: Srn, index: Max10, mode: Mode): Action[AnyContent] = (identify andThen allowAccess(srn) andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(
          withBankAccountAtIndex(srn, index)(bankAccount => BadRequest(view(formWithErrors, viewModel(srn, index, bankAccount, mode))))
        ),
        removeBankAccount => {
          if(removeBankAccount){
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.remove(SchemeBankAccountPage(srn, index)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(RemoveSchemeBankAccountPage(srn), mode, updatedAnswers))
          } else {
            Future.successful(Redirect(navigator.nextPage(RemoveSchemeBankAccountPage(srn), mode, request.userAnswers)))
          }
        }
      )
  }

  private def withBankAccountAtIndex(srn: Srn, index: Max10)(f: BankAccount => Result)(implicit request: DataRequest[_]): Result =
    request.userAnswers.get(SchemeBankAccountPage(srn, index)) match {
      case Some(bankAccount) => f(bankAccount)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

}

object RemoveSchemeBankAccountController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeSchemeBankAccount.error.required"
  )

  def viewModel(srn: Srn, index: Max10, bankAccount: BankAccount, mode: Mode): YesNoPageViewModel = YesNoPageViewModel(
    SimpleMessage("removeSchemeBankAccount.title", bankAccount.bankName, bankAccount.accountNumber),
    SimpleMessage("removeSchemeBankAccount.heading", bankAccount.bankName, bankAccount.accountNumber),
    controllers.routes.RemoveSchemeBankAccountController.onSubmit(srn, index, mode)
  )
}