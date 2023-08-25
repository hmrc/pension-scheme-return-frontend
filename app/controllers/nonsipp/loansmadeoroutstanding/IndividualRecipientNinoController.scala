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

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.Max5000
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.IndividualRecipientNinoController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.SchemeId.Srn
import models.{CheckMode, ConditionalYesNo, Mode, NormalMode}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{
  CompanyRecipientNamePage,
  IndividualRecipientNamePage,
  IndividualRecipientNinoPage,
  LoansCYAPage
}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(IndividualRecipientNamePage(srn, index, mode)).sync { individualName =>
        val preparedForm = request.userAnswers.fillForm(IndividualRecipientNinoPage(srn, index, mode), form)
        Ok(view(preparedForm, viewModel(srn, index, individualName, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(IndividualRecipientNamePage(srn, index, mode)).async { individualName =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, individualName, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(IndividualRecipientNinoPage(srn, index, mode), ConditionalYesNo(value))
                )
              _ <- saveService.save(updatedAnswers)
            } yield {

              mode match {
                case CheckMode => {
                  (
                    updatedAnswers.get(IndividualRecipientNinoPage(srn, index, mode)),
                    request.userAnswers.get(IndividualRecipientNinoPage(srn, index, mode))
                  ) match {
                    case (Some(newAnswer), Some(previousAnswer)) => {
                      if (newAnswer == previousAnswer) {
                        Redirect(navigator.nextPage(LoansCYAPage(srn, index, mode), mode, updatedAnswers))
                      } else {
                        Redirect(
                          navigator.nextPage(IndividualRecipientNinoPage(srn, index, mode), CheckMode, updatedAnswers)
                        )
                      }
                    }
                  }
                }
                case NormalMode =>
                  Redirect(navigator.nextPage(IndividualRecipientNinoPage(srn, index, mode), mode, updatedAnswers))
              }

            }
        )
  }
}

object IndividualRecipientNinoController {
  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Nino]] = formProvider.conditional(
    "individualRecipientNino.error.required",
    mappingNo = Mappings.textArea(
      "individualRecipientNino.no.conditional.error.required",
      "individualRecipientNino.no.conditional.error.invalid",
      "individualRecipientNino.no.conditional.error.length"
    ),
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
      ),
      routes.IndividualRecipientNinoController.onSubmit(srn, index, mode)
    )
}
