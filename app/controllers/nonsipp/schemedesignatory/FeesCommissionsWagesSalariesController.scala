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

package controllers.nonsipp.schemedesignatory

import services.SaveService
import pages.nonsipp.schemedesignatory.{FeesCommissionsWagesSalariesPage, FinancialDetailsCheckYourAnswersPage}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import controllers.nonsipp.schemedesignatory.FeesCommissionsWagesSalariesController._
import config.Constants.maxMoneyValue
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models._
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import views.html.MoneyView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class FeesCommissionsWagesSalariesController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = FeesCommissionsWagesSalariesController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(FeesCommissionsWagesSalariesPage(srn, mode), form)
    Ok(view(viewModel(srn, request.schemeDetails.schemeName, preparedForm, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(viewModel(srn, request.schemeDetails.schemeName, formWithErrors, mode)))),
        value =>
          for {
            updatedAnswers <- Future
              .fromTry(request.userAnswers.set(FeesCommissionsWagesSalariesPage(srn, mode), value))
            _ <- saveService.save(updatedAnswers)
          } yield {
            mode match {
              case CheckMode =>
                Redirect(navigator.nextPage(FinancialDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
              case NormalMode =>
                Redirect(navigator.nextPage(FeesCommissionsWagesSalariesPage(srn, mode), mode, updatedAnswers))
            }
          }
      )
  }
}

object FeesCommissionsWagesSalariesController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "feesCommissionsWagesSalaries.error.required",
      "feesCommissionsWagesSalaries.error.invalid",
      (maxMoneyValue, "feesCommissionsWagesSalaries.error.tooLarge")
    )
  )

  def viewModel(srn: Srn, schemeName: String, form: Form[Money], mode: Mode): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "feesCommissionsWagesSalaries.title",
      Message("feesCommissionsWagesSalaries.heading", schemeName),
      SingleQuestion(
        form,
        QuestionField.input(Empty, Some("feesCommissionsWagesSalaries.hint"))
      ),
      routes.FeesCommissionsWagesSalariesController.onSubmit(srn, mode)
    )
}
