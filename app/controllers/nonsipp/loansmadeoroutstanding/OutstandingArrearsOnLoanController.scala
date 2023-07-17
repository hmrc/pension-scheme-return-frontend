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

import config.Constants.maxCurrencyValue
import config.Refined.Max9999999
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.OutstandingArrearsOnLoanController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{ConditionalYesNo, Mode, Money}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.OutstandingArrearsOnLoanPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class OutstandingArrearsOnLoanController @Inject()(
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

  implicit val u = new Writes[Unit] {
    override def writes(o: Unit): JsValue = JsNull
  }

  implicit val uu = new Reads[Unit] {
    override def reads(json: JsValue): JsResult[Unit] = JsSuccess(())
  }

  implicitly[Reads[Unit]]

  private val form = OutstandingArrearsOnLoanController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(OutstandingArrearsOnLoanPage(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(OutstandingArrearsOnLoanPage(srn, index), ConditionalYesNo(value)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(OutstandingArrearsOnLoanPage(srn, index), mode, updatedAnswers))
        )
  }
}

object OutstandingArrearsOnLoanController {

  def form(formProvider: YesNoPageFormProvider): Form[Either[Unit, Money]] = formProvider.conditionalYes[Money](
    "outstandingArrearsOnLoan.title",
    mappingYes = Mappings.money(
      MoneyFormErrors(
        "outstandingArrearsOnLoan.yes.error.required",
        "outstandingArrearsOnLoan.yes.error.nonNumeric",
        (maxCurrencyValue, "outstandingArrearsOnLoan.yes.error.max")
      )
    )
  )

  def viewModel(srn: Srn, index: Max9999999, mode: Mode): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "outstandingArrearsOnLoan.title",
      "outstandingArrearsOnLoan.heading",
      page = ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional("outstandingArrearsOnLoan.yes.conditional", FieldType.Currency),
        no = YesNoViewModel.Unconditional
      ),
      routes.OutstandingArrearsOnLoanController.onSubmit(srn, index, mode)
    )
}
