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
import controllers.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.ConditionalYesNo._
import models.SchemeId.Srn
import models.{CheckMode, ConditionalYesNo, Mode, NormalMode, Security}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{LoansCYAPage, SecurityGivenForLoanPage}
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

class SecurityGivenForLoanController @Inject()(
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

  private val form = SecurityGivenForLoanController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      {
        val preparedForm = request.userAnswers.fillForm(SecurityGivenForLoanPage(srn, index, mode), form)
        Ok(view(preparedForm, viewModel(srn, index, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode))))
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(SecurityGivenForLoanPage(srn, index, mode), ConditionalYesNo(value))
                )
              _ <- saveService.save(updatedAnswers)
            } yield {
              mode match {
                case CheckMode =>
                  Redirect(navigator.nextPage(LoansCYAPage(srn, index, mode), mode, updatedAnswers))
                case NormalMode =>
                  Redirect(navigator.nextPage(SecurityGivenForLoanPage(srn, index, mode), mode, updatedAnswers))

              }
            }
        )
  }
}

object SecurityGivenForLoanController {
  def form(formProvider: YesNoPageFormProvider) = formProvider.conditionalYes[Security](
    "securityGivenForLoan.securityGiven.error.required",
    mappingYes = Mappings
      .security(
        "securityGivenForLoan.securityGiven.yes.conditional.error.required",
        "securityGivenForLoan.securityGiven.yes.conditional.error.invalid",
        "securityGivenForLoan.securityGiven.yes.conditional.error.length"
      )
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "securityGivenForLoan.securityGiven.title",
      Message("securityGivenForLoan.securityGiven.heading"),
      page = ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            "securityGivenForLoan.securityGiven.yes.conditional",
            Option(Message("securityGivenForLoan.securityGiven.yes.hint")),
            FieldType.Textarea
          ),
        no = YesNoViewModel.Unconditional
      ),
      routes.SecurityGivenForLoanController.onSubmit(srn, index, mode)
    )
}
