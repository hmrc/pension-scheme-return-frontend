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

package controllers.nonsipp.sharesdisposal

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.{Max50, Max5000}
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.{CompanyBuyerCrnPage, CompanyBuyerNamePage}
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Crn, Mode}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class CompanyBuyerCrnController @Inject()(
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
  val form: Form[Either[String, Crn]] = CompanyBuyerCrnController.form(formProvider)
  def onPageLoad(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(CompanyBuyerNamePage(srn, index, disposalIndex)).sync { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(CompanyBuyerCrnPage(srn, index, disposalIndex), form)
        Ok(
          view(
            preparedForm,
            CompanyBuyerCrnController.viewModel(srn, index, disposalIndex, mode, companyName)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(CompanyBuyerNamePage(srn, index, disposalIndex)).async { companyName =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      CompanyBuyerCrnController
                        .viewModel(srn, index, disposalIndex, mode, companyName)
                    )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(CompanyBuyerCrnPage(srn, index, disposalIndex), ConditionalYesNo(value))
                )
              nextPage = navigator.nextPage(CompanyBuyerCrnPage(srn, index, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object CompanyBuyerCrnController {

  private val noFormErrors = InputFormErrors.textArea(
    "companyBuyerCrn.no.conditional.error.required",
    "error.textarea.invalid",
    "companyBuyerCrn.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] =
    formProvider.conditional(
      "companyBuyerCrn.error.required",
      mappingNo = Mappings.input(noFormErrors),
      mappingYes = Mappings.crn(
        "companyBuyerCrn.yes.conditional.error.required",
        "companyBuyerCrn.yes.conditional.error.invalid",
        "companyBuyerCrn.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    companyName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "companyBuyerCrn.title",
      Message("companyBuyerCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            Message("companyBuyerCrn.yes.conditional", companyName),
            Some(Message("companyBuyerCrn.yes.conditional.hint")),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(Message("companyBuyerCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint("companyBuyerCrn.hint"),
      routes.CompanyBuyerCrnController.onSubmit(srn, index, disposalIndex, mode)
    )
}
