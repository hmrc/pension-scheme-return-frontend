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

package controllers.nonsipp.bondsdisposal

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.bondsdisposal.HowWereBondsDisposedOfController._
import forms.RadioListFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.GenericFormMapper.ConditionalRadioMapper
import models.HowDisposed._
import models.SchemeId.Srn
import models.{ConditionalRadioMapper, HowDisposed, Mode}
import navigation.Navigator
import pages.nonsipp.bondsdisposal.HowWereBondsDisposedOfPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class HowWereBondsDisposedOfController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  view: RadioListView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = HowWereBondsDisposedOfController.form(formProvider)

  def onPageLoad(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(
          HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex),
          form
        )

      Ok(view(preparedForm, viewModel(srn, bondIndex, disposalIndex, mode)))
    }

  def onSubmit(srn: Srn, bondIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(formWithErrors, viewModel(srn, bondIndex, disposalIndex, mode))
              )
            ),
          value => {
            val page = HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex)
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
              hasAnswerChanged = request.userAnswers.exists(page)(_ == value)
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex, hasAnswerChanged),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object HowWereBondsDisposedOfController {

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
    "howWereBondsDisposedOf.conditional.error.required",
    "howWereBondsDisposedOf.conditional.error.invalid",
    "howWereBondsDisposedOf.conditional.error.length"
  )

  def form(formProvider: RadioListFormProvider): Form[HowDisposed] =
    formProvider.singleConditional[HowDisposed, String](
      "howWereBondsDisposedOf.error.required",
      Other.name,
      Mappings.input("conditional", formErrors)
    )

  def viewModel(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "howWereBondsDisposedOf.title",
      Message("howWereBondsDisposedOf.heading"),
      List(
        RadioListRowViewModel("howWereBondsDisposedOf.option1", HowDisposed.Sold.name),
        RadioListRowViewModel("howWereBondsDisposedOf.option2", HowDisposed.Transferred.name),
        RadioListRowViewModel.conditional(
          content = "howWereBondsDisposedOf.option3",
          HowDisposed.Other.name,
          hint = None,
          RadioItemConditional(
            FieldType.Textarea,
            label = Some(Message("howWereBondsDisposedOf.option3.label"))
          )
        )
      ),
      controllers.nonsipp.bondsdisposal.routes.HowWereBondsDisposedOfController
        .onSubmit(srn, bondIndex, disposalIndex, mode)
    )
}
