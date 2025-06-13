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
import viewmodels.implicits._
import models.ConditionalYesNo._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.Max5000
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Mode, Security}
import pages.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanPage
import play.api.data.Form
import controllers.nonsipp.loansmadeoroutstanding.SecurityGivenForLoanController._
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SecurityGivenForLoanController @Inject() (
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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(SecurityGivenForLoanPage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- request.userAnswers
                .set(SecurityGivenForLoanPage(srn, index), ConditionalYesNo(value))
                .mapK
              nextPage = navigator.nextPage(SecurityGivenForLoanPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }
}

object SecurityGivenForLoanController {
  def form(formProvider: YesNoPageFormProvider): Form[Either[Unit, Security]] = formProvider.conditionalYes[Security](
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
