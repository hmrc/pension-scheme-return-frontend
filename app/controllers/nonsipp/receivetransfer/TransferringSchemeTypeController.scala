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

package controllers.nonsipp.receivetransfer

import services.SaveService
import viewmodels.implicits._
import forms.mappings.Mappings
import models.PensionSchemeType._
import config.RefinedTypes.{Max300, Max5}
import pages.nonsipp.receivetransfer.TransferringSchemeTypePage
import config.Constants.{inputRegexPSTR, inputRegexQROPS, maxNotRelevant}
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.receivetransfer.TransferringSchemeTypeController._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{ConditionalRadioMapper, Mode, PensionSchemeType}
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import models.GenericFormMapper.ConditionalRadioMapper
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.RadioListView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TransferringSchemeTypeController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max5,
    mode: Mode
  ): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val maybeAnswer = request.userAnswers.get(TransferringSchemeTypePage(srn, index, secondaryIndex))
    val builtForm = maybeAnswer.fold(form(formProvider))(answer => form(formProvider, Some(answer.name)))
    val schemeName = request.schemeDetails.schemeName
    val filledForm = maybeAnswer.fold(builtForm)(builtForm.fill)

    Ok(
      view(
        filledForm,
        TransferringSchemeTypeController.viewModel(srn, index, secondaryIndex, schemeName, mode)
      )
    )
  }

  def onSubmit(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max5,
    mode: Mode
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val schemeName = request.schemeDetails.schemeName
    form(formProvider)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future
            .successful(
              BadRequest(
                view(
                  formWithErrors,
                  TransferringSchemeTypeController.viewModel(srn, index, secondaryIndex, schemeName, mode)
                )
              )
            ),
        answer =>
          for {
            updatedAnswers <- request.userAnswers
              .set(TransferringSchemeTypePage(srn, index, secondaryIndex), answer)
              .mapK
            nextPage = navigator.nextPage(TransferringSchemeTypePage(srn, index, secondaryIndex), mode, updatedAnswers)
            updatedProgressAnswers <- saveProgress(srn, index, secondaryIndex, updatedAnswers, nextPage)
            _ <- saveService.save(updatedProgressAnswers)
          } yield Redirect(nextPage)
      )
  }
}

object TransferringSchemeTypeController {
  implicit val formMapping: ConditionalRadioMapper[String, PensionSchemeType] =
    ConditionalRadioMapper[String, PensionSchemeType](
      to = (value, conditional) =>
        ((value, conditional): @unchecked) match {
          case (PensionSchemeType.RegisteredPS.name, Some(code)) =>
            PensionSchemeType.RegisteredPS(code)

          case (PensionSchemeType.QualifyingRecognisedOverseasPS.name, Some(code)) =>
            PensionSchemeType.QualifyingRecognisedOverseasPS(code)

          case (PensionSchemeType.Other.name, Some(details)) => PensionSchemeType.Other(details)
        },
      from = {
        case PensionSchemeType.RegisteredPS(code) =>
          Some((PensionSchemeType.RegisteredPS.name, Some(code)))

        case PensionSchemeType.QualifyingRecognisedOverseasPS(code) =>
          Some((PensionSchemeType.QualifyingRecognisedOverseasPS.name, Some(code)))

        case PensionSchemeType.Other(details) => Some((PensionSchemeType.Other.name, Some(details)))
      }
    )

  private def formErrors(typeName: String): InputFormErrors =
    if (typeName == PensionSchemeType.RegisteredPS.name) {
      InputFormErrors.genericInput(
        "transferring.conditional.PSTR.error.required",
        "transferring.conditional.PSTR.error.invalid",
        "transferring.conditional.PSTR.error.length",
        inputRegexPSTR,
        maxNotRelevant
      )
    } else if (typeName == PensionSchemeType.QualifyingRecognisedOverseasPS.name) {
      InputFormErrors.genericInput(
        "transferring.conditional.QROPS.error.required",
        "transferring.conditional.QROPS.error.invalid",
        "transferring.conditional.QROPS.error.length",
        inputRegexQROPS,
        maxNotRelevant
      )
    } else {
      InputFormErrors.textArea(
        "transferring.conditional.Other.error.required",
        "transferring.conditional.Other.error.invalid",
        "transferring.conditional.Other.error.length"
      )
    }

  def form(formProvider: RadioListFormProvider, prePopKey: Option[String] = None): Form[PensionSchemeType] = {
    val valuesToMap = PensionSchemeType.values.map { name =>
      (name, Some(Mappings.input(s"$name-conditional", formErrors(name))))
    }
    formProvider.conditionalM[PensionSchemeType, String](
      "transferring.error.required",
      valuesToMap,
      prePopKey
    )
  }

  private def radioListItems(schemeName: String): List[RadioListRowViewModel] =
    PensionSchemeType.values.map { name =>
      val conditionalField = if (name == PensionSchemeType.Other.name) {
        RadioItemConditional(
          FieldType.ConditionalTextarea(name),
          label = Some(Message(s"transferring.pensionType.$name.label", schemeName))
        )
      } else {
        RadioItemConditional(
          FieldType.ConditionalInput(name),
          label = Some(Message(s"transferring.pensionType.$name.label", schemeName))
        )
      }

      RadioListRowViewModel.conditional(
        content = Message(s"transferring.pensionType.$name"),
        name,
        hint = None,
        conditionalField
      )
    }

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max5,
    schemeName: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("transferring.pensionType.title"),
      Message("transferring.pensionType.heading", schemeName),
      RadioListViewModel(
        None,
        radioListItems(schemeName)
      ),
      controllers.nonsipp.receivetransfer.routes.TransferringSchemeTypeController
        .onSubmit(srn, index, secondaryIndex, mode)
    )
}
