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

import config.Refined.Max9999999
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.CompanyRecipientCrnController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.SchemeId.Srn
import models.{ConditionalYesNo, Crn, Mode}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientCrnPage, CompanyRecipientNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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

  private val form: Form[Either[String, Crn]] = CompanyRecipientCrnController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(CompanyRecipientNamePage(srn, index)).sync { companyName =>
        val preparedForm = request.userAnswers.fillForm(CompanyRecipientCrnPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, companyName, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(CompanyRecipientNamePage(srn, index)).async { companyName =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, companyName, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CompanyRecipientCrnPage(srn, index), ConditionalYesNo(value)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(CompanyRecipientCrnPage(srn, index), mode, updatedAnswers))
        )
  }
}

object CompanyRecipientCrnController {
  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] = formProvider.conditional(
    "companyRecipientCrn.error.required",
    requiredNoKey = "companyRecipientCrn.no.conditional.error.required",
    invalidNoKey = "companyRecipientCrn.no.conditional.error.invalid",
    maxLengthNoKey = "companyRecipientCrn.no.conditional.error.length",
    Mappings.crn(
      "companyRecipientCrn.yes.conditional.error.required",
      "companyRecipientCrn.yes.conditional.error.invalid",
      "companyRecipientCrn.yes.conditional.error.length"
    )
  )

  def viewModel(
    srn: Srn,
    index: Max9999999,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "companyRecipientCrn.title",
      Message("companyRecipientCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel.Conditional(Message("companyRecipientCrn.yes.conditional", companyName)),
        no = YesNoViewModel.Conditional(Message("companyRecipientCrn.no.conditional", companyName))
      ).withHint("companyRecipientCrn.hint"),
      routes.CompanyRecipientCrnController.onSubmit(srn, index, mode)
    )
}
