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
import pages.nonsipp.otherassetsdisposal.HowWasAssetDisposedOfPage
import viewmodels.implicits._
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.RadioListFormProvider
import models.{ConditionalRadioMapper, HowDisposed, Mode}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import models.GenericFormMapper.ConditionalRadioMapper
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import models.HowDisposed._
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.RadioListView
import controllers.nonsipp.otherassetsdisposal.HowWasAssetDisposedOfController._
import models.SchemeId.Srn
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class HowWasAssetDisposedOfController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  view: RadioListView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = HowWasAssetDisposedOfController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(
          HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex),
          form
        )

      Ok(view(preparedForm, viewModel(srn, assetIndex, disposalIndex, mode)))
    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(formWithErrors, viewModel(srn, assetIndex, disposalIndex, mode))
              )
            ),
          value => {
            val page = HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex)
            for {
              updatedAnswers <- request.userAnswers
                .set(page, value)
                .mapK[Future]
              nextPage = navigator
                .nextPage(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, assetIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
          }
        )
    }
}

object HowWasAssetDisposedOfController {

  implicit val formMapping: ConditionalRadioMapper[String, HowDisposed] = ConditionalRadioMapper[String, HowDisposed](
    to = (value, conditional) =>
      ((value, conditional): @unchecked) match {
        case (HowDisposed.Sold.name, _) => HowDisposed.Sold
        case (HowDisposed.Transferred.name, _) => HowDisposed.Transferred
        case (HowDisposed.Other.name, Some(details)) => HowDisposed.Other(details)
      },
    from = {
      case HowDisposed.Sold => Some((HowDisposed.Sold.name, None))
      case HowDisposed.Transferred => Some((HowDisposed.Transferred.name, None))
      case HowDisposed.Other(details) => Some((HowDisposed.Other.name, Some(details)))
    }
  )

  private val formErrors = InputFormErrors.textArea(
    "howWasAssetDisposedOf.conditional.error.required",
    "error.textarea.invalid",
    "howWasAssetDisposedOf.conditional.error.length"
  )

  def form(formProvider: RadioListFormProvider): Form[HowDisposed] =
    formProvider.singleConditional[HowDisposed, String](
      "howWasAssetDisposedOf.error.required",
      Other.name,
      Mappings.input(formErrors)
    )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "howWasAssetDisposedOf.title",
      Message("howWasAssetDisposedOf.heading"),
      List(
        RadioListRowViewModel("howWasAssetDisposedOf.option1", HowDisposed.Sold.name),
        RadioListRowViewModel("howWasAssetDisposedOf.option2", HowDisposed.Transferred.name),
        RadioListRowViewModel.conditional(
          content = "howWasAssetDisposedOf.option3",
          HowDisposed.Other.name,
          hint = None,
          RadioItemConditional(
            FieldType.Textarea,
            label = Some(Message("howWasAssetDisposedOf.option3.label"))
          )
        )
      ),
      controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController
        .onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
