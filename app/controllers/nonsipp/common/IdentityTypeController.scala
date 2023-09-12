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

import config.Refined.Max5000
import controllers.actions._
import controllers.nonsipp.common.IdentityTypeController._
import forms.RadioListFormProvider
import models.IdentityType.{Individual, Other, UKCompany, UKPartnership}
import models.SchemeId.Srn
import models.{IdentitySubject, IdentityType, Mode, UserAnswers}
import navigation.Navigator
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPage
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

  def onPageLoad(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject
  ): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val form = IdentityTypeController.form(formProvider, subject)
    Ok(
      view(
        form.fromUserAnswers(IdentityTypePage(srn, index, subject)),
        viewModel(srn, index, mode, subject, request.userAnswers)
      )
    )
  }

  def onSubmit(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = IdentityTypeController.form(formProvider, subject)
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future
            .successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode, subject, request.userAnswers)))),
        answer => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IdentityTypePage(srn, index, subject), answer))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(IdentityTypePage(srn, index, subject), mode, updatedAnswers))
        }
      )
  }
}

object IdentityTypeController {

  def form(formProvider: RadioListFormProvider, subject: IdentitySubject): Form[IdentityType] = formProvider(
    s"${subject.key}.identityType.error.required"
  )

  private def radioListItems(subject: IdentitySubject): List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent"), Individual.name),
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent1"), UKCompany.name),
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent2"), UKPartnership.name),
      RadioListRowViewModel(Message(s"${subject.key}.identityType.pageContent3"), Other.name)
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject,
    userAnswers: UserAnswers
  ): FormPageViewModel[RadioListViewModel] = {
    val text = subject match {
      case IdentitySubject.LoanRecipient => ""
      case IdentitySubject.LandOrPropertySeller => {
        userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)) match {
          case Some(value) => value.addressLine1
          case None => ""
        }
      }
    }
    FormPageViewModel(
      Message(s"${subject.key}.identityType.title"),
      Message(s"${subject.key}.identityType.heading", text),
      RadioListViewModel(
        None,
        radioListItems(subject)
      ),
      controllers.nonsipp.common.routes.IdentityTypeController
        .onSubmit(srn, index, mode, subject)
    )
  }
}
