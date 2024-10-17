/*
 * Copyright 2024 HM Revenue & Customs
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

import services.SaveService
import controllers.nonsipp.common.OtherRecipientDetailsController.viewModel
import viewmodels.implicits._
import utils.FormUtils._
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import controllers.actions._
import navigation.Navigator
import forms.RecipientDetailsFormProvider
import models._
import pages.nonsipp.common.OtherRecipientDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.RecipientDetailsView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RecipientDetailsViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherRecipientDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: RecipientDetailsFormProvider,
  view: RecipientDetailsView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      subject match {
        case IdentitySubject.Unknown => Redirect(controllers.routes.UnauthorisedController.onPageLoad())
        case _ =>
          val form = OtherRecipientDetailsController.form(formProvider, subject)
          Ok(
            view(
              form.fromUserAnswers(OtherRecipientDetailsPage(srn, index, subject)),
              viewModel(srn, index, mode, subject, request.userAnswers)
            )
          )
      }
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val form = OtherRecipientDetailsController.form(formProvider, subject)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, viewModel(srn, index, mode, subject, request.userAnswers)))
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(OtherRecipientDetailsPage(srn, index, subject), answer))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(OtherRecipientDetailsPage(srn, index, subject), mode, updatedAnswers))
          }
        )
    }
}

object OtherRecipientDetailsController {
  def form(formProvider: RecipientDetailsFormProvider, subject: IdentitySubject): Form[RecipientDetails] = formProvider(
    s"${subject.key}.otherRecipientDetails.name.error.required",
    s"${subject.key}.otherRecipientDetails.name.error.invalid",
    s"${subject.key}.otherRecipientDetails.name.error.length",
    s"${subject.key}.otherRecipientDetails.description.error.required",
    s"${subject.key}.otherRecipientDetails.description.error.invalid",
    s"${subject.key}.otherRecipientDetails.description.error.length"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject,
    userAnswers: UserAnswers
  ): FormPageViewModel[RecipientDetailsViewModel] = {
    val text = subject match {
      case IdentitySubject.LoanRecipient => ""
      case IdentitySubject.LandOrPropertySeller =>
        userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)) match {
          case Some(value) => value.addressLine1
          case None => ""
        }
      case IdentitySubject.SharesSeller =>
        userAnswers.get(CompanyNameRelatedSharesPage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case _ => ""
    }
    FormPageViewModel(
      Message(s"${subject.key}.otherRecipientDetails.title"),
      Message(s"${subject.key}.otherRecipientDetails.heading", text),
      RecipientDetailsViewModel(
        Message(s"${subject.key}.otherRecipientDetails.name"),
        Message(s"${subject.key}.otherRecipientDetails.description")
      ),
      routes.OtherRecipientDetailsController.onSubmit(srn, index, mode, subject)
    )
  }
}
