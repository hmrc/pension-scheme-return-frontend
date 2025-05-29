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
import views.html.MoneyViewWithDescription
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
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
  view: MoneyViewWithDescription
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = FeesCommissionsWagesSalariesController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(FeesCommissionsWagesSalariesPage(srn, mode), form)
    Ok(view(preparedForm, viewModel(srn, form, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                viewModel(srn, form, mode)
              )
            )
          ),
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
              case ViewOnlyMode =>
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
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

  def viewModel(
    srn: Srn,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      title = "feesCommissionsWagesSalaries.title",
      heading = "feesCommissionsWagesSalaries.heading",
      page = SingleQuestion(
        form,
        QuestionField.currency("feesCommissionsWagesSalaries.label", Some("feesCommissionsWagesSalaries.hint"))
      ),
      onSubmit = routes.FeesCommissionsWagesSalariesController.onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage("feesCommissionsWagesSalaries.paragraph1") ++
        ParagraphMessage("feesCommissionsWagesSalaries.paragraph2") ++
        ListMessage(
          ListType.Bullet,
          "feesCommissionsWagesSalaries.bullet1",
          "feesCommissionsWagesSalaries.bullet2"
        ) ++
        ParagraphMessage("feesCommissionsWagesSalaries.paragraph3") ++
        ListMessage(
          ListType.Bullet,
          "feesCommissionsWagesSalaries.bullet3",
          "feesCommissionsWagesSalaries.bullet4"
        ) ++
        ParagraphMessage("feesCommissionsWagesSalaries.paragraph4") ++
        ParagraphMessage("feesCommissionsWagesSalaries.paragraph5")
    )
}
