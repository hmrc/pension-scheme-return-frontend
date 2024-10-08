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
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import config.RefinedTypes.{Max50, Max5000}
import controllers.actions._
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Mode}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import pages.nonsipp.otherassetsdisposal.{AssetIndividualBuyerNiNumberPage, IndividualNameOfAssetBuyerPage}
import controllers.nonsipp.otherassetsdisposal.AssetIndividualBuyerNiNumberController._
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class AssetIndividualBuyerNiNumberController @Inject()(
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

  private val form: Form[Either[String, Nino]] = AssetIndividualBuyerNiNumberController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex)).sync { individualName =>
        val preparedForm =
          request.userAnswers.fillForm(AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex), form)
        Ok(view(preparedForm, viewModel(srn, assetIndex, disposalIndex, individualName, mode)))
      }

    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex)).async {
              individualName =>
                Future.successful(
                  BadRequest(
                    view(formWithErrors, viewModel(srn, assetIndex, disposalIndex, individualName, mode))
                  )
                )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(
                      AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex),
                      ConditionalYesNo(value)
                    )
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object AssetIndividualBuyerNiNumberController {

  private val noFormErrors = InputFormErrors.textArea(
    "assetIndividualBuyerNiNumber.no.conditional.error.required",
    "assetIndividualBuyerNiNumber.no.conditional.error.invalid",
    "assetIndividualBuyerNiNumber.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Nino]] = formProvider.conditional(
    "assetIndividualBuyerNiNumber.error.required",
    mappingNo = Mappings.input(noFormErrors),
    mappingYes = Mappings.nino(
      "assetIndividualBuyerNiNumber.yes.conditional.error.required",
      "assetIndividualBuyerNiNumber.yes.conditional.error.invalid"
    )
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    individualName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "assetIndividualBuyerNiNumber.title",
      Message("assetIndividualBuyerNiNumber.heading", individualName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("assetIndividualBuyerNiNumber.yes.conditional", individualName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("assetIndividualBuyerNiNumber.no.conditional", individualName), FieldType.Textarea)
      ).withHint("assetIndividualBuyerNiNumber.hint"),
      routes.AssetIndividualBuyerNiNumberController.onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
