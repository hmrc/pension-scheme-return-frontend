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

import config.Refined.Max9999999
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.IsMemberOrConnectedPartyController._
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.{MemberOrConnectedParty, Mode}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{IndividualRecipientNamePage, IsMemberOrConnectedPartyPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IsMemberOrConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  formProvider: RadioListFormProvider,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = IsMemberOrConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(IndividualRecipientNamePage(srn, index)).sync { individualName =>
        Ok(
          view(
            form.fromUserAnswers(IsMemberOrConnectedPartyPage(srn, index)),
            viewModel(srn, index, individualName, mode)
          )
        )
      }
  }

  def onSubmit(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.usingAnswer(IndividualRecipientNamePage(srn, index)).async { individualName =>
              Future.successful(
                BadRequest(view(errors, viewModel(srn, index, individualName, mode)))
              )
            },
          success =>
            for {
              userAnswers <- Future.fromTry(request.userAnswers.set(IsMemberOrConnectedPartyPage(srn, index), success))
              _ <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(IsMemberOrConnectedPartyPage(srn, index), mode, userAnswers))
            }
        )
  }
}

object IsMemberOrConnectedPartyController {

  def form(formProvider: RadioListFormProvider): Form[MemberOrConnectedParty] =
    formProvider("isMemberOrConnectedParty.error.required")

  def viewModel(
    srn: Srn,
    index: Max9999999,
    individualName: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "isMemberOrConnectedParty.title",
      Message("isMemberOrConnectedParty.heading", individualName),
      List(
        RadioListRowViewModel("isMemberOrConnectedParty.option1", MemberOrConnectedParty.Member.name),
        RadioListRowViewModel("isMemberOrConnectedParty.option2", MemberOrConnectedParty.ConnectedParty.name),
        RadioListRowDivider.Or,
        RadioListRowViewModel("isMemberOrConnectedParty.option3", MemberOrConnectedParty.Neither.name)
      ),
      routes.IsMemberOrConnectedPartyController.onSubmit(srn, index, mode)
    )
}
