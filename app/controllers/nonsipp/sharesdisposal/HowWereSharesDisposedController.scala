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
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.sharesdisposal.{HowWereSharesDisposedPage, SharesDisposalCYAPointOfEntry}
import forms.RadioListFormProvider
import models._
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.InputFormErrors
import models.GenericFormMapper.ConditionalRadioMapper
import controllers.nonsipp.sharesdisposal.HowWereSharesDisposedController._
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import forms.mappings.Mappings
import models.PointOfEntry.{HowWereSharesDisposedPointOfEntry, NoPointOfEntry}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.RadioListView
import models.SchemeId.Srn
import navigation.Navigator
import models.HowSharesDisposed._
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

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
      // If this page is reached in CheckMode and there is no PointOfEntry set
      if (mode == CheckMode && request.userAnswers
          .get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex))
          .contains(NoPointOfEntry)) {
        // Set this page as the PointOfEntry
        saveService.save(
          request.userAnswers
            .set(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex), HowWereSharesDisposedPointOfEntry)
            .getOrElse(request.userAnswers)
        )
      }

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
              updatedAnswers <- request.userAnswers
                .set(page, value)
                .mapK[Future]
              hasAnswerChanged = request.userAnswers.exists(page)(_ == value)
              nextPage = navigator.nextPage(
                HowWereSharesDisposedPage(srn, shareIndex, disposalIndex, hasAnswerChanged),
                mode,
                updatedAnswers
              )
              updatedProgressAnswers <- saveProgress(srn, shareIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
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
      Mappings.input(formErrors)
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
