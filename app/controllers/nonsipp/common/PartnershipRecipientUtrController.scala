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
import models.SchemeId.Srn
import models.{ConditionalYesNo, Mode, Utr}
import navigation.Navigator
import pages.nonsipp.common.PartnershipRecipientUtrPage
import pages.nonsipp.loansmadeoroutstanding.PartnershipRecipientNamePage
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

  private val form: Form[Either[String, Utr]] = PartnershipRecipientUtrController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(PartnershipRecipientNamePage(srn, index)).sync { partnershipRecipientName =>
        val preparedForm = request.userAnswers.fillForm(PartnershipRecipientUtrPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, partnershipRecipientName, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(PartnershipRecipientNamePage(srn, index)).async { partnershipRecipientName =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, partnershipRecipientName, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(PartnershipRecipientUtrPage(srn, index), ConditionalYesNo(value)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(PartnershipRecipientUtrPage(srn, index), mode, updatedAnswers))
        )
  }
}

object PartnershipRecipientUtrController {
  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Utr]] = formProvider.conditional(
    "partnershipRecipientUtr.error.required",
    mappingNo = Mappings.textArea(
      "partnershipRecipientUtr.no.conditional.error.required",
      "partnershipRecipientUtr.no.conditional.error.invalid",
      "partnershipRecipientUtr.no.conditional.error.length"
    ),
    mappingYes = Mappings.utr(
      "partnershipRecipientUtr.yes.conditional.error.required",
      "partnershipRecipientUtr.yes.conditional.error.invalid"
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    partnershipRecipientName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "partnershipRecipientUtr.title",
      Message("partnershipRecipientUtr.heading", partnershipRecipientName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("partnershipRecipientUtr.yes.conditional", partnershipRecipientName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("partnershipRecipientUtr.no.conditional", partnershipRecipientName), FieldType.Textarea)
      ),
      routes.PartnershipRecipientUtrController.onSubmit(srn, index, mode)
    )
}
