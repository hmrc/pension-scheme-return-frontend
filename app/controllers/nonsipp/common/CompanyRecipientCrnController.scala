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
import viewmodels.implicits._
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.common.CompanyRecipientCrnPage
import pages.nonsipp.loansmadeoroutstanding.CompanyRecipientNamePage
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import pages.nonsipp.shares.CompanyNameOfSharesSellerPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import pages.nonsipp.otherassetsheld.CompanyNameOfOtherAssetSellerPage
import config.RefinedTypes.Max5000
import controllers.nonsipp.common.CompanyRecipientCrnController._
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty.CompanySellerNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

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

  def onPageLoad(srn: Srn, index: Int, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      subject match {
        case IdentitySubject.Unknown => Redirect(controllers.routes.UnauthorisedController.onPageLoad())
        case _ =>
          val form: Form[Either[String, Crn]] = CompanyRecipientCrnController.form(formProvider, subject)
          val preparedForm = request.userAnswers.fillForm(CompanyRecipientCrnPage(srn, index, subject), form)
          Ok(view(preparedForm, viewModel(srn, index, mode, subject, request.userAnswers)))
      }
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
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
              updatedAnswers <- request.userAnswers
                .set(CompanyRecipientCrnPage(srn, index, subject), ConditionalYesNo(value))
                .mapK
              nextPage = navigator.nextPage(CompanyRecipientCrnPage(srn, index, subject), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage, subject)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object CompanyRecipientCrnController {

  private def inputFormErrors(subjectKey: String) = InputFormErrors.textArea(
    s"$subjectKey.companyRecipientCrn.no.conditional.error.required",
    "error.textarea.invalid",
    s"$subjectKey.companyRecipientCrn.no.conditional.error.length"
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
      case IdentitySubject.OtherAssetSeller =>
        userAnswers.get(CompanyNameOfOtherAssetSellerPage(srn, index)) match {
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
          .Conditional(
            Message(s"${subject.key}.companyRecipientCrn.yes.conditional", companyName),
            Some(Message(s"${subject.key}.companyRecipientCrn.yes.conditional.hint")),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(Message(s"${subject.key}.companyRecipientCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint(s"${subject.key}.companyRecipientCrn.hint"),
      controllers.nonsipp.common.routes.CompanyRecipientCrnController.onSubmit(srn, index, mode, subject)
    )
  }
}
