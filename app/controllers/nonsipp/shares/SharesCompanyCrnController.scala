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

package controllers.nonsipp.shares

import services.SaveService
import viewmodels.implicits._
import forms.mappings.Mappings
import config.RefinedTypes.Max5000
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Crn, Mode}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesCompanyCrnPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class SharesCompanyCrnController @Inject()(
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
  val form: Form[Either[String, Crn]] = SharesCompanyCrnController.form(formProvider)
  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(CompanyNameRelatedSharesPage(srn, index.refined)).sync { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(SharesCompanyCrnPage(srn, index.refined), form)
        Ok(
          view(
            preparedForm,
            SharesCompanyCrnController.viewModel(srn, index.refined, mode, companyName)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(CompanyNameRelatedSharesPage(srn, index.refined)).async { companyName =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      SharesCompanyCrnController
                        .viewModel(srn, index.refined, mode, companyName)
                    )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(SharesCompanyCrnPage(srn, index.refined), ConditionalYesNo(value))
                )
              nextPage = navigator.nextPage(SharesCompanyCrnPage(srn, index.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object SharesCompanyCrnController {

  private val noFormErrors = InputFormErrors.textArea(
    "sharesCompanyCrn.no.conditional.error.required",
    "error.textarea.invalid",
    "sharesCompanyCrn.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] =
    formProvider.conditional(
      "sharesCompanyCrn.error.required",
      mappingNo = Mappings.input(noFormErrors),
      mappingYes = Mappings.crn(
        "sharesCompanyCrn.yes.conditional.error.required",
        "sharesCompanyCrn.yes.conditional.error.invalid",
        "sharesCompanyCrn.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    companyName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "sharesCompanyCrn.title",
      Message("sharesCompanyCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            Message("sharesCompanyCrn.yes.conditional", companyName),
            Some(Message("sharesCompanyCrn.yes.conditional.hint")),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(Message("sharesCompanyCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint("sharesCompanyCrn.hint"),
      routes.SharesCompanyCrnController.onSubmit(srn, index, mode)
    )
}
