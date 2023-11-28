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

package controllers.nonsipp.memberpayments

import config.Refined.{Max300, Max50}
import controllers.actions.IdentifyAndRequireData
import forms.mappings.errors.InputFormErrors
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.{ConditionalRadioMapper, Mode, PensionSchemeType}
import navigation.Navigator
import pages.nonsipp.memberpayments.PensionSchemeTypePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import forms.mappings.Mappings
import models.GenericFormMapper.ConditionalRadioMapper
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FieldType, FormPageViewModel, RadioItemConditional, RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView
import viewmodels.implicits._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TransferringSchemeType @Inject()(
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
    secondaryIndex: Max50,
    mode: Mode
  ): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val form = TransferringSchemeType.form(formProvider)
    val schemeName = request.schemeDetails.schemeName
    Ok(
      view(
        form.fromUserAnswers(PensionSchemeTypePage(srn, index, secondaryIndex)),
        TransferringSchemeType.viewModel(srn, index, secondaryIndex, schemeName, mode)
      )
    )
  }

  def onSubmit(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max50,
    mode: Mode
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = TransferringSchemeType.form(formProvider)
    val schemeName = request.schemeDetails.schemeName
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future
            .successful(
              BadRequest(
                view(formWithErrors, TransferringSchemeType.viewModel(srn, index, secondaryIndex, schemeName, mode))
              )
            ),
        answer => {
          for {
            updatedAnswers <- Future
              .fromTry(request.userAnswers.set(PensionSchemeTypePage(srn, index, secondaryIndex), answer))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(PensionSchemeTypePage(srn, index, secondaryIndex), mode, updatedAnswers))
        }
      )
  }
}

object TransferringSchemeType {
  implicit val formMapping: ConditionalRadioMapper[String, PensionSchemeType] =
    ConditionalRadioMapper[String, PensionSchemeType](
      to = (value, conditional) =>
        ((value, conditional): @unchecked) match {
          case (PensionSchemeType.RegisteredPS.name, Some(code)) => PensionSchemeType.RegisteredPS(code)
          case (PensionSchemeType.QualifyingRecognisedOverseasPS.name, Some(code)) =>
            PensionSchemeType.QualifyingRecognisedOverseasPS(code)
          case (PensionSchemeType.Other.name, Some(details)) => PensionSchemeType.Other(details)
        },
      from = {
        case PensionSchemeType.RegisteredPS(code) => Some((PensionSchemeType.RegisteredPS.name, Some(code)))
        case PensionSchemeType.QualifyingRecognisedOverseasPS(code) =>
          Some((PensionSchemeType.QualifyingRecognisedOverseasPS.name, Some(code)))
        case PensionSchemeType.Other(details) => Some((PensionSchemeType.Other.name, Some(details)))
      }
    )

  private val formErrors = InputFormErrors.textArea(
    "howWasDisposed.conditional.error.required",
    "howWasDisposed.conditional.error.invalid",
    "howWasDisposed.conditional.error.length"
  )
  def form(formProvider: RadioListFormProvider): Form[PensionSchemeType] =
    formProvider.singleConditional[PensionSchemeType, String](
      "howWasDisposed.error.required",
      PensionSchemeType.Other.name,
      Mappings.input(formErrors)
    )(formMapping)

  private def radioListItems(schemeName: String): List[RadioListRowViewModel] =
    PensionSchemeType.values.map { aType =>
      val conditionalField = if (aType.name == "other") {
        RadioItemConditional(
          FieldType.Textarea,
          label = Some(Message(s"transferring.pensionType.${aType.name}.label", schemeName))
        )
      } else {
        RadioItemConditional(
          FieldType.Input,
          label = Some(Message(s"transferring.pensionType.${aType.name}.label", schemeName))
        )
      }

      RadioListRowViewModel.conditional(
        content = Message(s"transferring.pensionType.${aType.name}"),
        aType.name,
        hint = None,
        conditionalField
      )
    }

  def viewModel(
    srn: Srn,
    index: Max300,
    secondaryIndex: Max50,
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
      controllers.nonsipp.memberpayments.routes.TransferringSchemeType.onPageLoad(srn, index, secondaryIndex, mode)
    )
}
