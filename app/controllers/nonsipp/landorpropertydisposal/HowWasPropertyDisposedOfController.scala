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

package controllers.nonsipp.landorpropertydisposal

import services.SaveService
import viewmodels.implicits._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.nonsipp.landorpropertydisposal.HowWasPropertyDisposedOfController._
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import pages.nonsipp.landorpropertydisposal.HowWasPropertyDisposedOfPage
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
import views.html.RadioListView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class HowWasPropertyDisposedOfController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  view: RadioListView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = HowWasPropertyDisposedOfController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
        address =>
          val preparedForm =
            request.userAnswers.fillForm(
              HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex),
              form
            )

          Ok(view(preparedForm, viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)))
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
              address =>
                Future.successful(
                  BadRequest(
                    view(formWithErrors, viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode))
                  )
                )
            },
          value => {
            val page = HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex)
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
              hasAnswerChanged = request.userAnswers.exists(page)(_ == value)
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex, hasAnswerChanged),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object HowWasPropertyDisposedOfController {

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
    "howWasDisposed.conditional.error.required",
    "howWasDisposed.conditional.error.invalid",
    "howWasDisposed.conditional.error.length"
  )

  def form(formProvider: RadioListFormProvider): Form[HowDisposed] =
    formProvider.singleConditional[HowDisposed, String](
      "howWasDisposed.error.required",
      Other.name,
      Mappings.input(formErrors)
    )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "howWasDisposed.title",
      Message("howWasDisposed.heading", addressLine1),
      List(
        RadioListRowViewModel("howWasDisposed.option1", HowDisposed.Sold.name),
        RadioListRowViewModel("howWasDisposed.option2", HowDisposed.Transferred.name),
        RadioListRowViewModel.conditional(
          content = "howWasDisposed.option3",
          HowDisposed.Other.name,
          hint = None,
          RadioItemConditional(
            FieldType.Textarea,
            label = Some(Message("howWasDisposed.option3.label"))
          )
        )
      ),
      controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
        .onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
