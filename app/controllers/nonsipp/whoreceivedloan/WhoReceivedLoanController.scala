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

package controllers.nonsipp.whoreceivedloan

import controllers.actions._
import controllers.nonsipp.whoreceivedloan.WhoReceivedLoanController._
import forms.RadioListFormProvider
import models.ReceivedLoanType.{Individual, Other, UKCompany, UKPartnership}
import models.SchemeId.Srn
import models.{NormalMode, ReceivedLoanType}
import navigation.Navigator
import pages.nonsipp.whoreceivedloan.WhoReceivedLoanPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhoReceivedLoanController @Inject()(
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

  private val form = WhoReceivedLoanController.form(formProvider)

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Ok(
      view(
        form.fromUserAnswers(WhoReceivedLoanPage(srn)),
        viewModel(srn)
      )
    )
  }

  def onSubmit(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn)))),
        answer => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(WhoReceivedLoanPage(srn), answer))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(WhoReceivedLoanPage(srn), NormalMode, updatedAnswers))
        }
      )
  }
}

object WhoReceivedLoanController {

  def form(formProvider: RadioListFormProvider): Form[ReceivedLoanType] = formProvider(
    "whoReceivedLoan.error.required"
  )

  private val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent"), Individual.name),
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent1"), UKCompany.name),
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent2"), UKPartnership.name),
      RadioListRowViewModel(Message("whoReceivedLoan.pageContent3"), Other.name)
    )

  def viewModel(srn: Srn): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("whoReceivedLoan.title"),
      Message("whoReceivedLoan.heading"),
      RadioListViewModel(
        None,
        radioListItems
      ),
      routes.WhoReceivedLoanController.onSubmit(srn)
    )
}
