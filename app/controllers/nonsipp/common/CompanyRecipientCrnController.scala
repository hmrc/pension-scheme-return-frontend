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
import controllers.nonsipp.common.CompanyRecipientCrnController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.SchemeId.Srn
import models.{ConditionalYesNo, Crn, IdentitySubject, Mode, UserAnswers}
import navigation.Navigator
import pages.nonsipp.common.CompanyRecipientCrnPage
import pages.nonsipp.landorproperty.CompanySellerNamePage
import pages.nonsipp.loansmadeoroutstanding.CompanyRecipientNamePage
import pages.nonsipp.shares.CompanyNameOfSharesSellerPage
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

class CompanyRecipientCrnController @Inject()(
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
          val form: Form[Either[String, Crn]] = CompanyRecipientCrnController.form(formProvider, subject)
          val preparedForm = request.userAnswers.fillForm(CompanyRecipientCrnPage(srn, index, subject), form)
          Ok(view(preparedForm, viewModel(srn, index, mode, subject, request.userAnswers)))
      }
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val form: Form[Either[String, Crn]] = CompanyRecipientCrnController.form(formProvider, subject)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode, subject, request.userAnswers)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CompanyRecipientCrnPage(srn, index, subject), ConditionalYesNo(value)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(CompanyRecipientCrnPage(srn, index, subject), mode, updatedAnswers))
        )
    }
}

object CompanyRecipientCrnController {

  private def inputFormErrors(subjectKey: String) = InputFormErrors.textArea(
    s"${subjectKey}.companyRecipientCrn.no.conditional.error.required",
    s"${subjectKey}.companyRecipientCrn.no.conditional.error.invalid",
    s"${subjectKey}.companyRecipientCrn.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider, subject: IdentitySubject): Form[Either[String, Crn]] =
    formProvider.conditional(
      s"${subject.key}.companyRecipientCrn.error.required",
      mappingNo = Mappings.input(inputFormErrors(subject.key)),
      mappingYes = Mappings.crn(
        s"${subject.key}.companyRecipientCrn.yes.conditional.error.required",
        s"${subject.key}.companyRecipientCrn.yes.conditional.error.invalid",
        s"${subject.key}.companyRecipientCrn.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject,
    userAnswers: UserAnswers
  ): FormPageViewModel[ConditionalYesNoPageViewModel] = {
    val companyName = subject match {
      case IdentitySubject.LoanRecipient =>
        userAnswers.get(CompanyRecipientNamePage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case IdentitySubject.LandOrPropertySeller =>
        userAnswers.get(CompanySellerNamePage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case IdentitySubject.SharesSeller =>
        userAnswers.get(CompanyNameOfSharesSellerPage(srn, index)) match {
          case Some(value) => value
          case None => ""
        }
      case _ => ""
    }
    FormPageViewModel[ConditionalYesNoPageViewModel](
      Message(s"${subject.key}.companyRecipientCrn.title"),
      Message(s"${subject.key}.companyRecipientCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message(s"${subject.key}.companyRecipientCrn.yes.conditional", companyName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message(s"${subject.key}.companyRecipientCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint(s"${subject.key}.companyRecipientCrn.hint"),
      controllers.nonsipp.common.routes.CompanyRecipientCrnController.onSubmit(srn, index, mode, subject)
    )
  }
}
