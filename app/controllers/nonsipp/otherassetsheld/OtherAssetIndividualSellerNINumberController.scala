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

package controllers.nonsipp.otherassetsheld

import config.Refined.Max5000
import controllers.actions._
import controllers.nonsipp.otherassetsheld.OtherAssetIndividualSellerNINumberController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.SchemeId.Srn
import models.{ConditionalYesNo, Mode}
import navigation.Navigator
import pages.nonsipp.otherassetsheld.{IndividualNameOfOtherAssetSellerPage, OtherAssetIndividualSellerNINumberPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class OtherAssetIndividualSellerNINumberController @Inject()(
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

  private val form: Form[Either[String, Nino]] = OtherAssetIndividualSellerNINumberController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(IndividualNameOfOtherAssetSellerPage(srn, index)).sync { individualName =>
        val preparedForm =
          request.userAnswers.fillForm(OtherAssetIndividualSellerNINumberPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, individualName, mode)))
      }

    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.usingAnswer(IndividualNameOfOtherAssetSellerPage(srn, index)).async { individualName =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, index, individualName, mode))
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(
                      OtherAssetIndividualSellerNINumberPage(srn, index),
                      ConditionalYesNo(value)
                    )
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator
                .nextPage(OtherAssetIndividualSellerNINumberPage(srn, index), mode, updatedAnswers)
            )
        )
    }
}

object OtherAssetIndividualSellerNINumberController {

  private val noFormErrors = InputFormErrors.textArea(
    "otherAssets.otherAssetIndividualSellerNINumber.no.conditional.error.required",
    "otherAssets.otherAssetIndividualSellerNINumber.no.conditional.error.invalid",
    "otherAssets.otherAssetIndividualSellerNINumber.no.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, Nino]] = formProvider.conditional(
    "otherAssets.otherAssetIndividualSellerNINumber.error.required",
    mappingNo = Mappings.input(noFormErrors),
    mappingYes = Mappings.nino(
      "otherAssets.otherAssetIndividualSellerNINumber.yes.conditional.error.required",
      "otherAssets.otherAssetIndividualSellerNINumber.yes.conditional.error.invalid"
    )
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "otherAssets.otherAssetIndividualSellerNINumber.title",
      Message("otherAssets.otherAssetIndividualSellerNINumber.heading", individualName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(
            Message("otherAssets.otherAssetIndividualSellerNINumber.yes.conditional", individualName),
            FieldType.Input
          ),
        no = YesNoViewModel
          .Conditional(
            Message("otherAssets.otherAssetIndividualSellerNINumber.no.conditional", individualName),
            FieldType.Textarea
          )
      ),
      routes.OtherAssetIndividualSellerNINumberController.onSubmit(srn, index, mode)
    )
}