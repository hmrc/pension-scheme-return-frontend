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

package controllers.nonsipp.sharesdisposal

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.sharesdisposal.HowWereSharesDisposedController._
import forms.RadioListFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.GenericFormMapper.ConditionalRadioMapper
import models.HowSharesDisposed._
import models.{ConditionalRadioMapper, HowSharesDisposed, Mode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import pages.nonsipp.sharesdisposal.HowWereSharesDisposedPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FieldType, FormPageViewModel, RadioItemConditional, RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class HowWereSharesDisposedController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RadioListFormProvider,
  view: RadioListView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = HowWereSharesDisposedController.form(formProvider)

  def onPageLoad(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
        val preparedForm =
          request.userAnswers.fillForm(
            HowWereSharesDisposedPage(srn, shareIndex, disposalIndex),
            form
          )

        Ok(view(preparedForm, viewModel(srn, shareIndex, disposalIndex, companyName, mode)))
      }
    }

  def onSubmit(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, viewModel(srn, shareIndex, disposalIndex, companyName, mode))
                )
              )
            },
          value => {
            val page = HowWereSharesDisposedPage(srn, shareIndex, disposalIndex)
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(page, value))
              hasAnswerChanged = request.userAnswers.exists(page)(_ == value)
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                HowWereSharesDisposedPage(srn, shareIndex, disposalIndex, hasAnswerChanged),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object HowWereSharesDisposedController {

  implicit val formMapping: ConditionalRadioMapper[String, HowSharesDisposed] =
    ConditionalRadioMapper[String, HowSharesDisposed](
      to = (value, conditional) =>
        ((value, conditional): @unchecked) match {
          case (HowSharesDisposed.Sold.name, _) => HowSharesDisposed.Sold
          case (HowSharesDisposed.Redeemed.name, _) => HowSharesDisposed.Redeemed
          case (HowSharesDisposed.Transferred.name, _) => HowSharesDisposed.Transferred
          case (HowSharesDisposed.Other.name, Some(details)) => HowSharesDisposed.Other(details)
        },
      from = {
        case HowSharesDisposed.Sold => Some((HowSharesDisposed.Sold.name, None))
        case HowSharesDisposed.Redeemed => Some((HowSharesDisposed.Redeemed.name, None))
        case HowSharesDisposed.Transferred => Some((HowSharesDisposed.Transferred.name, None))
        case HowSharesDisposed.Other(details) => Some((HowSharesDisposed.Other.name, Some(details)))
      }
    )

  private val formErrors = InputFormErrors.textArea(
    "sharesDisposal.howWereSharesDisposed.conditional.error.required",
    "sharesDisposal.howWereSharesDisposed.conditional.error.invalid",
    "sharesDisposal.howWereSharesDisposed.conditional.error.length"
  )

  def form(formProvider: RadioListFormProvider): Form[HowSharesDisposed] =
    formProvider.singleConditional[HowSharesDisposed, String](
      "sharesDisposal.howWereSharesDisposed.error.required",
      Other.name,
      Mappings.input("conditional", formErrors)
    )

  def viewModel(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "sharesDisposal.howWereSharesDisposed.title",
      Message("sharesDisposal.howWereSharesDisposed.heading", companyName),
      List(
        RadioListRowViewModel("sharesDisposal.howWereSharesDisposed.option1", HowSharesDisposed.Sold.name),
        RadioListRowViewModel("sharesDisposal.howWereSharesDisposed.option2", HowSharesDisposed.Redeemed.name),
        RadioListRowViewModel("sharesDisposal.howWereSharesDisposed.option3", HowSharesDisposed.Transferred.name),
        RadioListRowViewModel.conditional(
          content = "sharesDisposal.howWereSharesDisposed.option4",
          HowSharesDisposed.Other.name,
          hint = None,
          RadioItemConditional(
            FieldType.Textarea,
            label = Some(Message("sharesDisposal.howWereSharesDisposed.option4.label"))
          )
        )
      ),
      controllers.nonsipp.sharesdisposal.routes.HowWereSharesDisposedController
        .onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
