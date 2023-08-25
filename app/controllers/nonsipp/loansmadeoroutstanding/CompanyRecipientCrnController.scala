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
import controllers.nonsipp.loansmadeoroutstanding.CompanyRecipientCrnController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.SchemeId.Srn
import models.{CheckMode, ConditionalYesNo, Crn, Mode, NormalMode}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientCrnPage, CompanyRecipientNamePage, LoansCYAPage}
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

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(CompanyRecipientNamePage(srn, index, mode)).sync { companyName =>
        val preparedForm = request.userAnswers.fillForm(CompanyRecipientCrnPage(srn, index, mode), form)
        Ok(view(preparedForm, viewModel(srn, index, companyName, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(CompanyRecipientNamePage(srn, index, mode)).async { companyName =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, companyName, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CompanyRecipientCrnPage(srn, index, mode), ConditionalYesNo(value)))
              _ <- saveService.save(updatedAnswers)
            } yield {
              mode match {
                case CheckMode => {
                  (
                    updatedAnswers.get(CompanyRecipientCrnPage(srn, index, mode)),
                    request.userAnswers.get(CompanyRecipientCrnPage(srn, index, mode))
                  ) match {
                    case (Some(newAnswer), Some(previousAnswer)) => {
                      if (newAnswer == previousAnswer) {
                        Redirect(navigator.nextPage(LoansCYAPage(srn, index, mode), mode, updatedAnswers))
                      } else {
                        Redirect(
                          navigator.nextPage(CompanyRecipientCrnPage(srn, index, mode), CheckMode, updatedAnswers)
                        )
                      }
                    }
                  }
                }
                case NormalMode =>
                  Redirect(navigator.nextPage(CompanyRecipientCrnPage(srn, index, mode), mode, updatedAnswers))
              }

            }
        )
  }
}

object CompanyRecipientCrnController {

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] = formProvider.conditional(
    "companyRecipientCrn.error.required",
    mappingNo = Mappings.textArea(
      "companyRecipientCrn.no.conditional.error.required",
      "companyRecipientCrn.no.conditional.error.invalid",
      "companyRecipientCrn.no.conditional.error.length"
    ),
    mappingYes = Mappings.crn(
      "companyRecipientCrn.yes.conditional.error.required",
      "companyRecipientCrn.yes.conditional.error.invalid",
      "companyRecipientCrn.yes.conditional.error.length"
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "companyRecipientCrn.title",
      Message("companyRecipientCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel.Conditional(Message("companyRecipientCrn.yes.conditional", companyName), FieldType.Input),
        no = YesNoViewModel.Conditional(Message("companyRecipientCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint("companyRecipientCrn.hint"),
      routes.CompanyRecipientCrnController.onSubmit(srn, index, mode)
    )
}
