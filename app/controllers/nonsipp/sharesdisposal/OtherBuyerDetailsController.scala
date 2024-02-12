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
import controllers.nonsipp.sharesdisposal.OtherBuyerDetailsController._
import forms.RecipientDetailsFormProvider
import models.SchemeId.Srn
import models.{Mode, RecipientDetails}
import navigation.Navigator
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import pages.nonsipp.sharesdisposal.OtherBuyerDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, RecipientDetailsViewModel}
import views.html.RecipientDetailsView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class OtherBuyerDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: RecipientDetailsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: RecipientDetailsView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form: Form[RecipientDetails] = OtherBuyerDetailsController.form(formProvider)

  def onPageLoad(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
        val form = OtherBuyerDetailsController.form(formProvider)
        Ok(
          view(
            form.fromUserAnswers(OtherBuyerDetailsPage(srn, shareIndex, disposalIndex)),
            viewModel(
              srn,
              shareIndex,
              disposalIndex,
              companyName,
              mode
            )
          )
        )
      }
    }

  def onSubmit(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney { companyName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(
                      srn,
                      shareIndex,
                      disposalIndex,
                      companyName,
                      mode
                    )
                  )
                )
              ),
            answer =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(OtherBuyerDetailsPage(srn, shareIndex, disposalIndex), answer))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(OtherBuyerDetailsPage(srn, shareIndex, disposalIndex), mode, updatedAnswers)
              )
          )
      }
    }
}

object OtherBuyerDetailsController {
  def form(formProvider: RecipientDetailsFormProvider): Form[RecipientDetails] = formProvider(
    "sharesDisposal.otherBuyerDetails.name.error.required",
    "sharesDisposal.otherBuyerDetails.name.error.invalid",
    "sharesDisposal.otherBuyerDetails.name.error.length",
    "sharesDisposal.otherBuyerDetails.description.error.invalid",
    "sharesDisposal.otherBuyerDetails.description.error.length"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[RecipientDetailsViewModel] =
    FormPageViewModel(
      Message("sharesDisposal.otherBuyerDetails.title"),
      Message("sharesDisposal.otherBuyerDetails.heading", companyName),
      RecipientDetailsViewModel(
        Message("sharesDisposal.otherBuyerDetails.name"),
        Message("sharesDisposal.otherBuyerDetails.description")
      ),
      routes.OtherBuyerDetailsController.onSubmit(srn, index, disposalIndex, mode)
    )
}
