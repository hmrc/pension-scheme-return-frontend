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

package controllers.nonsipp.shares

import config.Refined.Max5000
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.SchemeId.Srn
import models.{ConditionalYesNo, Crn, Mode}
import navigation.Navigator
import pages.nonsipp.shares.{CompanyNameRelatedSharesPage, SharesCompanyCrnPage}
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
  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(CompanyNameRelatedSharesPage(srn, index)).sync { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(SharesCompanyCrnPage(srn, index), form)
        Ok(
          view(
            preparedForm,
            SharesCompanyCrnController.viewModel(srn, index, mode, companyName)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(CompanyNameRelatedSharesPage(srn, index)).async { companyName =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      SharesCompanyCrnController
                        .viewModel(srn, index, mode, companyName)
                    )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(SharesCompanyCrnPage(srn, index), ConditionalYesNo(value))
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(SharesCompanyCrnPage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object SharesCompanyCrnController {

  private val noFormErrors = InputFormErrors.input(
    "sharesCompanyCrn.no.conditional.error.required",
    "sharesCompanyCrn.no.conditional.error.invalid",
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
          .Conditional(Message("sharesCompanyCrn.yes.conditional", companyName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("sharesCompanyCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint("sharesCompanyCrn.hint"),
      routes.SharesCompanyCrnController.onSubmit(srn, index, mode)
    )
}
