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
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.SchemeId.Srn
import models.{ConditionalYesNo, IdentitySubject, Mode, UserAnswers, Utr}
import navigation.Navigator

import pages.nonsipp.landorproperty.PartnershipSellerNamePage
import pages.nonsipp.landorproperty.PartnershipUTRPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView
import viewmodels.implicits._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PartnershipUTRController @Inject()(
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
      val form = PartnershipUTRController.form(formProvider, subject)
      val preparedForm = request.userAnswers.fillForm(PartnershipUTRPage(srn, index), form)
      Ok(view(preparedForm, PartnershipUTRController.viewModel(srn, index, mode, subject, request.userAnswers)))
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode, subject: IdentitySubject): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val form: Form[Either[String, Utr]] = PartnershipUTRController.form(formProvider, subject)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(
                    formWithErrors,
                    PartnershipUTRController.viewModel(srn, index, mode, subject, request.userAnswers)
                  )
                )
              ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(PartnershipUTRPage(srn, index), ConditionalYesNo(value)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(PartnershipUTRPage(srn, index), mode, updatedAnswers))
        )
    }
}

object PartnershipUTRController {

  def form(formProvider: YesNoPageFormProvider, subject: IdentitySubject): Form[Either[String, Utr]] =
    formProvider.conditional(
      s"${subject.key}.companyRecipientCrn.error.required",
      mappingNo = Mappings.textArea(
        s"${subject.key}.companyRecipientCrn.no.conditional.error.required",
        s"${subject.key}.companyRecipientCrn.no.conditional.error.invalid",
        s"${subject.key}.companyRecipientCrn.no.conditional.error.length"
      ),
      mappingYes = Mappings.utr(
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
    val partnershipName = userAnswers.get(PartnershipSellerNamePage(srn, index)) match {
      case Some(value) => value
      case None => ""
    }

    FormPageViewModel[ConditionalYesNoPageViewModel](
      Message(s"${subject.key}.companyRecipientCrn.title"),
      Message(s"${subject.key}.companyRecipientCrn.heading", partnershipName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            Message(s"${subject.key}.companyRecipientCrn.yes.conditional", partnershipName.toString),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(
            Message(s"${subject.key}.companyRecipientCrn.no.conditional", partnershipName),
            FieldType.Textarea
          )
      ).withHint(s"${subject.key}.companyRecipientCrn.hint"),
      controllers.nonsipp.common.routes.PartnershipUTRController.onSubmit(srn, index, mode, subject)
    )
  }
}
