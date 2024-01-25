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
import controllers.nonsipp.common.PartnershipRecipientUtrController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.SchemeId.Srn
import models.{ConditionalYesNo, IdentitySubject, Mode, UserAnswers, Utr}
import navigation.Navigator
import pages.nonsipp.common.PartnershipRecipientUtrPage
import pages.nonsipp.landorproperty.PartnershipSellerNamePage
import pages.nonsipp.loansmadeoroutstanding.PartnershipRecipientNamePage
import pages.nonsipp.shares.PartnershipShareSellerNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PartnershipRecipientUtrController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConditionalYesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      subject match {
        case IdentitySubject.Unknown => Redirect(controllers.routes.UnauthorisedController.onPageLoad())
        case _ =>
          val form: Form[Either[String, Utr]] = PartnershipRecipientUtrController.form(formProvider, subject)
          val preparedForm = request.userAnswers.fillForm(PartnershipRecipientUtrPage(srn, index, subject), form)
          Ok(view(preparedForm, viewModel(srn, index, mode, subject, request.userAnswers)))
      }
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val form: Form[Either[String, Utr]] = PartnershipRecipientUtrController.form(formProvider, subject)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode, subject, request.userAnswers)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(PartnershipRecipientUtrPage(srn, index, subject), ConditionalYesNo(value))
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(PartnershipRecipientUtrPage(srn, index, subject), mode, updatedAnswers))
        )
    }
}

object PartnershipRecipientUtrController {

  private def noFormErrors(subjectKey: String) = InputFormErrors.input(
    s"${subjectKey}.partnershipRecipientUtr.no.conditional.error.required",
    s"${subjectKey}.partnershipRecipientUtr.no.conditional.error.invalid",
    s"${subjectKey}.partnershipRecipientUtr.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider, subject: IdentitySubject): Form[Either[String, Utr]] =
    formProvider.conditional(
      s"${subject.key}.partnershipRecipientUtr.error.required",
      mappingNo = Mappings.input(noFormErrors(subject.key)),
      mappingYes = Mappings.utr(
        s"${subject.key}.partnershipRecipientUtr.yes.conditional.error.required",
        s"${subject.key}.partnershipRecipientUtr.yes.conditional.error.invalid"
      )
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject,
    userAnswers: UserAnswers
  ): FormPageViewModel[ConditionalYesNoPageViewModel] = {
    val partnershipRecipientName = subject match {
      case IdentitySubject.LoanRecipient =>
        userAnswers.get(PartnershipRecipientNamePage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case IdentitySubject.LandOrPropertySeller =>
        userAnswers.get(PartnershipSellerNamePage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case IdentitySubject.SharesSeller =>
        userAnswers.get(PartnershipShareSellerNamePage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case _ => ""
    }
    FormPageViewModel[ConditionalYesNoPageViewModel](
      s"${subject.key}.partnershipRecipientUtr.title",
      Message(s"${subject.key}.partnershipRecipientUtr.heading", partnershipRecipientName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            Message(s"${subject.key}.partnershipRecipientUtr.yes.conditional", partnershipRecipientName),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(
            Message(s"${subject.key}.partnershipRecipientUtr.no.conditional", partnershipRecipientName),
            FieldType.Textarea
          )
      ),
      routes.PartnershipRecipientUtrController.onSubmit(srn, index, mode, subject)
    )
  }
}
