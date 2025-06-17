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

package controllers.nonsipp.otherassetsdisposal

import services.SaveService
import pages.nonsipp.otherassetsdisposal.{AssetCompanyBuyerCrnPage, CompanyNameOfAssetBuyerPage}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.{Max50, Max5000}
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.actions.IdentifyAndRequireData
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

class AssetCompanyBuyerCrnController @Inject() (
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
  val form: Form[Either[String, Crn]] = AssetCompanyBuyerCrnController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex)).sync { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(AssetCompanyBuyerCrnPage(srn, assetIndex, disposalIndex), form)
        Ok(
          view(
            preparedForm,
            AssetCompanyBuyerCrnController.viewModel(srn, assetIndex, disposalIndex, mode, companyName)
          )
        )
      }
    }

  def onSubmit(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex)).async { companyName =>
              Future
                .successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      AssetCompanyBuyerCrnController
                        .viewModel(srn, assetIndex, disposalIndex, mode, companyName)
                    )
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(AssetCompanyBuyerCrnPage(srn, assetIndex, disposalIndex), ConditionalYesNo(value))
                )
              nextPage = navigator
                .nextPage(AssetCompanyBuyerCrnPage(srn, assetIndex, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, assetIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object AssetCompanyBuyerCrnController {

  private val noFormErrors = InputFormErrors.textArea(
    "assetCompanyBuyerCrn.no.conditional.error.required",
    "error.textarea.invalid",
    "assetCompanyBuyerCrn.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Crn]] =
    formProvider.conditional(
      "assetCompanyBuyerCrn.error.required",
      mappingNo = Mappings.input(noFormErrors),
      mappingYes = Mappings.crn(
        "assetCompanyBuyerCrn.yes.conditional.error.required",
        "assetCompanyBuyerCrn.yes.conditional.error.invalid",
        "assetCompanyBuyerCrn.yes.conditional.error.length"
      )
    )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    companyName: String
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "assetCompanyBuyerCrn.title",
      Message("assetCompanyBuyerCrn.heading", companyName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            Message("assetCompanyBuyerCrn.yes.conditional", companyName),
            Some(Message("assetCompanyBuyerCrn.yes.conditional.hint")),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(Message("assetCompanyBuyerCrn.no.conditional", companyName), FieldType.Textarea)
      ).withHint("assetCompanyBuyerCrn.hint"),
      routes.AssetCompanyBuyerCrnController.onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
