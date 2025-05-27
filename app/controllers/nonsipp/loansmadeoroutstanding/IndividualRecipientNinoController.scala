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

package controllers.nonsipp.loansmadeoroutstanding

import services.SaveService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.Max5000
import utils.IntUtils.IntOpts
import controllers.actions._
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Mode}
import pages.nonsipp.loansmadeoroutstanding.{IndividualRecipientNamePage, IndividualRecipientNinoPage}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import viewmodels.implicits._
import controllers.nonsipp.loansmadeoroutstanding.IndividualRecipientNinoController._
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IndividualRecipientNinoController @Inject()(
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

  private val form: Form[Either[String, Nino]] = IndividualRecipientNinoController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(IndividualRecipientNamePage(srn, index.refined)).sync { individualName =>
        val preparedForm = request.userAnswers.fillForm(IndividualRecipientNinoPage(srn, index.refined), form)
        Ok(view(preparedForm, viewModel(srn, index.refined, individualName, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(IndividualRecipientNamePage(srn, index.refined)).async { individualName =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index.refined, individualName, mode))))
            },
          value =>
            for {
              updatedAnswers <- request.userAnswers
                .set(IndividualRecipientNinoPage(srn, index.refined), ConditionalYesNo(value))
                .mapK
              nextPage = navigator.nextPage(IndividualRecipientNinoPage(srn, index.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }
}

object IndividualRecipientNinoController {

  private val noFormErrors = InputFormErrors.textArea(
    "individualRecipientNino.no.conditional.error.required",
    "error.textarea.invalid",
    "individualRecipientNino.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Nino]] = formProvider.conditional(
    "individualRecipientNino.error.required",
    mappingNo = Mappings.input(noFormErrors),
    mappingYes = Mappings.nino(
      "individualRecipientNino.yes.conditional.error.required",
      "individualRecipientNino.yes.conditional.error.invalid"
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "individualRecipientNino.title",
      Message("individualRecipientNino.heading", individualName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("individualRecipientNino.yes.conditional", individualName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("individualRecipientNino.no.conditional", individualName), FieldType.Textarea)
      ).withHint("individualRecipientNino.hint"),
      routes.IndividualRecipientNinoController.onSubmit(srn, index.value, mode)
    )
}
