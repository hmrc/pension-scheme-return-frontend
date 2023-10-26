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

package controllers.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.RecipientDetailsFormProvider
import models.SchemeId.Srn
import models.{Mode, RecipientDetails}
import navigation.Navigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils.FormOps
import viewmodels.models.{FormPageViewModel, RecipientDetailsViewModel}
import views.html.RecipientDetailsView
import controllers.nonsipp.landorpropertydisposal.OtherBuyerDetailsController._
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import pages.nonsipp.landorpropertydisposal.OtherBuyerDetailsPage
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class OtherBuyerDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: RecipientDetailsFormProvider,
  view: RecipientDetailsView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val address: String =
        request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).get.addressLine1
      val form = OtherBuyerDetailsController.form(formProvider)
      Ok(
        view(
          form.fromUserAnswers(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex)),
          viewModel(srn, landOrPropertyIndex, disposalIndex, mode, address)
        )
      )
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val address: String =
        request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).get.addressLine1
      val form = OtherBuyerDetailsController.form(formProvider)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, viewModel(srn, landOrPropertyIndex, disposalIndex, mode, address)))
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex), answer)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
            )
          }
        )
    }
}

object OtherBuyerDetailsController {
  def form(formProvider: RecipientDetailsFormProvider): Form[RecipientDetails] = formProvider(
    "otherBuyerDetails.name.error.required",
    "otherBuyerDetails.name.error.invalid",
    "otherBuyerDetails.name.error.length",
    "otherBuyerDetails.description.error.invalid",
    "otherBuyerDetails.description.error.length"
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    address: String
  ): FormPageViewModel[RecipientDetailsViewModel] =
    FormPageViewModel(
      Message("otherBuyerDetails.title"),
      Message("otherBuyerDetails.heading", address),
      RecipientDetailsViewModel(
        Message("otherBuyerDetails.name"),
        Message("otherBuyerDetails.description")
      ),
      routes.OtherBuyerDetailsController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
