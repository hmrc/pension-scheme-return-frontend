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

package controllers.nonsipp.otherassetsdisposal

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.otherassetsdisposal.OtherBuyerDetailsController._
import forms.RecipientDetailsFormProvider
import models.SchemeId.Srn
import models.{Mode, RecipientDetails}
import navigation.Navigator
import pages.nonsipp.otherassetsdisposal.OtherBuyerDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
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

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val form = OtherBuyerDetailsController.form(formProvider)
      Ok(
        view(
          form.fromUserAnswers(OtherBuyerDetailsPage(srn, index, disposalIndex)),
          viewModel(
            srn,
            index,
            disposalIndex,
            mode
          )
        )
      )
    }

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
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
                    index,
                    disposalIndex,
                    mode
                  )
                )
              )
            ),
          answer =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(OtherBuyerDetailsPage(srn, index, disposalIndex), answer))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(OtherBuyerDetailsPage(srn, index, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object OtherBuyerDetailsController {
  def form(formProvider: RecipientDetailsFormProvider): Form[RecipientDetails] = formProvider(
    "otherAssetsDisposal.otherBuyerDetails.name.error.required",
    "otherAssetsDisposal.otherBuyerDetails.name.error.invalid",
    "otherAssetsDisposal.otherBuyerDetails.name.error.length",
    "otherAssetsDisposal.otherBuyerDetails.description.error.invalid",
    "otherAssetsDisposal.otherBuyerDetails.description.error.length"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[RecipientDetailsViewModel] =
    FormPageViewModel(
      Message("otherAssetsDisposal.otherBuyerDetails.title"),
      Message("otherAssetsDisposal.otherBuyerDetails.heading"),
      RecipientDetailsViewModel(
        Message("otherAssetsDisposal.otherBuyerDetails.name"),
        Message("otherAssetsDisposal.otherBuyerDetails.description")
      ),
      routes.OtherBuyerDetailsController.onSubmit(srn, index, disposalIndex, mode)
    )
}
