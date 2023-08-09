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

package controllers.nonsipp.common

import config.Refined.Max9999999
import controllers.actions._
import controllers.nonsipp.common.IdentityTypeController._
import forms.RadioListFormProvider
import models.IdentityType.{Individual, Other, UKCompany, UKPartnership}
import models.SchemeId.Srn
import models.{IdentitySubject, IdentityType, Mode, NormalMode}
import navigation.Navigator
import pages.nonsipp.common.IdentityTypePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IdentityTypeController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = IdentityTypeController.form(formProvider)

  def onPageLoad(
    srn: Srn,
    index: Max9999999,
    mode: Mode,
    subject: IdentitySubject = IdentitySubject.Unknown
  ): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Ok(
      view(
        form.fromUserAnswers(IdentityTypePage(srn, index)),
        viewModel(srn, index, mode)
      )
    )
  }

  def onSubmit(
    srn: Srn,
    index: Max9999999,
    mode: Mode,
    subject: IdentitySubject = IdentitySubject.Unknown
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
        answer => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IdentityTypePage(srn, index), answer))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(IdentityTypePage(srn, index), NormalMode, updatedAnswers))
        }
      )
  }
}

object IdentityTypeController {

  def form(formProvider: RadioListFormProvider): Form[IdentityType] = formProvider(
    "whoReceivedLoan.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent"), Individual.name),
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent1"), UKCompany.name),
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent2"), UKPartnership.name),
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent3"), Other.name)
    )

  def viewModel(srn: Srn, index: Max9999999, mode: Mode): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("whoReceivedLoan.title"),
      Message("whoReceivedLoan.heading"),
      RadioListViewModel(
        None,
        radioListItems
      ),
      // TODO:
      controllers.nonsipp.common.routes.IdentityTypeController
        .onSubmit(srn, index, mode, IdentitySubject.LoanRecipient)
    )
}
